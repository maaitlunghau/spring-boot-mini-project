package com.maaitlunghau.spring_boot_mini_project.module.auth.service;

import com.maaitlunghau.spring_boot_mini_project.module.auth.dto.request.LoginRequest;
import com.maaitlunghau.spring_boot_mini_project.module.auth.dto.request.RegisterRequest;
import com.maaitlunghau.spring_boot_mini_project.module.auth.dto.response.AuthResponse;

public interface AuthService {
    public void register(RegisterRequest request);
    public AuthResponse login(LoginRequest request, String deviceInfo, String info);
    public AuthResponse refreshToken(String rawRefreshToken, String deviceInfo, String ip);
    public void logout(String accessToken);
}
