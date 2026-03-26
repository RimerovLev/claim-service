package com.claims.mvp.service;

import com.claims.mvp.dto.UserDto;
import jakarta.validation.Valid;

public interface UserService {
    UserDto createUser(@Valid UserDto request);
}
