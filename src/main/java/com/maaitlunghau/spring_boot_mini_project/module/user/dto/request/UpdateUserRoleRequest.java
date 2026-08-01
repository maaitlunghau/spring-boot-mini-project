package com.maaitlunghau.spring_boot_mini_project.module.user.dto.request;

import com.maaitlunghau.spring_boot_mini_project.module.user.entity.Role;

import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
    @NotNull(message = "Role is required.")
    Role role
) {}
