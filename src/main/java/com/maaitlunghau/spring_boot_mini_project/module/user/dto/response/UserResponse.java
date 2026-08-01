package com.maaitlunghau.spring_boot_mini_project.module.user.dto.response;

import java.time.LocalDateTime;

import com.maaitlunghau.spring_boot_mini_project.module.user.entity.Role;
import com.maaitlunghau.spring_boot_mini_project.module.user.entity.User;

public record UserResponse(
    Long id,
    String fullName,
    String email,
    String imageUrl,
    Role role,
    boolean enabled,
    LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(), 
            user.getFullName(), 
            user.getEmail(), 
            user.getImageUrl(),
            user.getRole(), 
            user.isEnabled(), 
            user.getCreatedAt()
        );
    }
}
