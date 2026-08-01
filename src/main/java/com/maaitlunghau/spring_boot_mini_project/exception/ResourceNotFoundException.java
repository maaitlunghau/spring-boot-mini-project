package com.maaitlunghau.spring_boot_mini_project.exception;

public class ResourceNotFoundException extends AppException {
    public ResourceNotFoundException(String resource, Object identifier) {
        super(resource + " not found: " + identifier);
    }
}
