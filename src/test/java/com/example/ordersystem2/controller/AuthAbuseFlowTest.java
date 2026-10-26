package com.example.ordersystem2.controller;

import com.example.ordersystem2.config.CsrfTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.session.store-type=none")
@AutoConfigureMockMvc(addFilters = false)
class AuthAbuseFlowTest {
    @Autowired MockMvc mvc;
    @Autowired CsrfTokenService csrf;

    @Test
    void failedAccountCannotBypassCooldownBySpoofingForwardedHeaders() throws Exception {
        String account = "missing-" + UUID.randomUUID();
        MockHttpSession session = new MockHttpSession();
        String token = csrf.getOrCreateToken(session);
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/login").session(session).param("_csrf", token)
                    .param("username", account).param("password", "synthetic-password")
                    .header("CF-Connecting-IP", "203.0.113." + i))
                    .andExpect(status().isOk());
        }
        mvc.perform(post("/login").session(session).param("_csrf", token)
                .param("username", account).param("password", "synthetic-password")
                .header("X-Forwarded-For", "198.51.100.99"))
                .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
        mvc.perform(post("/login").session(session).param("_csrf", token)
                .param("username", "other-" + UUID.randomUUID()).param("password", "synthetic-password"))
                .andExpect(status().isOk());
    }

    @Test
    void signupRequestsAreBoundedBeforeValidationOrDatabaseWrites() throws Exception {
        String account = "signup-" + UUID.randomUUID();
        MockHttpSession session = new MockHttpSession();
        String token = csrf.getOrCreateToken(session);
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/signup").session(session).param("_csrf", token).param("username", account))
                    .andExpect(status().isOk());
        }
        mvc.perform(post("/signup").session(session).param("_csrf", token).param("username", account))
                .andExpect(status().isTooManyRequests());
    }
}
