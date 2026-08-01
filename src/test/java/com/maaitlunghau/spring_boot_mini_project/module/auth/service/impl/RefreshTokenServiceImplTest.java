package com.maaitlunghau.spring_boot_mini_project.module.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.maaitlunghau.spring_boot_mini_project.exception.RefreshTokenReuseException;
import com.maaitlunghau.spring_boot_mini_project.module.auth.entity.RefreshToken;
import com.maaitlunghau.spring_boot_mini_project.module.auth.entity.RevokeReason;
import com.maaitlunghau.spring_boot_mini_project.module.auth.repository.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenServiceImpl refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenServiceImpl(refreshTokenRepository, 300_000L, 2_592_000_000L);
        // Self-inject so rotate()'s call to self.revokeAllSessions(...) reaches the real method under test.
        ReflectionTestUtils.setField(refreshTokenService, "self", refreshTokenService);
    }

    @Test
    void rotateDetectsReuseEvenWhenTheRevokedTokenIsAlsoExpired() {
        RefreshToken revokedAndExpiredToken = new RefreshToken(
            1L, "session-1", "irrelevant-hash", "device", "127.0.0.1",
            LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1)
        );
        revokedAndExpiredToken.revoke(RevokeReason.ROTATED);

        when(refreshTokenRepository.findByTokenHashForUpdate(any()))
            .thenReturn(Optional.of(revokedAndExpiredToken));
        when(refreshTokenRepository.findByUserIdAndRevokedFalseOrderByCreatedAtDesc(1L))
            .thenReturn(List.of());

        assertThatThrownBy(() -> refreshTokenService.rotate("stolen-raw-token", "device", "127.0.0.1"))
            .as("a revoked token that is also expired must still be treated as reuse, not just 'expired'")
            .isInstanceOf(RefreshTokenReuseException.class);

        verify(refreshTokenRepository).findByUserIdAndRevokedFalseOrderByCreatedAtDesc(1L);
    }
}
