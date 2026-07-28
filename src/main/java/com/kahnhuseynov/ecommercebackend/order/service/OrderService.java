package com.kahnhuseynov.ecommercebackend.order.service;

import com.kahnhuseynov.ecommercebackend.order.dto.CheckoutRequest;
import com.kahnhuseynov.ecommercebackend.order.dto.OrderResponse;

import java.util.List;

public interface OrderService {
    OrderResponse checkout(Long userId, CheckoutRequest request);

    List<OrderResponse> getOrders(Long userId);

    OrderResponse getOrder(Long userId, Long orderId);
}
