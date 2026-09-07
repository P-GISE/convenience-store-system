package com.example.ordersystem2.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

@Component
public class CsrfInterceptor implements HandlerInterceptor {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (SAFE_METHODS.contains(request.getMethod())) {
            return true;
        }

        HttpSession session = request.getSession(false);
        String expectedToken = session == null ? null : (String) session.getAttribute(CsrfTokenService.ATTRIBUTE_NAME);
        String actualToken = request.getParameter("_csrf");

        if (tokensMatch(expectedToken, actualToken)) {
            return true;
        }

        response.sendError(HttpServletResponse.SC_FORBIDDEN, "잘못된 요청입니다. 화면을 새로고침한 뒤 다시 시도해주세요.");
        return false;
    }

    private boolean tokensMatch(String expectedToken, String actualToken) {
        if (expectedToken == null || actualToken == null) {
            return false;
        }

        byte[] expected = expectedToken.getBytes(StandardCharsets.UTF_8);
        byte[] actual = actualToken.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }
}
