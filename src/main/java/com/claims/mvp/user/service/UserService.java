package com.claims.mvp.user.service;

import com.claims.mvp.user.dto.request.CreateUserRequest;
import com.claims.mvp.user.dto.response.UserResponse;
import jakarta.validation.Valid;

public interface UserService {
    UserResponse createUser(@Valid CreateUserRequest request);
}
