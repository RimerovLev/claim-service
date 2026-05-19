package com.claims.mvp.security.service;

import com.claims.mvp.security.dto.AuthResponse;
import com.claims.mvp.security.dto.LoginRequest;
import com.claims.mvp.security.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);

}
