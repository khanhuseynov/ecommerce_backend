package com.kahnhuseynov.ecommercebackend.cart.service;

import com.kahnhuseynov.ecommercebackend.cart.dto.*;

public interface CartService {
    CartResponse getCart(Long userId);
    CartResponse addItem(Long userId, AddCartItemRequest request);
    CartResponse updateItem(Long userId, Long itemId, UpdateCartItemRequest request);
    void removeItem(Long userId, Long itemId);
    void clearCart(Long userId);
}
