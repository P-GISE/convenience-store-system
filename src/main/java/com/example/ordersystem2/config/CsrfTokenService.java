package com.example.ordersystem2.config;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class CsrfTokenService {

    public static final String ATTRIBUTE_NAME = "_csrfToken";

    private final SecureRandom secureRandom = new SecureRandom();

    public String getOrCreateToken(HttpSession session) {
        Object existingToken = session.getAttribute(ATTRIBUTE_NAME);
        if (existingToken instanceof String token && !token.isBlank()) {
            return token;
        }

        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(ATTRIBUTE_NAME, token);
        return token;
    }
}
