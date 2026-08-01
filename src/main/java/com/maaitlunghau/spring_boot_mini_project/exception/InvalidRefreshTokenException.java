package com.maaitlunghau.spring_boot_mini_project.exception;

public class InvalidRefreshTokenException extends AppException {
    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
