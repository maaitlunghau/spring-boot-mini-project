package com.maaitlunghau.spring_boot_mini_project.exception;

public class RefreshTokenReuseException extends AppException {
    public RefreshTokenReuseException(String message) {
        super(message);
    }
}
