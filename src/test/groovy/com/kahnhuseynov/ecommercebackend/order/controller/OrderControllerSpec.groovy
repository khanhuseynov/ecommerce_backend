package com.kahnhuseynov.ecommercebackend.order.controller

import com.kahnhuseynov.ecommercebackend.core.security.CustomUserPrincipal
import com.kahnhuseynov.ecommercebackend.order.dto.CheckoutRequest
import com.kahnhuseynov.ecommercebackend.order.dto.OrderResponse
import com.kahnhuseynov.ecommercebackend.order.entity.OrderStatus
import com.kahnhuseynov.ecommercebackend.order.service.OrderService
import com.kahnhuseynov.ecommercebackend.user.entity.User
import org.springframework.http.HttpStatus
import spock.lang.Specification

class OrderControllerSpec extends Specification {
    OrderService orderService = Mock()
    OrderController controller = new OrderController(orderService)
    CustomUserPrincipal principal = principal()

    def "checkout delegates authenticated user and returns created"() {
        given:
        def request = new CheckoutRequest("Baku")
        orderService.checkout(1L, request) >> response()

        when:
        def result = controller.checkout(principal, request)

        then:
        result.statusCode == HttpStatus.CREATED
        result.body.id() == 10L
    }

    def "getOrders delegates authenticated user"() {
        given:
        orderService.getOrders(1L) >> [response()]

        expect:
        controller.getOrders(principal).body*.id() == [10L]
    }

    def "getOrder scopes lookup to authenticated user"() {
        given:
        orderService.getOrder(1L, 10L) >> response()

        expect:
        controller.getOrder(principal, 10L).body.id() == 10L
    }

    private static CustomUserPrincipal principal() {
        def user = new User()
        user.id = 1L
        user.email = "user@test.com"
        user.password = "password"
        user.enabled = true
        new CustomUserPrincipal(user)
    }

    private static OrderResponse response() {
        new OrderResponse(10L, OrderStatus.PLACED, "Baku", [], new BigDecimal("25.50"), null, null)
    }
}
