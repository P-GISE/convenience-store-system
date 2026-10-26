package com.example.ordersystem2.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import static org.assertj.core.api.Assertions.assertThat;

class AuthRateLimitInterceptorTest {
    private static class MutableClock extends Clock {
        private long now = 1_000_000;
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return Instant.ofEpochMilli(now); }
    }
    private MockHttpServletRequest login(String account) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        request.setRemoteAddr("203.0.113.1");
        request.addParameter("username", account);
        return request;
    }
    @Test
    void acceptsClientHeaderOnlyFromAnExplicitExactNumericPeer() {
        AuthRateLimitInterceptor limiter = new AuthRateLimitInterceptor("172.25.0.1");
        MockHttpServletRequest request = login("synthetic");
        request.addHeader("CF-Connecting-IP", "198.51.100.10");
        assertThat(limiter.clientAddress(request)).isEqualTo("203.0.113.1");
        request.setRemoteAddr("172.25.0.1");
        assertThat(limiter.clientAddress(request)).isEqualTo("198.51.100.10");
        request.removeHeader("CF-Connecting-IP");
        request.addHeader("CF-Connecting-IP", "198.51.100.10, 198.51.100.11");
        assertThat(limiter.clientAddress(request)).isEqualTo("172.25.0.1");
    }
    @Test
    void failedLoginCooldownExpiresAndIncreasesWithoutBlockingOtherAccounts() throws Exception {
        MutableClock clock = new MutableClock();
        AuthRateLimitInterceptor limiter = new AuthRateLimitInterceptor("", clock);
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = login("synthetic");
            assertThat(limiter.preHandle(request, new MockHttpServletResponse(), null)).isTrue();
            request.setAttribute(AuthRateLimitInterceptor.FAILURE_ATTRIBUTE, true);
            limiter.afterCompletion(request, new MockHttpServletResponse(), null, null);
        }
        MockHttpServletResponse rejected = new MockHttpServletResponse();
        assertThat(limiter.preHandle(login("synthetic"), rejected, null)).isFalse();
        assertThat(rejected.getHeader("Retry-After")).isEqualTo("30");
        assertThat(limiter.preHandle(login("other"), new MockHttpServletResponse(), null)).isTrue();
        clock.now += 30_001;
        MockHttpServletRequest retried = login("synthetic");
        assertThat(limiter.preHandle(retried, new MockHttpServletResponse(), null)).isTrue();
        retried.setAttribute(AuthRateLimitInterceptor.FAILURE_ATTRIBUTE, true);
        limiter.afterCompletion(retried, new MockHttpServletResponse(), null, null);
        rejected = new MockHttpServletResponse();
        assertThat(limiter.preHandle(login("synthetic"), rejected, null)).isFalse();
        assertThat(rejected.getHeader("Retry-After")).isEqualTo("60");
        clock.now += 900_001;
        assertThat(limiter.preHandle(login("synthetic"), new MockHttpServletResponse(), null)).isTrue();
    }
}
