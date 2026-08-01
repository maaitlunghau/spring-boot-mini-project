package com.maaitlunghau.spring_boot_mini_project.module.auth.service;

import java.util.List;

import com.maaitlunghau.spring_boot_mini_project.module.auth.dto.response.RotationResult;
import com.maaitlunghau.spring_boot_mini_project.module.auth.entity.RefreshToken;
import com.maaitlunghau.spring_boot_mini_project.module.auth.entity.RevokeReason;

public interface RefreshTokenService {

    public String issue(Long userId, String sessionId, String deviceInfo, String ip);

    public RotationResult rotate(String rawOldToken, String deviceInfo, String ip);

    public void revokeSession(Long userId, String sessionId, RevokeReason reason);

    public void revokeAllSessions(Long userId, RevokeReason reason);

    public List<RefreshToken> listActiveSessions(Long userId);

    public long getRefreshTokenExpirationSeconds();
}
