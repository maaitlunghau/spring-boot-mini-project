package com.maaitlunghau.spring_boot_mini_project.util;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestUtils {

    private RequestUtils() {};

    /**
     * This app has no reverse proxy in front of it, so X-Forwarded-For is
     * attacker-controlled and must not be trusted for rate limiting or
     * auditing. Always use the actual TCP peer address.
     */
    public static String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    public static String userAgent(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return ua != null ? ua : "unknown";
    }
}