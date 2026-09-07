package com.example.ordersystem2.config;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CsrfModelAdvice {

    private final CsrfTokenService csrfTokenService;

    public CsrfModelAdvice(CsrfTokenService csrfTokenService) {
        this.csrfTokenService = csrfTokenService;
    }

    @ModelAttribute("csrfToken")
    public String csrfToken(HttpSession session) {
        return csrfTokenService.getOrCreateToken(session);
    }
}
