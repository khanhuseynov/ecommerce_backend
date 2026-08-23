package com.kahnhuseynov.ecommercebackend.order.service

import com.kahnhuseynov.ecommercebackend.cart.entity.Cart
import com.kahnhuseynov.ecommercebackend.cart.entity.CartItem
import com.kahnhuseynov.ecommercebackend.cart.repository.CartRepository
import com.kahnhuseynov.ecommercebackend.core.exception.BusinessException
import com.kahnhuseynov.ecommercebackend.core.exception.ResourceNotFoundException
import com.kahnhuseynov.ecommercebackend.order.dto.CheckoutRequest
import com.kahnhuseynov.ecommercebackend.order.entity.Order
import com.kahnhuseynov.ecommercebackend.order.entity.OrderStatus
import com.kahnhuseynov.ecommercebackend.order.repository.OrderRepository
import com.kahnhuseynov.ecommercebackend.product.entity.Product
import com.kahnhuseynov.ecommercebackend.product.repository.ProductRepository
import com.kahnhuseynov.ecommercebackend.user.entity.User
import spock.lang.Specification
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class OrderServiceImplSpec extends Specification {
    OrderRepository orderRepository = Mock()
    CartRepository cartRepository = Mock()
    ProductRepository productRepository = Mock()
    OrderServiceImpl service = new OrderServiceImpl(orderRepository, cartRepository, productRepository)

    def "checkout creates price snapshot decreases stock and clears cart"() {
        given:
        def product = product(2L, "Laptop", "25.50", 5, true)
        def cart = cart(product, 2)
        cartRepository.findByUserId(1L) >> Optional.of(cart)
        productRepository.findWithLockById(2L) >> Optional.of(product)
        orderRepository.save(_ as Order) >> { Order order ->
            order.id = 10L
            order.items[0].id = 11L
            order
        }

        when:
        def response = service.checkout(1L, new CheckoutRequest("  Baku, Nizami street  "))

        then:
        response.id() == 10L
        response.status() == OrderStatus.PLACED
        response.shippingAddress() == "Baku, Nizami street"
        response.totalPrice() == new BigDecimal("51.00")
        response.items()[0].productName() == "Laptop"
        response.items()[0].unitPrice() == new BigDecimal("25.50")
        response.items()[0].quantity() == 2
        response.items()[0].subtotal() == new BigDecimal("51.00")
        product.stockQuantity == 3
        cart.items.empty
    }

    def "checkout rejects missing or empty cart"() {
        given:
        cartRepository.findByUserId(1L) >> cartResult

        when:
        service.checkout(1L, new CheckoutRequest("Baku"))

        then:
        def exception = thrown(BusinessException)
        exception.message == "Cart is empty."
        0 * orderRepository.save(_)

        where:
        cartResult << [Optional.empty(), Optional.of(new Cart(items: []))]
    }

    def "checkout rejects insufficient stock"() {
        given:
        def cartProduct = product(2L, "Laptop", "25.50", 5, true)
        def lockedProduct = product(2L, "Laptop", "25.50", 1, true)
        cartRepository.findByUserId(1L) >> Optional.of(cart(cartProduct, 2))
        productRepository.findWithLockById(2L) >> Optional.of(lockedProduct)

        when:
        service.checkout(1L, new CheckoutRequest("Baku"))

        then:
        def exception = thrown(BusinessException)
        exception.message == "Insufficient stock for product 'Laptop'."
        lockedProduct.stockQuantity == 1
        0 * orderRepository.save(_)
    }

    def "getOrder only looks up an order owned by current user"() {
        given:
        orderRepository.findByIdAndUserId(99L, 1L) >> Optional.empty()

        when:
        service.getOrder(1L, 99L)

        then:
        def exception = thrown(ResourceNotFoundException)
        exception.message == "Order with id '99' not found."
    }

    def "getOrders returns a paginated response scoped to current user"() {
        given:
        def pageable = PageRequest.of(0, 20)
        def order = new Order(
                id: 10L,
                user: new User(id: 1L),
                status: OrderStatus.PLACED,
                shippingAddress: "Baku",
                totalPrice: new BigDecimal("25.50"),
                items: []
        )
        orderRepository.findAllByUserId(1L, pageable) >> new PageImpl<>([order], pageable, 1)

        when:
        def result = service.getOrders(1L, pageable)

        then:
        result.content*.id() == [10L]
        result.totalElements == 1
    }

    private static Product product(Long id, String name, String price, int stock, boolean active) {
        new Product(id: id, name: name, price: new BigDecimal(price), stockQuantity: stock, active: active)
    }

    private static Cart cart(Product product, int quantity) {
        def user = new User(id: 1L)
        def cart = new Cart(id: 3L, user: user, items: [])
        cart.items.add(new CartItem(id: 4L, cart: cart, product: product, quantity: quantity))
        cart
    }
}
