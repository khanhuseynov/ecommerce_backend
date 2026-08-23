package com.kahnhuseynov.ecommercebackend.order.controller;

import com.kahnhuseynov.ecommercebackend.core.security.CustomUserPrincipal;
import com.kahnhuseynov.ecommercebackend.order.dto.CheckoutRequest;
import com.kahnhuseynov.ecommercebackend.order.dto.OrderResponse;
import com.kahnhuseynov.ecommercebackend.order.service.OrderService;
import com.kahnhuseynov.ecommercebackend.core.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CheckoutRequest request
    ) {
        return new ResponseEntity<>(
                orderService.checkout(principal.getId(), request),
                HttpStatus.CREATED
        );
    }

    @GetMapping
    public ResponseEntity<PageResponse<OrderResponse>> getOrders(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(PageResponse.from(orderService.getOrders(principal.getId(), pageable)));
    }


    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(orderService.getOrder(principal.getId(), orderId));
    }
}
