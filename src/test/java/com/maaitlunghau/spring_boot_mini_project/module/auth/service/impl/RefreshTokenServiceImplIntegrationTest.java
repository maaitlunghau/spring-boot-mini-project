package com.maaitlunghau.spring_boot_mini_project.module.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.maaitlunghau.spring_boot_mini_project.exception.RefreshTokenReuseException;
import com.maaitlunghau.spring_boot_mini_project.module.auth.repository.RefreshTokenRepository;
import com.maaitlunghau.spring_boot_mini_project.module.auth.service.RefreshTokenService;
import com.maaitlunghau.spring_boot_mini_project.module.user.entity.Role;
import com.maaitlunghau.spring_boot_mini_project.module.user.entity.User;
import com.maaitlunghau.spring_boot_mini_project.module.user.repository.UserRepository;

@SpringBootTest
class RefreshTokenServiceImplIntegrationTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void reuseDetectionRevocationSurvivesTheThrownException() {
        String email = "reuse-test-" + UUID.randomUUID() + "@example.com";
        User user = userRepository.save(new User("Reuse Test", email, "encoded-password", Role.USER));
        String sessionId = UUID.randomUUID().toString();

        String rawToken1 = refreshTokenService.issue(user.getId(), sessionId, "device-a", "127.0.0.1");
        // Normal rotation: token1 becomes revoked (ROTATED), token2 is the new active token.
        refreshTokenService.rotate(rawToken1, "device-a", "127.0.0.1");

        // Replaying the old (now-revoked) token simulates theft/reuse.
        assertThatThrownBy(() -> refreshTokenService.rotate(rawToken1, "device-a", "127.0.0.1"))
            .isInstanceOf(RefreshTokenReuseException.class);

        assertThat(refreshTokenRepository.findByUserIdAndRevokedFalseOrderByCreatedAtDesc(user.getId()))
            .as("reuse detection must revoke ALL sessions durably, even though rotate() then throws")
            .isEmpty();
    }
}
