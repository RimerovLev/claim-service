package com.claims.mvp.user.service;

import com.claims.mvp.user.dao.UserRepository;
import com.claims.mvp.user.dto.request.CreateUserRequest;
import com.claims.mvp.user.dto.response.UserResponse;
import com.claims.mvp.user.mapper.UserMapper;
import com.claims.mvp.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
/**
 * UserService.
 *
 * Service for user operations (MVP scope):
 * - create user
 *
 * Contains basic business checks (e.g., email uniqueness).
 */
public class UserServiceImpl implements UserService{
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        // Create a user. For MVP we reject duplicates by email.
        if(userRepository.existsByEmail(request.getEmail())){
            throw new IllegalArgumentException("User already exists");
        }
        User user = userMapper.toEntity(request);
        User saved = userRepository.save(user);
        return userMapper.toResponse(saved);
    }
}
