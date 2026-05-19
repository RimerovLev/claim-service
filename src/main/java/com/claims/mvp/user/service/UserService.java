package com.claims.mvp.user.service;

import com.claims.mvp.user.dto.request.ChangeRoleRequest;
import com.claims.mvp.user.dto.request.CreateUserRequest;
import com.claims.mvp.user.dto.response.UserResponse;
import jakarta.validation.Valid;

import java.util.List;

public interface UserService {
    UserResponse createUser(@Valid CreateUserRequest request);
    List<UserResponse> getAllUsers();
    UserResponse changeRole(Long id, ChangeRoleRequest request);
}
