package com.maaitlunghau.spring_boot_mini_project.module.user.service;

import org.springframework.data.domain.Pageable;

import com.maaitlunghau.spring_boot_mini_project.common.dto.PageResponse;
import com.maaitlunghau.spring_boot_mini_project.module.user.dto.request.CreateUserRequest;
import com.maaitlunghau.spring_boot_mini_project.module.user.dto.request.UpdateProfileRequest;
import com.maaitlunghau.spring_boot_mini_project.module.user.dto.request.UpdateUserRoleRequest;
import com.maaitlunghau.spring_boot_mini_project.module.user.dto.response.UserResponse;
import com.maaitlunghau.spring_boot_mini_project.module.user.entity.Role;

public interface UserService {
    PageResponse<UserResponse> search(String keyword, Role role, Pageable pageable);
    UserResponse getById(Long id);
    UserResponse create(CreateUserRequest request);
    UserResponse updateProfile(Long id, UpdateProfileRequest request);
    UserResponse updateRole(Long id, UpdateUserRoleRequest request);
    void delete(Long id);
}
