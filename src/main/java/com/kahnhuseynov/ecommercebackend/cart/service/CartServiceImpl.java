package com.kahnhuseynov.ecommercebackend.cart.service;

import com.kahnhuseynov.ecommercebackend.cart.dto.*;
import com.kahnhuseynov.ecommercebackend.cart.entity.Cart;
import com.kahnhuseynov.ecommercebackend.cart.entity.CartItem;
import com.kahnhuseynov.ecommercebackend.cart.repository.CartItemRepository;
import com.kahnhuseynov.ecommercebackend.cart.repository.CartRepository;
import com.kahnhuseynov.ecommercebackend.core.exception.BusinessException;
import com.kahnhuseynov.ecommercebackend.core.exception.ResourceNotFoundException;
import com.kahnhuseynov.ecommercebackend.product.entity.Product;
import com.kahnhuseynov.ecommercebackend.product.repository.ProductRepository;
import com.kahnhuseynov.ecommercebackend.user.entity.User;
import com.kahnhuseynov.ecommercebackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        return cartRepository.findByUserId(userId).map(this::toResponse).orElseGet(this::emptyCart);
    }

    @Override @Transactional
    public CartResponse addItem(Long userId, AddCartItemRequest request) {
        Product product = findAvailableProduct(request.productId());
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> createCart(userId));
        CartItem item = cart.getItems().stream().filter(i -> i.getProduct().getId().equals(product.getId()))
                .findFirst().orElseGet(() -> newItem(cart, product));
        int quantity = item.getQuantity() + request.quantity();
        validateStock(product, quantity);
        item.setQuantity(quantity);
        return toResponse(cartRepository.save(cart));
    }

    @Override @Transactional
    public CartResponse updateItem(Long userId, Long itemId, UpdateCartItemRequest request) {
        CartItem item = findOwnedItem(userId, itemId);
        validateProductActive(item.getProduct());
        validateStock(item.getProduct(), request.quantity());
        item.setQuantity(request.quantity());
        cartItemRepository.save(item);
        return toResponse(item.getCart());
    }

    @Override @Transactional
    public void removeItem(Long userId, Long itemId) {
        cartItemRepository.delete(findOwnedItem(userId, itemId));
    }

    @Override @Transactional
    public void clearCart(Long userId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> cart.getItems().clear());
    }

    private Cart createCart(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new ResourceNotFoundException("User with id '%s' not found.".formatted(userId)));
        Cart cart = new Cart();
        cart.setUser(user);
        return cart;
    }

    private CartItem newItem(Cart cart, Product product) {
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(0);
        cart.getItems().add(item);
        return item;
    }

    private Product findAvailableProduct(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(() ->
                new ResourceNotFoundException("Product with id '%s' not found.".formatted(productId)));
        validateProductActive(product);
        return product;
    }

    private CartItem findOwnedItem(Long userId, Long itemId) {
        return cartItemRepository.findByIdAndCartUserId(itemId, userId).orElseThrow(() ->
                new ResourceNotFoundException("Cart item with id '%s' not found.".formatted(itemId)));
    }

    private void validateProductActive(Product product) {
        if (!Boolean.TRUE.equals(product.getActive())) throw new BusinessException("Inactive product cannot be added to cart.");
    }

    private void validateStock(Product product, int quantity) {
        if (quantity > product.getStockQuantity()) throw new BusinessException("Requested quantity exceeds available stock.");
    }

    private CartResponse toResponse(Cart cart) {
        var items = cart.getItems().stream().map(item -> {
            BigDecimal subtotal = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            return new CartItemResponse(item.getId(), item.getProduct().getId(), item.getProduct().getName(),
                    item.getProduct().getPrice(), item.getQuantity(), subtotal);
        }).toList();
        int quantity = items.stream().mapToInt(CartItemResponse::quantity).sum();
        BigDecimal total = items.stream().map(CartItemResponse::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(cart.getId(), items, quantity, total);
    }

    private CartResponse emptyCart() { return new CartResponse(null, java.util.List.of(), 0, BigDecimal.ZERO); }
}
