package com.maaitlunghau.spring_boot_mini_project.module.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @Email(message = "Email is invalid.") 
    @NotBlank(message = "Email is required.") 
    String email,

    @NotBlank(message = "Password is required.") 
    String password
) {}
