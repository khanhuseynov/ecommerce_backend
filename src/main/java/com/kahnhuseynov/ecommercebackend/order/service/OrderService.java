package com.kahnhuseynov.ecommercebackend.order.service;

import com.kahnhuseynov.ecommercebackend.order.dto.CheckoutRequest;
import com.kahnhuseynov.ecommercebackend.order.dto.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    OrderResponse checkout(Long userId, CheckoutRequest request);

    Page<OrderResponse> getOrders(Long userId, Pageable pageable);

    OrderResponse getOrder(Long userId, Long orderId);
}
