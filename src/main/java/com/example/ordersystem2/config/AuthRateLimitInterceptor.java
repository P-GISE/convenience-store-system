package com.example.ordersystem2.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AuthRateLimitInterceptor implements HandlerInterceptor {
    public static final String FAILURE_ATTRIBUTE = AuthRateLimitInterceptor.class.getName() + ".failure";
    public static final String SUCCESS_ATTRIBUTE = AuthRateLimitInterceptor.class.getName() + ".success";
    private static final String ACCOUNT_ATTRIBUTE = AuthRateLimitInterceptor.class.getName() + ".account";
    private static final int MAX_BUCKETS = 4096;
    private final Map<String, Bucket> buckets = new HashMap<>();
    private final Set<String> trustedProxies;
    private final Clock clock;

    @Autowired
    public AuthRateLimitInterceptor(@Value("${app.auth.trusted-proxy-addresses:}") String proxies) {
        this(proxies, Clock.systemUTC());
    }

    AuthRateLimitInterceptor(String proxies, Clock clock) {
        this.trustedProxies = Arrays.stream(proxies.split(",")).map(String::trim)
                .filter(value -> !value.isEmpty()).collect(Collectors.toUnmodifiableSet());
        this.clock = clock;
    }

    @Override
    public synchronized boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getServletPath();
        if (path.isEmpty()) path = request.getRequestURI().substring(request.getContextPath().length());
        if (!"POST".equals(request.getMethod()) || !(path.equals("/login") || path.equals("/signup"))) return true;
        long now = clock.millis();
        long wait = consume("global", 300, 60_000, now);
        if (wait == 0) wait = consume("ip:" + clientAddress(request), 60, 60_000, now);
        String username = request.getParameter("username");
        String account = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
                (username == null ? "" : username.trim().toLowerCase(Locale.ROOT)).getBytes(StandardCharsets.UTF_8)));
        if (wait == 0 && path.equals("/signup")) wait = consume("signup:" + account, 3, 3_600_000, now);
        if (path.equals("/login")) {
            request.setAttribute(ACCOUNT_ATTRIBUTE, "login:" + account);
            Bucket failed = getBucket("login:" + account, 900_000, now);
            if (wait == 0) wait = failed == null ? 60_000 : Math.max(0, failed.blockedUntil - now);
        }
        if (wait == 0) return true;
        response.setStatus(429);
        response.setHeader("Retry-After", Long.toString(Math.max(1, (wait + 999) / 1000)));
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write("요청이 많습니다. 잠시 후 다시 시도해 주세요.");
        return false;
    }

    @Override
    public synchronized void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {
        String key = (String) request.getAttribute(ACCOUNT_ATTRIBUTE);
        if (key == null) return;
        if (Boolean.TRUE.equals(request.getAttribute(SUCCESS_ATTRIBUTE))) {
            buckets.remove(key);
        } else if (Boolean.TRUE.equals(request.getAttribute(FAILURE_ATTRIBUTE))) {
            long now = clock.millis();
            Bucket failed = getBucket(key, 900_000, now);
            if (failed == null) return;
            failed.count++;
            if (failed.count >= 5) failed.blockedUntil = now + Math.min(900_000L, 30_000L << Math.min(5, failed.count - 5));
        }
    }

    private long consume(String key, int limit, long window, long now) {
        Bucket bucket = getBucket(key, window, now);
        if (bucket == null) return 60_000;
        if (bucket.count >= limit) return bucket.expiresAt - now;
        bucket.count++;
        return 0;
    }

    private Bucket getBucket(String key, long window, long now) {
        Bucket bucket = buckets.get(key);
        if (bucket != null && bucket.expiresAt > now) return bucket;
        if (buckets.size() >= MAX_BUCKETS) buckets.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
        if (buckets.size() >= MAX_BUCKETS && !buckets.containsKey(key)) return null;
        bucket = new Bucket(now + window);
        buckets.put(key, bucket);
        return bucket;
    }

    String clientAddress(HttpServletRequest request) {
        String peer = request.getRemoteAddr();
        if (!trustedProxies.contains(peer)) return peer;
        String forwarded = request.getHeader("CF-Connecting-IP");
        if (forwarded == null || !forwarded.matches("[0-9a-fA-F:.]{2,45}")) return peer;
        if (!forwarded.contains(":")) {
            if (!forwarded.matches("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}")) return peer;
            for (String octet : forwarded.split("\\.")) if (Integer.parseInt(octet) > 255) return peer;
        }
        try { return InetAddress.getByName(forwarded).getHostAddress(); }
        catch (Exception invalidAddress) { return peer; }
    }

    private static final class Bucket {
        private final long expiresAt;
        private int count;
        private long blockedUntil;
        private Bucket(long expiresAt) { this.expiresAt = expiresAt; }
    }
}
