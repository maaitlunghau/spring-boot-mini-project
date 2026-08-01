package com.maaitlunghau.spring_boot_mini_project.module.auth.service;

public interface TokenBlacklistService {
    public void blacklist(String jti, long remainingSeconds);
    public boolean isBlacklisted(String jti);
}   
