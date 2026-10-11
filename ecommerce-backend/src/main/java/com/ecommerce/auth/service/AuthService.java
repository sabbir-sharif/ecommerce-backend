package com.ecommerce.auth.service;

import com.ecommerce.auth.dto.RegisterRequest;
import com.ecommerce.auth.dto.RegisterResponse;
import com.ecommerce.common.exception.ConflictException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.role.entity.Role;
import com.ecommerce.role.repository.RoleRepository;

import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.entity.UserStatus;
import com.ecommerce.user.mapper.UserMapper;
import com.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;


    @Transactional
    public UserResponse register(RegisterRequest request) {

        String normalizedEmail =
                request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException(
                    "An account with this email already exists"
            );
        }

        Role customerRole = roleRepository.findByName("USER")
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "USER role is not configured"
                        )
                );

        User user = new User();

        user.setName(request.getName().trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(customerRole);

        // Set the default status according to your existing User entity.
        // Example, if status is a String:
         user.setStatus(UserStatus.ACTIVE);
        //
        // If status is an enum, use the appropriate enum constant.

        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }
}

