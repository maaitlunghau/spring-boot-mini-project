package com.maaitlunghau.spring_boot_mini_project.module.user.dto.request;

public record UpdateProfileRequest(
    String fullName,
    String imageUrl
) {}
