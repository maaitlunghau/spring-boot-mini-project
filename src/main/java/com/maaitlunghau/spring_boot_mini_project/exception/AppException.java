package com.maaitlunghau.spring_boot_mini_project.exception;

public class AppException extends RuntimeException {
    protected AppException(String message) {
        super(message);
    }
}
