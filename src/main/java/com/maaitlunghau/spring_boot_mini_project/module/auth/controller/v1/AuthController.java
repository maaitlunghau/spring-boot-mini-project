package com.maaitlunghau.spring_boot_mini_project.module.auth.controller.v1;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.maaitlunghau.spring_boot_mini_project.common.dto.ApiResponse;
import com.maaitlunghau.spring_boot_mini_project.module.auth.dto.request.LoginRequest;
import com.maaitlunghau.spring_boot_mini_project.module.auth.dto.request.RefreshTokenRequest;
import com.maaitlunghau.spring_boot_mini_project.module.auth.dto.request.RegisterRequest;
import com.maaitlunghau.spring_boot_mini_project.module.auth.dto.response.AuthResponse;
import com.maaitlunghau.spring_boot_mini_project.module.auth.service.AuthService;
import com.maaitlunghau.spring_boot_mini_project.module.auth.service.RefreshTokenService;
import com.maaitlunghau.spring_boot_mini_project.util.CookieUtils;
import com.maaitlunghau.spring_boot_mini_project.util.RequestUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthService authService, RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.message(201, "Registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest servletRequest,
        HttpServletResponse servletResponse
    ) {
        AuthResponse tokens = authService.login(
            request,
            RequestUtils.userAgent(servletRequest),
            RequestUtils.clientIp(servletRequest)
        );
        CookieUtils.setAuthCookies(servletResponse, tokens, refreshTokenService.getRefreshTokenExpirationSeconds());

        return ResponseEntity.ok(ApiResponse.ok("Login successfully", tokens));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
        @RequestBody(required = false) RefreshTokenRequest request,
        HttpServletRequest servletRequest,
        HttpServletResponse servletResponse
    ) {
        String rawRefreshToken = request != null ? request.refreshToken() : null;
        if (rawRefreshToken == null) {
            rawRefreshToken = CookieUtils.readCookie(servletRequest, "refresh_token");
        }

        AuthResponse tokens = authService.refreshToken(
            rawRefreshToken,
            RequestUtils.userAgent(servletRequest),
            RequestUtils.clientIp(servletRequest)
        );
        CookieUtils.setAuthCookies(servletResponse, tokens, refreshTokenService.getRefreshTokenExpirationSeconds());

        return ResponseEntity.ok(ApiResponse.ok("Created new token successfully!", tokens));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        HttpServletRequest servletRequest,
        HttpServletResponse servletResponse
    ) {
        String accessToken = authHeader != null
            ? (authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader)
            : CookieUtils.readCookie(servletRequest, "access_token");

        if (accessToken != null) {
            authService.logout(accessToken);
        }
        CookieUtils.clearAuthCookies(servletResponse);

        return ResponseEntity.ok(ApiResponse.message(200, "Logout successfully"));
    }
}