package com.kahnhuseynov.ecommercebackend.promotion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kahnhuseynov.ecommercebackend.cart.entity.*;
import com.kahnhuseynov.ecommercebackend.cart.repository.CartRepository;
import com.kahnhuseynov.ecommercebackend.core.exception.BusinessException;
import com.kahnhuseynov.ecommercebackend.order.dto.CheckoutRequest;
import com.kahnhuseynov.ecommercebackend.order.repository.OrderRepository;
import com.kahnhuseynov.ecommercebackend.order.service.OrderService;
import com.kahnhuseynov.ecommercebackend.product.entity.Product;
import com.kahnhuseynov.ecommercebackend.product.repository.ProductRepository;
import com.kahnhuseynov.ecommercebackend.promotion.dto.PromotionRequest;
import com.kahnhuseynov.ecommercebackend.promotion.entity.DiscountType;
import com.kahnhuseynov.ecommercebackend.promotion.repository.*;
import com.kahnhuseynov.ecommercebackend.promotion.service.PromotionService;
import com.kahnhuseynov.ecommercebackend.user.entity.User;
import com.kahnhuseynov.ecommercebackend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Opt in with a disposable PostgreSQL database; Liquibase and Hibernate validation run against it.
@SpringBootTest(properties = {
        "spring.datasource.url=${PROMOTION_TEST_DB_URL}",
        "spring.datasource.username=${PROMOTION_TEST_DB_USER:khan}",
        "spring.datasource.password=${PROMOTION_TEST_DB_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.show-sql=false",
        "app.jwt.secret=cHJvbW90aW9uLXRlc3Qtc2VjcmV0LWtleS0zMi1ieXRlcy1sb25n"
})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "PROMOTION_TEST_DB_URL", matches = ".+")
class PromotionIntegrationTest {
    @Autowired PromotionService promotions;
    @Autowired PromotionRepository promotionRepository;
    @Autowired PromotionUsageRepository usages;
    @Autowired OrderService orders;
    @Autowired OrderRepository orderRepository;
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired CartRepository carts;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void checkoutPersistsDiscountAndUsageAndKeepsHistoricalPriceAfterCampaignEdit() {
        Fixture f = fixture();
        String code = code();
        var p = promotions.create(request(code, 10));
        var order = orders.checkout(f.userId(), new CheckoutRequest("Baku", code.toLowerCase()));
        assertEquals(0, new BigDecimal("80.00").compareTo(order.totalPrice()));
        assertEquals(0, new BigDecimal("100.00").compareTo(order.originalTotalPrice()));
        assertEquals(0, new BigDecimal("20.00").compareTo(order.discountAmount()));
        assertEquals(code, order.couponCode());
        assertEquals(1, usages.countByPromotionIdAndUserId(p.id(), f.userId()));
        assertEquals(1, promotionRepository.findById(p.id()).orElseThrow().getUsageCount());
        assertEquals(4, products.findById(f.productId()).orElseThrow().getStockQuantity());
        assertTrue(carts.findByUserId(f.userId()).orElseThrow().getItems().isEmpty());
        promotions.deactivate(p.id());
        assertEquals(order.totalPrice(), orders.getOrder(f.userId(), order.id()).totalPrice());
    }

    @Test
    void automaticProductCampaignAppliesWithoutCouponAndDoesNotStack() {
        Fixture f = fixture();
        var automatic = promotions.create(new PromotionRequest("Automatic product discount", null,
                DiscountType.FIXED_AMOUNT, new BigDecimal("35.00"), BigDecimal.ZERO, null,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                true, null, null, f.productId(), null));
        var order = orders.checkout(f.userId(), new CheckoutRequest("Baku"));
        assertEquals(0, new BigDecimal("65.00").compareTo(order.totalPrice()));
        assertNull(order.couponCode());
        assertEquals("Automatic product discount", order.promotionName());
        assertEquals(1, usages.countByPromotionIdAndUserId(automatic.id(), f.userId()));
    }

