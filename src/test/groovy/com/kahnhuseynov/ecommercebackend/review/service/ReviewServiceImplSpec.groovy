package com.kahnhuseynov.ecommercebackend.review.service

import com.kahnhuseynov.ecommercebackend.core.exception.*
import com.kahnhuseynov.ecommercebackend.order.entity.OrderStatus
import com.kahnhuseynov.ecommercebackend.order.repository.OrderRepository
import com.kahnhuseynov.ecommercebackend.product.entity.Product
import com.kahnhuseynov.ecommercebackend.product.repository.ProductRepository
import com.kahnhuseynov.ecommercebackend.review.dto.ReviewRequest
import com.kahnhuseynov.ecommercebackend.review.entity.Review
import com.kahnhuseynov.ecommercebackend.review.repository.ReviewRepository
import com.kahnhuseynov.ecommercebackend.user.entity.User
import com.kahnhuseynov.ecommercebackend.user.repository.UserRepository
import org.springframework.data.domain.*
import spock.lang.Specification

class ReviewServiceImplSpec extends Specification {
    ReviewRepository reviews = Mock()
    ProductRepository products = Mock()
    OrderRepository orders = Mock()
    UserRepository users = Mock()
    def service = new ReviewServiceImpl(reviews, products, orders, users)
    def product = new Product(id: 2L)
    def buyer = new User(id: 1L, firstName: 'Buyer')

    def 'only a non-cancelled purchase permits creating a review'() {
        given:
        products.findWithLockById(2L) >> Optional.of(product)
        orders.existsByUserIdAndStatusNotAndItems_Product_Id(1L, OrderStatus.CANCELLED, 2L) >> true
        users.findById(1L) >> Optional.of(buyer)
        reviews.saveAndFlush(_ as Review) >> { Review r -> r.id = 3L; r }

        when:
        def result = service.create(1L, 2L, new ReviewRequest(5, '  Great product  '))

        then:
        result.id() == 3L
        result.rating() == 5
        result.comment() == 'Great product'
        result.userId() == 1L
        result.productId() == 2L
        result.reviewerName() == 'Buyer'
    }

    def 'buyer without eligible order cannot create a review'() {
        given:
        products.findWithLockById(2L) >> Optional.of(product)

        when:
        service.create(1L, 2L, new ReviewRequest(5, null))

        then:
        thrown(BusinessException)
        0 * reviews.saveAndFlush(_)
    }

    def 'duplicate review is rejected'() {
        given:
        products.findWithLockById(2L) >> Optional.of(product)
        orders.existsByUserIdAndStatusNotAndItems_Product_Id(1L, OrderStatus.CANCELLED, 2L) >> true
        reviews.existsByUserIdAndProductId(1L, 2L) >> true

        when:
        service.create(1L, 2L, new ReviewRequest(5, null))

        then:
        thrown(BusinessException)
        0 * reviews.saveAndFlush(_)
    }

    def 'owner can update rating and remove comment'() {
        given:
        def review = new Review(id: 3L, user: buyer, product: product, rating: 5, comment: 'old')
        products.findWithLockById(2L) >> Optional.of(product)
        reviews.findByIdAndProductId(3L, 2L) >> Optional.of(review)
        reviews.saveAndFlush(review) >> review

        when:
        def result = service.update(1L, 2L, 3L, new ReviewRequest(2, '  '))

        then:
        result.rating() == 2
        result.comment() == null
        0 * orders._
    }

    def 'another user cannot modify a review'() {
        given:
        def review = new Review(id: 3L, user: buyer, product: product, rating: 5)
        products.findWithLockById(2L) >> Optional.of(product)
        reviews.findByIdAndProductId(3L, 2L) >> Optional.of(review)

        when:
        if (deleting) service.delete(9L, false, 2L, 3L)
        else service.update(9L, 2L, 3L, new ReviewRequest(1, 'changed'))

        then:
        thrown(ResourceNotFoundException)
        review.rating == 5
        0 * reviews.saveAndFlush(_)
        0 * reviews.delete(_)

        where:
        deleting << [true, false]
    }

    def 'owner or admin may delete a review'() {
        given:
        def review = new Review(id: 3L, user: buyer, product: product)
        products.findWithLockById(2L) >> Optional.of(product)
        reviews.findByIdAndProductId(3L, 2L) >> Optional.of(review)

        when:
        service.delete(actorId, admin, 2L, 3L)

        then:
        1 * reviews.delete(review)

        where:
        actorId | admin
        1L      | false
        9L      | true
    }

    def 'review lookup is scoped to the product in the URL'() {
        given:
        reviews.findByIdAndProductId(3L, 999L) >> Optional.empty()

        when:
        service.get(999L, 3L)

        then:
        thrown(ResourceNotFoundException)
    }

    def 'listing normalizes sort and adds stable ID ordering'() {
        given:
        products.existsById(2L) >> true
        def expected = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, 'createdAt', 'id'))
        reviews.findAllByProductId(2L, expected) >> new PageImpl<Review>([], expected, 0)

        expect:
        service.getAll(2L, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, 'created_at'))).empty
    }

    def 'unsupported sorting fails before repository query'() {
        given:
        products.existsById(2L) >> true

        when:
        service.getAll(2L, PageRequest.of(0, 10, Sort.by('user.password')))

        then:
        thrown(InvalidRequestException)
        0 * reviews.findAllByProductId(_, _)
    }
}
