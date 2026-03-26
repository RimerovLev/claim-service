package com.claims.mvp.controller;

import com.claims.mvp.dto.UserDto;
import com.claims.mvp.exception.GlobalExceptionHandler;
import com.claims.mvp.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest {

    private MockMvc mockMvc;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = request -> {
            if ("dupe@example.com".equals(request.getEmail())) {
                throw new IllegalArgumentException("User already exists");
            }
            UserDto dto = new UserDto();
            dto.setId(1L);
            dto.setFullName(request.getFullName());
            dto.setEmail(request.getEmail());
            return dto;
        };
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createUser_returnsUser() throws Exception {
        String body = """
                {
                  "fullName": "Ivan Petrov",
                  "email": "ivan@example.com"
                }
                """;

        mockMvc.perform(post("/api/claims/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("ivan@example.com"));
    }

    @Test
    void createUser_invalidEmail_returns400() throws Exception {
        String body = """
                {
                  "fullName": "Ivan Petrov",
                  "email": "not-an-email"
                }
                """;

        mockMvc.perform(post("/api/claims/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_duplicateEmail_returns409() throws Exception {
        String body = """
                {
                  "fullName": "Ivan Petrov",
                  "email": "dupe@example.com"
                }
                """;

        mockMvc.perform(post("/api/claims/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }
}
