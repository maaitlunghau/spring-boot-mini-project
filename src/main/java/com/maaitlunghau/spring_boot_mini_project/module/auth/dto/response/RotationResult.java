package com.maaitlunghau.spring_boot_mini_project.module.auth.dto.response;

public record RotationResult(
    String newRawRefreshToken, 
    Long userId, 
    String sessionId
) {}