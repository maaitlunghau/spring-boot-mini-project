package com.maaitlunghau.spring_boot_mini_project.module.auth.dto.response;

import java.time.LocalDateTime;

import com.maaitlunghau.spring_boot_mini_project.module.auth.entity.RefreshToken;

public record SessionResponse(
    String sessionId,
    String deviceInfo,
    String ip,
    LocalDateTime createdAt
) {
    public static SessionResponse from(RefreshToken token) {
        return new SessionResponse(
            token.getSessionId(), 
            token.getDeviceInfo(),
            token.getIp(), 
            token.getCreatedAt()
        );
    }
}