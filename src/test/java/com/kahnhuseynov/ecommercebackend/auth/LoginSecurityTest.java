package com.kahnhuseynov.ecommercebackend.auth;

import com.kahnhuseynov.ecommercebackend.auth.controller.AuthController;
import com.kahnhuseynov.ecommercebackend.auth.service.AuthServiceImpl;
import com.kahnhuseynov.ecommercebackend.config.SecurityConfig;
import com.kahnhuseynov.ecommercebackend.core.security.CustomUserDetailsService;
import com.kahnhuseynov.ecommercebackend.core.security.jwt.JwtAuthenticationFilter;
import com.kahnhuseynov.ecommercebackend.core.security.jwt.JwtTokenProvider;
import com.kahnhuseynov.ecommercebackend.user.entity.User;
import com.kahnhuseynov.ecommercebackend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Optional;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class, properties = {
        "app.jwt.secret=cHJvbW90aW9uLXRlc3Qtc2VjcmV0LWtleS0zMi1ieXRlcy1sb25n",
        "app.jwt.expiration-ms=86400000"
})
@Import({SecurityConfig.class, AuthServiceImpl.class, CustomUserDetailsService.class,
        JwtAuthenticationFilter.class, JwtTokenProvider.class})
class LoginSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @MockitoBean UserRepository users;
    private User account;

    @BeforeEach
    void setUp() {
        account = new User();
        account.setId(1L);
        account.setEmail("login@test.example");
        account.setPassword(encoder.encode("LocalTestPassword!"));
        account.setEnabled(true);
        when(users.findByEmail(account.getEmail())).thenReturn(Optional.of(account));
    }

    @Test
    void validCredentialsReturnTokenWithoutAdminRoleOrExistingToken() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"login@test.example\",\"password\":\"LocalTestPassword!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void wrongPasswordReturnsStructuredUnauthorizedResponse() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"login@test.example\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password, or account is unavailable."));
    }

    @Test
    void unknownAccountReturnsSameUnauthorizedResponse() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"missing@test.example\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password, or account is unavailable."));
    }

    @Test
    void disabledAccountCannotLogin() throws Exception {
        account.setEnabled(false);
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"login@test.example\",\"password\":\"LocalTestPassword!\"}"))
                .andExpect(status().isUnauthorized());
    }
    @Test
    void missingTokenReturnsUnauthorized() throws Exception {
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/promotions")).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokensReturnUnauthorized() throws Exception {
        for (String token : new String[]{"", "invalid-token", signedToken(-60_000, false),
                signedToken(60_000, true)}) {
            mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void validTokenAuthenticatesButDoesNotGrantAdminAccess() throws Exception {
        String token = signedToken(60_000, false);
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(account.getEmail()));
        mvc.perform(get("/api/v1/promotions").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void tokenForDisabledOrDeletedUserReturnsUnauthorized() throws Exception {
        String token = signedToken(60_000, false);
        account.setEnabled(false);
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        when(users.findByEmail(account.getEmail())).thenReturn(Optional.empty());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private String signedToken(long expiresInMs, boolean wrongKey) {
        var key = wrongKey ? Jwts.SIG.HS256.key().build() : Keys.hmacShaKeyFor(Decoders.BASE64.decode(
                "cHJvbW90aW9uLXRlc3Qtc2VjcmV0LWtleS0zMi1ieXRlcy1sb25n"));
        return Jwts.builder().subject(account.getEmail())
                .expiration(new Date(System.currentTimeMillis() + expiresInMs))
                .signWith(key).compact();
    }

}
