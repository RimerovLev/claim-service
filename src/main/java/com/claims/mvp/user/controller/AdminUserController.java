package com.claims.mvp.user.controller;

import com.claims.mvp.user.dto.request.ChangeRoleRequest;
import com.claims.mvp.user.dto.response.UserResponse;
import com.claims.mvp.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @PatchMapping("/{id}/role")
    public UserResponse updateUserRole(@PathVariable Long id, @Valid @RequestBody ChangeRoleRequest newRole) {
        return userService.changeRole(id, newRole);
    }
}
