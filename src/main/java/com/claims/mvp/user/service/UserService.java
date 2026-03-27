package com.claims.mvp.user.service;

import com.claims.mvp.user.dto.UserDto;
import jakarta.validation.Valid;

public interface UserService {
    UserDto createUser(@Valid UserDto request);
}
