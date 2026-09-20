package com.kahnhuseynov.ecommercebackend.promotion.service;

import com.kahnhuseynov.ecommercebackend.promotion.dto.*;
import com.kahnhuseynov.ecommercebackend.order.entity.Order;
import org.springframework.data.domain.*;

public interface PromotionService {
    PromotionResponse create(PromotionRequest request);
    PromotionResponse update(Long id, PromotionRequest request);
    PromotionResponse get(Long id);
    Page<PromotionResponse> getAll(Pageable pageable);
    void deactivate(Long id);
    void apply(Order order, String couponCode);
}
