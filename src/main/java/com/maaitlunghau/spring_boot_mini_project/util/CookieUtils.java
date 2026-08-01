package com.maaitlunghau.spring_boot_mini_project.util;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import com.maaitlunghau.spring_boot_mini_project.module.auth.dto.response.AuthResponse;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class CookieUtils {

    private CookieUtils() {}

    public static void setAuthCookies(HttpServletResponse response, AuthResponse tokens, long refreshTokenMaxAgeSeconds) {
        ResponseCookie accessCookie = ResponseCookie.from("access_token", tokens.accessToken())
            .httpOnly(true)
            .secure(false)
            .path("/")
            .sameSite("Lax")
            .maxAge(Duration.ofSeconds(tokens.expiresIn()))
            .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", tokens.refreshToken())
            .httpOnly(true)
            .secure(false)
            .path("/api/v1/auth/refresh")
            .sameSite("Lax")
            .maxAge(Duration.ofSeconds(refreshTokenMaxAgeSeconds))
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    public static String readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public static void clearAuthCookies(HttpServletResponse response) {
        ResponseCookie accessCookie = ResponseCookie.from("access_token", "")
            .httpOnly(true)
            .path("/")
            .sameSite("Lax")
            .maxAge(0)
            .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "")
            .httpOnly(true)
            .path("/api/v1/auth/refresh")
            .sameSite("Lax")
            .maxAge(0)
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }
}