    @Test
    void perUserLimitSurvivesRefillingCartAndRejectsSecondOrder() {
        Fixture f = fixture();
        String code = code();
        var p = promotions.create(request(code, 10));
        orders.checkout(f.userId(), new CheckoutRequest("Baku", code));
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Cart cart = carts.findByUserId(f.userId()).orElseThrow();
            CartItem item = new CartItem(); item.setCart(cart);
            item.setProduct(products.findById(f.productId()).orElseThrow()); item.setQuantity(1);
            cart.getItems().add(item);
        });
        assertThrows(BusinessException.class,
                () -> orders.checkout(f.userId(), new CheckoutRequest("Baku", code)));
        assertEquals(1, usages.countByPromotionIdAndUserId(p.id(), f.userId()));
        assertEquals(4, products.findById(f.productId()).orElseThrow().getStockQuantity());
        assertEquals(1, carts.findByUserId(f.userId()).orElseThrow().getItems().size());
    }

    @Test
    void rejectedCouponRollsBackOrderStockAndCart() {
        Fixture f = fixture();
        long count = orderRepository.count();
        assertThrows(BusinessException.class,
                () -> orders.checkout(f.userId(), new CheckoutRequest("Baku", "MISSING")));
        assertEquals(count, orderRepository.count());
        assertEquals(5, products.findById(f.productId()).orElseThrow().getStockQuantity());
        assertEquals(1, carts.findByUserId(f.userId()).orElseThrow().getItems().size());
    }

    @Test
    void concurrentCheckoutsCannotExceedGlobalUsageLimit() throws Exception {
        Fixture first = fixture();
        Fixture second = fixture();
        String code = code();
        var p = promotions.create(request(code, 1));
        var start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> a = executor.submit(() -> checkoutAfter(start, first.userId(), code));
            Future<Boolean> b = executor.submit(() -> checkoutAfter(start, second.userId(), code));
            start.countDown();
            boolean aSucceeded = a.get(20, TimeUnit.SECONDS);
            boolean bSucceeded = b.get(20, TimeUnit.SECONDS);
            assertNotEquals(aSucceeded, bSucceeded);
            assertEquals(1, promotionRepository.findById(p.id()).orElseThrow().getUsageCount());
            assertEquals(1, usages.countByPromotionIdAndUserId(p.id(), first.userId())
                    + usages.countByPromotionIdAndUserId(p.id(), second.userId()));
            Fixture failed = aSucceeded ? second : first;
            assertEquals(5, products.findById(failed.productId()).orElseThrow().getStockQuantity());
            assertEquals(1, carts.findByUserId(failed.userId()).orElseThrow().getItems().size());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void repeatedConcurrentCheckoutOfSameCartCreatesOnlyOneOrder() throws Exception {
        Fixture f = fixture();
        String code = code();
        var p = promotions.create(request(code, 10));
        var start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> a = executor.submit(() -> checkoutAfter(start, f.userId(), code));
            Future<Boolean> b = executor.submit(() -> checkoutAfter(start, f.userId(), code));
            start.countDown();
            assertNotEquals(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
            assertEquals(1, promotionRepository.findById(p.id()).orElseThrow().getUsageCount());
            assertEquals(4, products.findById(f.productId()).orElseThrow().getStockQuantity());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void promotionEndpointsRequireAdminAndValidateRequests() throws Exception {
        mvc.perform(get("/api/v1/promotions")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/promotions").with(user("buyer").roles("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/promotions").with(user("buyer").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(request(code(), 5))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/promotions").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        String code = code();
        mvc.perform(post("/api/v1/promotions").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(request(code, 5))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value(code));
        mvc.perform(get("/api/v1/promotions?sort=unsupported").with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    private boolean checkoutAfter(CountDownLatch start, Long userId, String code) throws InterruptedException {
        start.await();
        try {
            orders.checkout(userId, new CheckoutRequest("Baku", code));
            return true;
        } catch (BusinessException expected) {
            return false;
        }
    }

    private Fixture fixture() {
        return new TransactionTemplate(transactionManager).execute(status -> {
            User u = new User();
            u.setFirstName("Test"); u.setLastName("Buyer");
            u.setEmail(UUID.randomUUID() + "@test.example"); u.setPassword("unused");
            users.save(u);
            Product product = new Product();
            product.setName("Test product"); product.setPrice(new BigDecimal("100.00"));
            product.setStockQuantity(5); products.save(product);
            Cart cart = new Cart(); cart.setUser(u);
            CartItem item = new CartItem(); item.setCart(cart); item.setProduct(product); item.setQuantity(1);
            cart.getItems().add(item); carts.save(cart);
            return new Fixture(u.getId(), product.getId());
        });
    }

    private static PromotionRequest request(String code, int limit) {
        return new PromotionRequest("Test campaign", code, DiscountType.PERCENTAGE,
                new BigDecimal("20.00"), BigDecimal.ZERO, null,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                true, limit, 1, null, null);
    }

    private static String code() { return "TEST_" + UUID.randomUUID().toString().replace("-", "").toUpperCase(); }
    private record Fixture(Long userId, Long productId) {}
}
