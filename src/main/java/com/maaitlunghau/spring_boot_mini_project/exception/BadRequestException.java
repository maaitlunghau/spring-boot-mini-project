package com.maaitlunghau.spring_boot_mini_project.exception;

public class BadRequestException extends AppException {
    public BadRequestException(String message) {
        super(message);
    }
}
