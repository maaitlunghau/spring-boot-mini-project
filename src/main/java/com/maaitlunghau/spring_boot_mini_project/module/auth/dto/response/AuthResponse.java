package com.maaitlunghau.spring_boot_mini_project.module.auth.dto.response;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    long expiresIn
) {}
