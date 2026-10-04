package com.requirements.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.requirements.backend.dto.BusinessAnalystResponse;
import com.requirements.backend.repository.UserRepository;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/business-analysts")
    public List<BusinessAnalystResponse> getBusinessAnalysts() {

        return userRepository
                .findByRole("BUSINESS_ANALYST")
                .stream()
                .map(user ->
                        new BusinessAnalystResponse(
                                user.getId(),
                                user.getName(),
                                user.getEmail()
                        )
                )
                .toList();
    }
}