package com.kahnhuseynov.ecommercebackend.promotion.service

import com.kahnhuseynov.ecommercebackend.category.entity.Category
import com.kahnhuseynov.ecommercebackend.category.repository.CategoryRepository
import com.kahnhuseynov.ecommercebackend.core.exception.*
import com.kahnhuseynov.ecommercebackend.order.entity.*
import com.kahnhuseynov.ecommercebackend.product.entity.Product
import com.kahnhuseynov.ecommercebackend.product.repository.ProductRepository
import com.kahnhuseynov.ecommercebackend.promotion.dto.PromotionRequest
import com.kahnhuseynov.ecommercebackend.promotion.entity.*
import com.kahnhuseynov.ecommercebackend.promotion.repository.*
import com.kahnhuseynov.ecommercebackend.user.entity.User
import spock.lang.Specification
import java.time.LocalDateTime

class PromotionServiceImplSpec extends Specification {
    PromotionRepository promotions = Mock()
    PromotionUsageRepository usages = Mock()
    ProductRepository products = Mock()
    CategoryRepository categories = Mock()
    def service = new PromotionServiceImpl(promotions, usages, products, categories)

    def 'coupon normalizes input and snapshots capped percentage discount'() {
        given:
        def promotion = promotion(code: 'SAVE20', maximumDiscountAmount: 15G)
        def order = order()
        promotions.findLockedByCode('SAVE20') >> Optional.of(promotion)

        when:
        service.apply(order, ' save20 ')

        then:
        order.originalTotalPrice == 100G
        order.discountAmount == 15G
        order.totalPrice == 85G
        order.couponCode == 'SAVE20'
        order.promotionName == 'Summer'
        promotion.usageCount == 1
        1 * usages.save({ PromotionUsage u ->
            u.orderId == 10L && u.userId == 1L && u.promotionId == 2L && u.discountAmount == 15G
        })
        0 * promotions.findAutomaticWithLock()
    }

    def 'fixed discount cannot exceed eligible product subtotal'() {
        given:
        def p = promotion(code: 'FIXED', discountType: DiscountType.FIXED_AMOUNT,
                discountValue: 90G, productId: 3L)
        def order = order()
        promotions.findLockedByCode('FIXED') >> Optional.of(p)

        when:
        service.apply(order, 'FIXED')

        then:
        order.discountAmount == 40G
        order.totalPrice == 60G
    }

    def 'category scope only discounts matching lines and rounds half up'() {
        given:
        def p = promotion(code: 'CATEGORY', categoryId: 7L, discountValue: 12.34G)
        def order = order()
        promotions.findLockedByCode('CATEGORY') >> Optional.of(p)

        when:
        service.apply(order, 'CATEGORY')

        then:
        order.discountAmount == 4.94G
        order.totalPrice == 95.06G
    }

    def 'automatic promotions choose greatest saving without stacking and break ties by repository order'() {
        given:
        def a = promotion(id: 2L, discountValue: 10G)
        def b = promotion(id: 3L, discountType: DiscountType.FIXED_AMOUNT, discountValue: 25G)
        def c = promotion(id: 4L, discountValue: 25G)
        promotions.findAutomaticWithLock() >> [a, b, c]
        def order = order()

        when:
        service.apply(order, null)

        then:
        order.totalPrice == 75G
        order.discountAmount == 25G
        order.couponCode == null
        a.usageCount == 0
        b.usageCount == 1
        c.usageCount == 0
        1 * usages.save({ it.promotionId == 3L })
    }

    def 'inapplicable coupon fails without consuming usage'() {
        given:
        def p = promotion(changes)
        promotions.findLockedByCode('BAD') >> Optional.of(p)
        usages.countByPromotionIdAndUserId(2L, 1L) >> 1L

        when:
        service.apply(order(), 'BAD')

        then:
        thrown(BusinessException)
        p.usageCount == (changes.usageCount ?: 0)
        0 * usages.save(_)

        where:
        changes << [
                [active: false],
                [startsAt: LocalDateTime.now().plusDays(1)],
                [endsAt: LocalDateTime.now().minusSeconds(1)],
                [minimumOrderAmount: 101G],
                [usageLimit: 1, usageCount: 1L],
                [perUserLimit: 1],
                [productId: 999L],
                [categoryId: 999L]
        ]
    }

