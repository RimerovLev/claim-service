package com.claims.mvp.user.service;

import com.claims.mvp.user.dao.UserRepository;
import com.claims.mvp.user.dto.UserDto;
import com.claims.mvp.user.model.User;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
/**
 * UserService.
 *
 * Сервис для операций с пользователями (MVP-уровень):
 * - создание пользователя
 *
 * Здесь же живут базовые бизнес-проверки (например, уникальность email).
 */
public class UserServiceImpl implements UserService{
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Override
    public UserDto createUser(UserDto request) {
        // Создание пользователя. Для MVP запрещаем дубликаты по email.
        if(userRepository.existsByEmail(request.getEmail())){
            throw new IllegalArgumentException("User already exists");
        }
        User user = modelMapper.map(request, User.class);
        User saved = userRepository.save(user);
        return modelMapper.map(saved, UserDto.class);
    }
}
