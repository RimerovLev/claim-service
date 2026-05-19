package com.claims.mvp.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class RegisterRequest {
    @NotBlank
    String fullName;
    @Email @NotBlank
    String email;
    @NotBlank @Size(min = 8)
    String password;
}
