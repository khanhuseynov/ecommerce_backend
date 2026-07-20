package com.kahnhuseynov.ecommercebackend.cart.controller

import com.kahnhuseynov.ecommercebackend.cart.dto.*
import com.kahnhuseynov.ecommercebackend.cart.service.CartService
import com.kahnhuseynov.ecommercebackend.core.security.CustomUserPrincipal
import com.kahnhuseynov.ecommercebackend.user.entity.User
import org.springframework.http.HttpStatus
import spock.lang.Specification

class CartControllerSpec extends Specification {
    CartService cartService = Mock()
    CartController controller = new CartController(cartService)
    CustomUserPrincipal principal = principal()

    def "getCart delegates authenticated user id"() {
        given: cartService.getCart(1L) >> response()
        expect: controller.getCart(principal).body == response()
    }

    def "addItem returns created"() {
        given:
        def request = new AddCartItemRequest(2L, 1)
        cartService.addItem(1L, request) >> response()
        expect:
        controller.addItem(principal, request).statusCode == HttpStatus.CREATED
    }

    def "removeItem returns no content"() {
        when: def result = controller.removeItem(principal, 3L)
        then:
        result.statusCode == HttpStatus.NO_CONTENT
        1 * cartService.removeItem(1L, 3L)
    }

    def "clearCart returns no content"() {
        when: def result = controller.clearCart(principal)
        then:
        result.statusCode == HttpStatus.NO_CONTENT
        1 * cartService.clearCart(1L)
    }

    private static CustomUserPrincipal principal() {
        def user = new User(); user.id = 1L; user.email = "user@test.com"; user.password = "password"; user.enabled = true
        new CustomUserPrincipal(user)
    }

    private static CartResponse response() { new CartResponse(1L, [], 0, BigDecimal.ZERO) }
}
