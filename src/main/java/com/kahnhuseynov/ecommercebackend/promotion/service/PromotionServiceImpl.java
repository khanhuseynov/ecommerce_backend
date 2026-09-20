package com.kahnhuseynov.ecommercebackend.promotion.service;

import com.kahnhuseynov.ecommercebackend.promotion.dto.*;
import com.kahnhuseynov.ecommercebackend.promotion.entity.*;
import com.kahnhuseynov.ecommercebackend.promotion.repository.*;
import com.kahnhuseynov.ecommercebackend.order.entity.Order;
import com.kahnhuseynov.ecommercebackend.product.repository.ProductRepository;
import com.kahnhuseynov.ecommercebackend.category.repository.CategoryRepository;
import com.kahnhuseynov.ecommercebackend.core.exception.*;
import com.kahnhuseynov.ecommercebackend.core.pagination.PaginationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.math.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class PromotionServiceImpl implements PromotionService {
    private final PromotionRepository promotionRepository;
    private final PromotionUsageRepository usageRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public PromotionResponse create(PromotionRequest request) {
        return save(new Promotion(), request);
    }

    @Override
    public PromotionResponse update(Long id, PromotionRequest request) {
        return save(locked(id), request);
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponse get(Long id) {
        return PromotionResponse.from(promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found.")));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PromotionResponse> getAll(Pageable pageable) {
        return promotionRepository.findAll(PaginationValidator.normalizeSort(pageable,
                Map.of("id", "id", "name", "name", "starts_at", "startsAt", "ends_at", "endsAt")))
                .map(PromotionResponse::from);
    }

    @Override
    public void deactivate(Long id) {
        locked(id).setActive(false);
    }

    private Promotion locked(Long id) {
        return promotionRepository.findLockedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found."));
    }

    private PromotionResponse save(Promotion p, PromotionRequest r) {
        if (!r.endsAt().isAfter(r.startsAt())) {
            throw new InvalidRequestException("End date must be after start date.");
        }
        if (r.discountType() == DiscountType.PERCENTAGE && r.discountValue().compareTo(new BigDecimal("100")) > 0) {
            throw new InvalidRequestException("Percentage cannot exceed 100.");
        }
        if (r.productId() != null && r.categoryId() != null) {
            throw new InvalidRequestException("Choose either a product or a category scope.");
        }
        if (r.productId() != null && !productRepository.existsById(r.productId())) {
            throw new ResourceNotFoundException("Product not found.");
        }
        if (r.categoryId() != null && !categoryRepository.existsById(r.categoryId())) {
            throw new ResourceNotFoundException("Category not found.");
        }
        if (r.usageLimit() != null && r.usageLimit() < p.getUsageCount()) {
            throw new InvalidRequestException("Usage limit cannot be below existing usage count.");
        }
        String code = r.code() == null ? null : r.code().toUpperCase(Locale.ROOT);
        if (code != null && !code.equals(p.getCode()) && promotionRepository.existsByCode(code)) {
            throw new BusinessException("Coupon code already exists.");
        }
        p.setName(r.name().trim());
        p.setCode(code);
        p.setDiscountType(r.discountType());
        p.setDiscountValue(r.discountValue());
        p.setMinimumOrderAmount(r.minimumOrderAmount());
        p.setMaximumDiscountAmount(r.maximumDiscountAmount());
        p.setStartsAt(r.startsAt());
        p.setEndsAt(r.endsAt());
        p.setActive(r.active());
        p.setUsageLimit(r.usageLimit());
        p.setPerUserLimit(r.perUserLimit());
        p.setProductId(r.productId());
        p.setCategoryId(r.categoryId());
        try {
            return PromotionResponse.from(promotionRepository.saveAndFlush(p));
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException("Promotion conflicts with existing data.");
        }
    }

    // Checkout owns the transaction: usage, stock and order must commit or roll back together.
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void apply(Order order, String couponCode) {
        order.setOriginalTotalPrice(order.getTotalPrice());
        order.setDiscountAmount(BigDecimal.ZERO.setScale(2));
        order.setCouponCode(null);
        order.setPromotionName(null);
        Promotion selected = null;
        BigDecimal best = BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();
        if (couponCode != null && !couponCode.isBlank()) {
            selected = promotionRepository.findLockedByCode(couponCode.trim().toUpperCase(Locale.ROOT))
                    .orElseThrow(() -> new BusinessException("Invalid coupon code."));
            best = discount(selected, order, now);
            if (best.signum() <= 0) {
                throw new BusinessException("Coupon is expired, exhausted or not applicable to this order.");
            }
        } else {
            for (Promotion candidate : promotionRepository.findAutomaticWithLock()) {
                BigDecimal amount = discount(candidate, order, now);
                if (amount.compareTo(best) > 0) {
                    selected = candidate;
                    best = amount;
                }
            }
        }
        if (selected == null) return;
        order.setDiscountAmount(best);
        order.setTotalPrice(order.getOriginalTotalPrice().subtract(best));
        order.setCouponCode(selected.getCode());
        order.setPromotionName(selected.getName());
        selected.setUsageCount(selected.getUsageCount() + 1);
        PromotionUsage usage = new PromotionUsage();
        usage.setPromotionId(selected.getId());
        usage.setUserId(order.getUser().getId());
        usage.setOrderId(order.getId());
        usage.setDiscountAmount(best);
        usageRepository.save(usage);
    }

    private BigDecimal discount(Promotion p, Order order, LocalDateTime now) {
        if (!p.isActive() || now.isBefore(p.getStartsAt()) || !now.isBefore(p.getEndsAt())
                || order.getOriginalTotalPrice().compareTo(p.getMinimumOrderAmount()) < 0
                || (p.getUsageLimit() != null && p.getUsageCount() >= p.getUsageLimit())
                || (p.getPerUserLimit() != null && usageRepository.countByPromotionIdAndUserId(
                        p.getId(), order.getUser().getId()) >= p.getPerUserLimit())) {
            return BigDecimal.ZERO;
        }
        BigDecimal eligible = order.getItems().stream().filter(item ->
                (p.getProductId() == null || p.getProductId().equals(item.getProduct().getId()))
                && (p.getCategoryId() == null || (item.getProduct().getCategory() != null
                && p.getCategoryId().equals(item.getProduct().getCategory().getId()))))
                .map(item -> item.getSubtotal()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal amount = p.getDiscountType() == DiscountType.PERCENTAGE
                ? eligible.multiply(p.getDiscountValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                : p.getDiscountValue();
        if (p.getMaximumDiscountAmount() != null) amount = amount.min(p.getMaximumDiscountAmount());
        return amount.min(eligible).min(order.getOriginalTotalPrice()).setScale(2, RoundingMode.HALF_UP);
    }
}
