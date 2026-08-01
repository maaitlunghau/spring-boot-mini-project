package com.maaitlunghau.spring_boot_mini_project.exception;

public class DuplicateResourceException extends AppException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
