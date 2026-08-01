package com.maaitlunghau.spring_boot_mini_project.module.auth.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.maaitlunghau.spring_boot_mini_project.module.auth.entity.RefreshToken;

import jakarta.persistence.LockModeType;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    List<RefreshToken> findByUserIdAndRevokedFalseOrderByCreatedAtDesc(Long userId);
    
    Optional<RefreshToken> findByUserIdAndSessionIdAndRevokedFalse(Long userId, String sessionId);

    @Modifying
    @Query(
        "DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now "
        + "OR (rt.revoked = true AND rt.revokedAt < :revokedRetentionCutoff)"
    )
    void purgeExpiredOrLongRevoked(
        @Param("now") LocalDateTime now,
        @Param("revokedRetentionCutoff") LocalDateTime revokedRetentionCutoff
    );
}
