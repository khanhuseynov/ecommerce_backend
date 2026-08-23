package com.kahnhuseynov.ecommercebackend.order.service;

import com.kahnhuseynov.ecommercebackend.cart.entity.Cart;
import com.kahnhuseynov.ecommercebackend.cart.repository.CartRepository;
import com.kahnhuseynov.ecommercebackend.core.exception.BusinessException;
import com.kahnhuseynov.ecommercebackend.core.exception.ResourceNotFoundException;
import com.kahnhuseynov.ecommercebackend.order.dto.CheckoutRequest;
import com.kahnhuseynov.ecommercebackend.order.dto.OrderItemResponse;
import com.kahnhuseynov.ecommercebackend.order.dto.OrderResponse;
import com.kahnhuseynov.ecommercebackend.order.entity.Order;
import com.kahnhuseynov.ecommercebackend.order.entity.OrderItem;
import com.kahnhuseynov.ecommercebackend.order.entity.OrderStatus;
import com.kahnhuseynov.ecommercebackend.order.repository.OrderRepository;
import com.kahnhuseynov.ecommercebackend.product.entity.Product;
import com.kahnhuseynov.ecommercebackend.product.repository.ProductRepository;
import com.kahnhuseynov.ecommercebackend.core.pagination.PaginationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private static final Map<String, String> ALLOWED_SORT_FIELDS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("status", "status"),
            Map.entry("total_price", "totalPrice"),
            Map.entry("totalPrice", "totalPrice"),
            Map.entry("created_at", "createdAt"),
            Map.entry("createdAt", "createdAt"),
            Map.entry("updated_at", "updatedAt"),
            Map.entry("updatedAt", "updatedAt")
    );
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public OrderResponse checkout(Long userId, CheckoutRequest request) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Cart is empty."));

        if (cart.getItems().isEmpty()) {
            throw new BusinessException("Cart is empty.");
        }

        Order order = new Order();
        order.setUser(cart.getUser());
        order.setStatus(OrderStatus.PLACED);
        order.setShippingAddress(request.shippingAddress().trim());

        BigDecimal total = BigDecimal.ZERO;
        for (var cartItem : cart.getItems()) {
            Product product = productRepository.findWithLockById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product with id '%s' not found.".formatted(cartItem.getProduct().getId())
                    ));

            validateAvailability(product, cartItem.getQuantity());
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());

            BigDecimal subtotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setProductName(product.getName());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setSubtotal(subtotal);
            order.getItems().add(orderItem);
            total = total.add(subtotal);
        }

        order.setTotalPrice(total);
        Order savedOrder = orderRepository.save(order);
        cart.getItems().clear();

        return toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(Long userId, Pageable pageable) {
        Pageable normalizedPageable = PaginationValidator.normalizeSort(pageable, ALLOWED_SORT_FIELDS);
        return orderRepository.findAllByUserId(userId, normalizedPageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order with id '%s' not found.".formatted(orderId)
                ));
    }

    private void validateAvailability(Product product, int quantity) {
        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new BusinessException("Product '%s' is inactive.".formatted(product.getName()));
        }
        if (quantity > product.getStockQuantity()) {
            throw new BusinessException(
                    "Insufficient stock for product '%s'.".formatted(product.getName())
            );
        }
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProductName(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getSubtotal()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getShippingAddress(),
                items,
                order.getTotalPrice(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