    def 'unknown coupon is not silently replaced by automatic promotion'() {
        given:
        promotions.findLockedByCode('MISSING') >> Optional.empty()

        when:
        service.apply(order(), 'MISSING')

        then:
        thrown(BusinessException)
        0 * promotions.findAutomaticWithLock()
        0 * usages.save(_)
    }

    def 'no applicable automatic promotion leaves price unchanged'() {
        given:
        promotions.findAutomaticWithLock() >> [promotion(active: false)]
        def order = order()

        when:
        service.apply(order, '')

        then:
        order.totalPrice == 100G
        order.originalTotalPrice == 100G
        order.discountAmount == 0G
        0 * usages.save(_)
    }

    def 'percentage of 100 permits free order but never negative price'() {
        given:
        promotions.findLockedByCode('FREE') >> Optional.of(promotion(discountValue: 100G))
        def order = order()

        when:
        service.apply(order, 'FREE')

        then:
        order.totalPrice == 0G
        order.discountAmount == 100G
    }

    def 'create normalizes coupon and preserves zero initial usage'() {
        given:
        promotions.saveAndFlush(_ as Promotion) >> { Promotion p -> p.id = 9L; p }

        when:
        def result = service.create(request(code: 'save20'))

        then:
        result.code() == 'SAVE20'
        result.usageCount() == 0
    }

    def 'invalid campaign rules are rejected before persistence'() {
        when:
        service.create(request(changes))

        then:
        thrown(InvalidRequestException)
        0 * promotions.saveAndFlush(_)

        where:
        changes << [[discountValue: 101G], [endsAt: LocalDateTime.now().minusDays(2)],
                    [productId: 3L, categoryId: 7L]]
    }

    def 'duplicate codes are rejected case insensitively'() {
        given:
        promotions.existsByCode('SAVE20') >> true

        when:
        service.create(request(code: 'save20'))

        then:
        thrown(BusinessException)
        0 * promotions.saveAndFlush(_)
    }

    def 'deactivation retains campaign and usage history'() {
        given:
        def p = promotion(usageCount: 3L)
        promotions.findLockedById(2L) >> Optional.of(p)

        when:
        service.deactivate(2L)

        then:
        !p.active
        p.usageCount == 3
        0 * promotions.delete(_)
    }

    private static Promotion promotion(Map changes = [:]) {
        new Promotion([id: 2L, name: 'Summer', discountType: DiscountType.PERCENTAGE,
                       discountValue: 20G, minimumOrderAmount: 0G, active: true,
                       startsAt: LocalDateTime.now().minusDays(1),
                       endsAt: LocalDateTime.now().plusDays(1)] + changes)
    }

    private static PromotionRequest request(Map changes = [:]) {
        def r = [name: 'Summer', code: null, discountType: DiscountType.PERCENTAGE,
                 discountValue: 20G, minimumOrderAmount: 0G, maximumDiscountAmount: null,
                 startsAt: LocalDateTime.now().minusDays(1), endsAt: LocalDateTime.now().plusDays(1),
                 active: true, usageLimit: null, perUserLimit: null, productId: null, categoryId: null] + changes
        new PromotionRequest(r.name, r.code, r.discountType, r.discountValue, r.minimumOrderAmount,
                r.maximumDiscountAmount, r.startsAt, r.endsAt, r.active, r.usageLimit, r.perUserLimit,
                r.productId, r.categoryId)
    }

    private static Order order() {
        new Order(id: 10L, user: new User(id: 1L), totalPrice: 100G, items: [
                new OrderItem(product: new Product(id: 3L, category: new Category(id: 7L)), subtotal: 40G),
                new OrderItem(product: new Product(id: 4L), subtotal: 60G)
        ])
    }
}
