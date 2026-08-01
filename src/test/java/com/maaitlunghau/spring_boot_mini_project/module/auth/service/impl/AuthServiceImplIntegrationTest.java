package com.maaitlunghau.spring_boot_mini_project.module.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.maaitlunghau.spring_boot_mini_project.module.auth.dto.request.LoginRequest;
import com.maaitlunghau.spring_boot_mini_project.module.auth.dto.request.RegisterRequest;
import com.maaitlunghau.spring_boot_mini_project.module.auth.dto.response.AuthResponse;
import com.maaitlunghau.spring_boot_mini_project.module.auth.repository.RefreshTokenRepository;
import com.maaitlunghau.spring_boot_mini_project.module.auth.service.AuthService;
import com.maaitlunghau.spring_boot_mini_project.module.user.entity.User;
import com.maaitlunghau.spring_boot_mini_project.module.user.repository.UserRepository;

@SpringBootTest
class AuthServiceImplIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void loginPersistsARefreshTokenRow() {
        String email = "login-test-" + UUID.randomUUID() + "@example.com";
        authService.register(new RegisterRequest("Login Test", email, "password123"));

        AuthResponse response = authService.login(
            new LoginRequest(email, "password123"), "junit-test-device", "127.0.0.1"
        );

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();

        User user = userRepository.findByEmail(email).orElseThrow();
        assertThat(refreshTokenRepository.findByUserIdAndRevokedFalseOrderByCreatedAtDesc(user.getId()))
            .as("issue() must actually persist a refresh token row despite login()'s outer transaction")
            .isNotEmpty();
    }
}
