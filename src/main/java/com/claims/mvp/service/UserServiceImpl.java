package com.claims.mvp.service;

import com.claims.mvp.dao.UserRepository;
import com.claims.mvp.dto.UserDto;
import com.claims.mvp.model.User;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService{
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Override
    public UserDto createUser(UserDto request) {
        if(userRepository.existsByEmail(request.getEmail())){
            throw new IllegalArgumentException("User already exists");
        }
        User user = modelMapper.map(request, User.class);
        User saved = userRepository.save(user);
        return modelMapper.map(saved, UserDto.class);
    }
}
