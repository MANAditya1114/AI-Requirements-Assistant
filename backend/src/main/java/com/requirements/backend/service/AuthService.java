package com.requirements.backend.service;

import java.security.SecureRandom;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.requirements.backend.dto.AuthResponse;
import com.requirements.backend.dto.LoginRequest;
import com.requirements.backend.dto.RegisterRequest;
import com.requirements.backend.model.User;
import com.requirements.backend.repository.UserRepository;

@Service
public class AuthService {

    private static final String CODE_CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private static final int CODE_LENGTH = 6;

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.secureRandom = new SecureRandom();
    }

    public AuthResponse register(RegisterRequest request) {

        if (request.getName() == null ||
                request.getName().trim().isEmpty()) {

            throw new IllegalStateException(
                    "Name is required.");
        }

        if (request.getEmail() == null ||
                request.getEmail().trim().isEmpty()) {

            throw new IllegalStateException(
                    "Email is required.");
        }

        if (request.getPassword() == null ||
                request.getPassword().length() < 6) {

            throw new IllegalStateException(
                    "Password must contain at least 6 characters.");
        }

        String role = normalizeRole(request.getRole());

        String email =
                request.getEmail()
                        .trim()
                        .toLowerCase();

        if (userRepository.existsByEmail(email)) {

            throw new IllegalStateException(
                    "An account with this email already exists.");
        }

        String passwordHash =
                passwordEncoder.encode(
                        request.getPassword());

        User user = new User(
                request.getName().trim(),
                email,
                passwordHash,
                role);

        // =====================================================
        // Stakeholder registration
        // =====================================================

        if ("STAKEHOLDER".equals(role)) {

            String assignmentCode =
                    request.getAssignmentCode();

            if (assignmentCode == null ||
                    assignmentCode.trim().isEmpty()) {

                throw new IllegalStateException(
                        "Business Analyst assignment code is required.");
            }

            String normalizedCode =
                    assignmentCode
                            .trim()
                            .toUpperCase();

            User businessAnalyst =
                    userRepository
                            .findByAssignmentCode(normalizedCode)
                            .orElseThrow(() ->
                                    new IllegalStateException(
                                            "Invalid Business Analyst assignment code."));

            if (!"BUSINESS_ANALYST".equals(
                    businessAnalyst.getRole())) {

                throw new IllegalStateException(
                        "The assignment code does not belong to a Business Analyst.");
            }

            user.setAssignedBusinessAnalystId(
                    businessAnalyst.getId());

            user.setAssignmentCode(null);
        }

        // =====================================================
        // Business Analyst registration
        // =====================================================

        else if ("BUSINESS_ANALYST".equals(role)) {

            user.setAssignedBusinessAnalystId(null);

            String assignmentCode =
                    generateUniqueAssignmentCode();

            user.setAssignmentCode(assignmentCode);
        }

        User savedUser =
                userRepository.save(user);

        return buildResponse(
                savedUser,
                "Registration successful.");
    }

    public AuthResponse login(LoginRequest request) {

        if (request.getEmail() == null ||
                request.getEmail().trim().isEmpty()) {

            throw new IllegalStateException(
                    "Email is required.");
        }

        if (request.getPassword() == null ||
                request.getPassword().isEmpty()) {

            throw new IllegalStateException(
                    "Password is required.");
        }

        String email =
                request.getEmail()
                        .trim()
                        .toLowerCase();

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Invalid email or password."));

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash())) {

            throw new IllegalStateException(
                    "Invalid email or password.");
        }

        return buildResponse(
                user,
                "Login successful.");
    }

    // =====================================================
    // Generate unique Business Analyst assignment code
    // Example: BA-X7K29P
    // =====================================================

    private String generateUniqueAssignmentCode() {

        String assignmentCode;

        do {

            StringBuilder code =
                    new StringBuilder("BA-");

            for (int i = 0; i < CODE_LENGTH; i++) {

                int index =
                        secureRandom.nextInt(
                                CODE_CHARACTERS.length());

                code.append(
                        CODE_CHARACTERS.charAt(index));
            }

            assignmentCode = code.toString();

        } while (userRepository
                .findByAssignmentCode(assignmentCode)
                .isPresent());

        return assignmentCode;
    }

    private String normalizeRole(String role) {

        if (role == null ||
                role.trim().isEmpty()) {

            throw new IllegalStateException(
                    "Role is required.");
        }

        String normalizedRole =
                role.trim()
                        .toUpperCase()
                        .replace(" ", "_");

        if (!normalizedRole.equals("STAKEHOLDER") &&
                !normalizedRole.equals("BUSINESS_ANALYST")) {

            throw new IllegalStateException(
                    "Role must be STAKEHOLDER or BUSINESS_ANALYST.");
        }

        return normalizedRole;
    }

    private AuthResponse buildResponse(
            User user,
            String message) {

        return new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getAssignedBusinessAnalystId(),
                user.getAssignmentCode(),
                message);
    }
}