package com.kahnhuseynov.ecommercebackend.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kahnhuseynov.ecommercebackend.core.exception.BusinessException;
import com.kahnhuseynov.ecommercebackend.core.security.CustomUserPrincipal;
import com.kahnhuseynov.ecommercebackend.order.entity.*;
import com.kahnhuseynov.ecommercebackend.order.repository.OrderRepository;
import com.kahnhuseynov.ecommercebackend.product.entity.Product;
import com.kahnhuseynov.ecommercebackend.product.repository.ProductRepository;
import com.kahnhuseynov.ecommercebackend.product.service.ProductService;
import com.kahnhuseynov.ecommercebackend.review.dto.ReviewRequest;
import com.kahnhuseynov.ecommercebackend.review.repository.ReviewRepository;
import com.kahnhuseynov.ecommercebackend.review.service.ReviewService;
import com.kahnhuseynov.ecommercebackend.user.entity.*;
import com.kahnhuseynov.ecommercebackend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=${REVIEW_TEST_DB_URL}",
        "spring.datasource.username=${REVIEW_TEST_DB_USER:khan}",
        "spring.datasource.password=${REVIEW_TEST_DB_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.show-sql=false",
        "app.jwt.secret=cHJvbW90aW9uLXRlc3Qtc2VjcmV0LWtleS0zMi1ieXRlcy1sb25n"
})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "REVIEW_TEST_DB_URL", matches = ".+")
class ReviewIntegrationTest {
    @Autowired ReviewService reviews;
    @Autowired ReviewRepository reviewRepository;
    @Autowired ProductService productService;
    @Autowired ProductRepository products;
    @Autowired UserRepository users;
    @Autowired OrderRepository orders;
    @Autowired PlatformTransactionManager transactions;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void buyerCanCreateUpdateAndDeleteWhilePublicReadsExposeAccurateStatistics() throws Exception {
        Fixture f = fixture(OrderStatus.PLACED);
        mvc.perform(get("/api/v1/products/{id}", f.productId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.average_rating").value(0))
                .andExpect(jsonPath("$.review_count").value(0));
        String body = mvc.perform(post(path(f)).with(user(f.principal()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":5,\"comment\":\"  Good  \"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.comment").value("Good"))
                .andExpect(jsonPath("$.user_id").value(f.userId())).andReturn().getResponse().getContentAsString();
        Long reviewId = mapper.readTree(body).get("id").asLong();
        mvc.perform(get(path(f))).andExpect(status().isOk())
                .andExpect(jsonPath("$.total_elements").value(1))
                .andExpect(jsonPath("$.content[0].rating").value(5))
                .andExpect(jsonPath("$.content[0].email").doesNotExist())
                .andExpect(jsonPath("$.content[0].password").doesNotExist());
        mvc.perform(get("/api/v1/products/{id}", f.productId()))
                .andExpect(jsonPath("$.average_rating").value(5)).andExpect(jsonPath("$.review_count").value(1));
        mvc.perform(put(path(f) + "/" + reviewId).with(user(f.principal()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":3}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rating").value(3));
        assertEquals(new BigDecimal("3.00"), productService.getProductById(f.productId()).averageRating());
        mvc.perform(delete(path(f) + "/" + reviewId).with(user(f.principal())))
                .andExpect(status().isNoContent());
        assertEquals(0L, productService.getProductById(f.productId()).reviewCount());
        assertEquals(0, productService.getProductById(f.productId()).averageRating().signum());
    }

    @Test
    void averageAcrossBuyersIsRoundedAndAppearsInProductSearch() throws Exception {
        Fixture f = fixture(OrderStatus.PLACED);
        reviews.create(f.userId(), f.productId(), new ReviewRequest(5, null));
        Long second = buyerAndOrder(f.productId(), OrderStatus.DELIVERED);
        Long third = buyerAndOrder(f.productId(), OrderStatus.SHIPPED);
        reviews.create(second, f.productId(), new ReviewRequest(4, null));
        reviews.create(third, f.productId(), new ReviewRequest(4, null));
        var response = productService.getProductById(f.productId());
        assertEquals(new BigDecimal("4.33"), response.averageRating());
        assertEquals(3L, response.reviewCount());
        // A newly created product is newest by ID; search must use the same product mapper.
        mvc.perform(get("/api/v1/products?sort=id,desc&size=1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(f.productId()))
                .andExpect(jsonPath("$.content[0].average_rating").value(4.33));
    }

    @Test
    void noPurchaseOrOnlyCancelledPurchaseCannotReview() throws Exception {
        Fixture cancelled = fixture(OrderStatus.CANCELLED);
        mvc.perform(post(path(cancelled)).with(user(cancelled.principal()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":4}"))
                .andExpect(status().isConflict());
        Fixture another = fixture(OrderStatus.PLACED);
        mvc.perform(post(path(another)).with(user(cancelled.principal()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":4}"))
                .andExpect(status().isConflict());
    }

    @Test
    void otherUserCannotEditOrDeleteButAdminCanDelete() throws Exception {
        Fixture owner = fixture(OrderStatus.PLACED);
        Fixture stranger = fixture(OrderStatus.PLACED);
        var review = reviews.create(owner.userId(), owner.productId(), new ReviewRequest(5, null));
        String url = path(owner) + "/" + review.id();
        mvc.perform(put(url).with(user(stranger.principal())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":1}")).andExpect(status().isNotFound());
        mvc.perform(delete(url).with(user(stranger.principal()))).andExpect(status().isNotFound());
        User admin = new User(); admin.setId(stranger.userId()); admin.setEmail("admin@test.example");
        admin.setPassword("unused"); admin.setRoles(Set.of(new Role(99L, "ROLE_ADMIN")));
        var adminPrincipal = new CustomUserPrincipal(admin);
        mvc.perform(put(url).with(user(adminPrincipal)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":1}")).andExpect(status().isNotFound());
        mvc.perform(delete(url).with(user(adminPrincipal))).andExpect(status().isNoContent());
        assertEquals(0L, productService.getProductById(owner.productId()).reviewCount());
    }

    @Test
    void duplicateAndWrongProductPathsAreRejected() throws Exception {
        Fixture f = fixture(OrderStatus.PLACED);
        var r = reviews.create(f.userId(), f.productId(), new ReviewRequest(5, null));
        mvc.perform(post(path(f)).with(user(f.principal())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":4}")).andExpect(status().isConflict());
        Fixture other = fixture(OrderStatus.PLACED);
        mvc.perform(get(path(other) + "/" + r.id())).andExpect(status().isNotFound());
        mvc.perform(delete(path(other) + "/" + r.id()).with(user(f.principal())))
                .andExpect(status().isNotFound());
        assertTrue(reviewRepository.existsById(r.id()));
    }

    @Test
    void validationAndAuthenticationCannotBeBypassed() throws Exception {
        Fixture f = fixture(OrderStatus.PLACED);
        mvc.perform(post(path(f)).contentType(MediaType.APPLICATION_JSON).content("{\"rating\":5}"))
                .andExpect(status().isForbidden());
        for (String body : List.of("{}", "{\"rating\":0}", "{\"rating\":6}",
                mapper.writeValueAsString(new ReviewRequest(5, "x".repeat(2001))))) {
            mvc.perform(post(path(f)).with(user(f.principal())).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        // Allowing review writes must not grant buyers permission to change products.
        mvc.perform(delete("/api/v1/products/{id}", f.productId()).with(user(f.principal())))
                .andExpect(status().isForbidden());
        mvc.perform(get(path(f) + "?sort=user.password")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/products/9223372036854775807/reviews")).andExpect(status().isNotFound());
    }

    @Test
    void paginationAndRatingSortAreScopedToProduct() throws Exception {
        Fixture f = fixture(OrderStatus.PLACED);
        reviews.create(f.userId(), f.productId(), new ReviewRequest(2, null));
        Long second = buyerAndOrder(f.productId(), OrderStatus.PROCESSING);
        reviews.create(second, f.productId(), new ReviewRequest(5, null));
        mvc.perform(get(path(f) + "?sort=rating,desc&size=1&page=0"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total_elements").value(2))
                .andExpect(jsonPath("$.content[0].rating").value(5)).andExpect(jsonPath("$.has_next").value(true));
        mvc.perform(get(path(f) + "?sort=rating,desc&size=1&page=1"))
                .andExpect(jsonPath("$.content[0].rating").value(2));
    }

    @Test
    void concurrentRequestsCannotCreateTwoReviewsForSameUserAndProduct() throws Exception {
        Fixture f = fixture(OrderStatus.PLACED);
        var start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Callable<Boolean> task = () -> {
            start.await();
            try { reviews.create(f.userId(), f.productId(), new ReviewRequest(5, null)); return true; }
            catch (BusinessException expected) { return false; }
        };
        try {
            Future<Boolean> first = executor.submit(task);
            Future<Boolean> second = executor.submit(task);
            start.countDown();
            assertNotEquals(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
            assertEquals(1L, productService.getProductById(f.productId()).reviewCount());
        } finally { executor.shutdownNow(); }
    }

    private Fixture fixture(OrderStatus status) {
        return new TransactionTemplate(transactions).execute(tx -> {
            Product p = new Product(); p.setName("Review test product"); p.setPrice(new BigDecimal("10.00"));
            p.setStockQuantity(20); products.save(p);
            User buyer = createBuyer(); createOrder(buyer, p, status);
            return new Fixture(buyer.getId(), p.getId(), new CustomUserPrincipal(buyer));
        });
    }

    private Long buyerAndOrder(Long productId, OrderStatus status) {
        return new TransactionTemplate(transactions).execute(tx -> {
            User buyer = createBuyer(); createOrder(buyer, products.findById(productId).orElseThrow(), status);
            return buyer.getId();
        });
    }

    private User createBuyer() {
        User u = new User(); u.setFirstName("Buyer"); u.setLastName("Test");
        u.setEmail(UUID.randomUUID() + "@test.example"); u.setPassword("unused");
        return users.save(u);
    }

    private void createOrder(User buyer, Product product, OrderStatus status) {
        Order order = new Order(); order.setUser(buyer); order.setStatus(status); order.setShippingAddress("Baku");
        order.setTotalPrice(product.getPrice()); order.setOriginalTotalPrice(product.getPrice());
        OrderItem item = new OrderItem(); item.setOrder(order); item.setProduct(product);
        item.setProductName(product.getName()); item.setQuantity(1); item.setUnitPrice(product.getPrice());
        item.setSubtotal(product.getPrice()); order.getItems().add(item); orders.save(order);
    }

    private static String path(Fixture f) { return "/api/v1/products/" + f.productId() + "/reviews"; }
    private record Fixture(Long userId, Long productId, CustomUserPrincipal principal) {}
}
