package com.example.ordersystem2.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final CsrfInterceptor csrfInterceptor;
    private final AuthRateLimitInterceptor authRateLimitInterceptor;

    public WebMvcConfig(CsrfInterceptor csrfInterceptor, AuthRateLimitInterceptor authRateLimitInterceptor) {
        this.csrfInterceptor = csrfInterceptor;
        this.authRateLimitInterceptor = authRateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(csrfInterceptor);
        registry.addInterceptor(authRateLimitInterceptor).addPathPatterns("/login", "/signup");
    }
}
