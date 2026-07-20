package com.kahnhuseynov.ecommercebackend.cart.service

import com.kahnhuseynov.ecommercebackend.cart.dto.AddCartItemRequest
import com.kahnhuseynov.ecommercebackend.cart.dto.UpdateCartItemRequest
import com.kahnhuseynov.ecommercebackend.cart.entity.Cart
import com.kahnhuseynov.ecommercebackend.cart.entity.CartItem
import com.kahnhuseynov.ecommercebackend.cart.repository.CartItemRepository
import com.kahnhuseynov.ecommercebackend.cart.repository.CartRepository
import com.kahnhuseynov.ecommercebackend.core.exception.BusinessException
import com.kahnhuseynov.ecommercebackend.core.exception.ResourceNotFoundException
import com.kahnhuseynov.ecommercebackend.product.entity.Product
import com.kahnhuseynov.ecommercebackend.product.repository.ProductRepository
import com.kahnhuseynov.ecommercebackend.user.entity.User
import com.kahnhuseynov.ecommercebackend.user.repository.UserRepository
import spock.lang.Specification

class CartServiceImplSpec extends Specification {
    CartRepository cartRepository = Mock()
    CartItemRepository cartItemRepository = Mock()
    ProductRepository productRepository = Mock()
    UserRepository userRepository = Mock()
    CartServiceImpl service = new CartServiceImpl(cartRepository, cartItemRepository, productRepository, userRepository)

    def "getCart returns empty response when user has no cart"() {
        given: cartRepository.findByUserId(1L) >> Optional.empty()
        expect:
        with(service.getCart(1L)) {
            id() == null
            items().empty
            totalQuantity() == 0
            totalPrice() == BigDecimal.ZERO
        }
    }

    def "addItem creates cart and calculates totals"() {
        given:
        def product = product(10)
        def user = new User(); user.id = 1L
        productRepository.findById(2L) >> Optional.of(product)
        cartRepository.findByUserId(1L) >> Optional.empty()
        userRepository.findById(1L) >> Optional.of(user)
        cartRepository.save(_) >> { Cart cart -> cart.id = 3L; cart.items[0].id = 4L; cart }

        when: def result = service.addItem(1L, new AddCartItemRequest(2L, 2))
        then:
        result.id() == 3L
        result.totalQuantity() == 2
        result.totalPrice() == new BigDecimal("20.00")
        result.items()[0].subtotal() == new BigDecimal("20.00")
    }

    def "addItem increases quantity for existing product"() {
        given:
        def product = product(10)
        def cart = cart(product, 2)
        productRepository.findById(2L) >> Optional.of(product)
        cartRepository.findByUserId(1L) >> Optional.of(cart)
        cartRepository.save(cart) >> cart

        expect:
        service.addItem(1L, new AddCartItemRequest(2L, 3)).totalQuantity() == 5
        cart.items[0].quantity == 5
    }

    def "addItem rejects quantity above stock"() {
        given:
        productRepository.findById(2L) >> Optional.of(product(2))
        cartRepository.findByUserId(1L) >> Optional.of(new Cart(items: []))
        when: service.addItem(1L, new AddCartItemRequest(2L, 3))
        then:
        def exception = thrown(BusinessException)
        exception.message == "Requested quantity exceeds available stock."
        0 * cartRepository.save(_)
    }

    def "updateItem rejects item belonging to another cart"() {
        given: cartItemRepository.findByIdAndCartUserId(7L, 1L) >> Optional.empty()
        when: service.updateItem(1L, 7L, new UpdateCartItemRequest(2))
        then:
        def exception = thrown(ResourceNotFoundException)
        exception.message == "Cart item with id '7' not found."
    }

    def "removeItem deletes owned item"() {
        given:
        def item = new CartItem(id: 7L)
        cartItemRepository.findByIdAndCartUserId(7L, 1L) >> Optional.of(item)
        when: service.removeItem(1L, 7L)
        then: 1 * cartItemRepository.delete(item)
    }

    private static Product product(int stock) {
        def product = new Product()
        product.id = 2L; product.name = "Laptop"; product.price = new BigDecimal("10.00")
        product.stockQuantity = stock; product.active = true
        product
    }

    private static Cart cart(Product product, int quantity) {
        def cart = new Cart(); cart.id = 3L; cart.items = []
        def item = new CartItem(); item.id = 4L; item.cart = cart; item.product = product; item.quantity = quantity
        cart.items.add(item)
        cart
    }
}
