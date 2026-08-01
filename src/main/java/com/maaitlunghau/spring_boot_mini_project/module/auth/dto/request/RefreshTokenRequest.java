package com.maaitlunghau.spring_boot_mini_project.module.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
    @NotBlank(message = "Refresh Token is required")
    String refreshToken
) {}
