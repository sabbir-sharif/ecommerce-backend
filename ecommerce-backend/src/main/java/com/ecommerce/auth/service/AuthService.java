package com.ecommerce.auth.service;

import com.ecommerce.auth.dto.LoginRequest;
import com.ecommerce.auth.dto.LoginResponse;
import com.ecommerce.auth.dto.RegisterRequest;
import com.ecommerce.auth.dto.RefreshTokenRequest;
import com.ecommerce.auth.dto.RefreshTokenResult;

import com.ecommerce.auth.security.CustomUserDetailsService;
import com.ecommerce.auth.security.JwtService;

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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
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

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final RefreshTokenService refreshTokenService;

    private final CustomUserDetailsService userDetailsService;

    @Value("${jwt.expiration}")
    private long jwtExpiration;


    // REGISTER
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

        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        user.setRole(customerRole);

        user.setStatus(UserStatus.ACTIVE);

        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }


    //LOGIN
    @Transactional
    public LoginResponse login(LoginRequest request) {

        String normalizedEmail =
                request.getEmail().trim().toLowerCase(Locale.ROOT);

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                normalizedEmail,
                                request.getPassword()
                        )
                );

        UserDetails userDetails =
                (UserDetails) authentication.getPrincipal();

        String accessToken =
                jwtService.generateToken(userDetails);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );

        RefreshTokenResult refreshResult =
                refreshTokenService.create(user);

        return LoginResponse.builder()

                .accessToken(accessToken)

                .tokenType("Bearer")

                .expiresIn(jwtExpiration / 1000)

                .refreshToken(refreshResult.getRefreshToken())

                .refreshTokenExpiresIn(refreshResult.getExpiresIn())

                .user(userMapper.toResponse(user))

                .build();
    }


    //REFRESH TOKEN
    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request) {

        RefreshTokenResult refreshResult =
                refreshTokenService.rotate(request.getRefreshToken());

        User user = refreshResult.getUser();

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(user.getEmail());

        String newAccessToken =
                jwtService.generateToken(userDetails);

        return LoginResponse.builder()

                .accessToken(newAccessToken)

                .tokenType("Bearer")

                .expiresIn(jwtExpiration / 1000)

                .refreshToken(refreshResult.getRefreshToken())

                .refreshTokenExpiresIn(refreshResult.getExpiresIn())

                .user(userMapper.toResponse(user))

                .build();
    }


    // LOGOUT
    @Transactional
    public void logout(String refreshToken) {

        refreshTokenService.revoke(refreshToken);
    }
}