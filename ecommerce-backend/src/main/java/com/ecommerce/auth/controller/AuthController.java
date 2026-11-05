package com.ecommerce.auth.controller;

import com.ecommerce.auth.dto.LoginRequest;
import com.ecommerce.auth.dto.LoginResponse;
import com.ecommerce.auth.dto.RegisterRequest;
import com.ecommerce.auth.dto.RefreshTokenRequest;
import com.ecommerce.auth.dto.LogoutRequest;

import com.ecommerce.auth.service.AuthService;

import com.ecommerce.common.response.ApiResponse;

import com.ecommerce.user.dto.UserResponse;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


    // REGISTER
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        UserResponse response = authService.register(request);

        ApiResponse<UserResponse> apiResponse =
                ApiResponse.success(
                        "Registration successful",
                        response
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(apiResponse);
    }


    //LOGIN
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response = authService.login(request);

        ApiResponse<LoginResponse> apiResponse =
                ApiResponse.success(
                        "Login successful",
                        response
                );

        return ResponseEntity.ok(apiResponse);
    }


    //REFRESH
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {

        LoginResponse response = authService.refresh(request);

        ApiResponse<LoginResponse> apiResponse =
                ApiResponse.success(
                        "Token refreshed successfully",
                        response
                );

        return ResponseEntity.ok(apiResponse);
    }


    // LOGOUT
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody LogoutRequest request) {

        authService.logout(request.getRefreshToken());

        ApiResponse<Void> apiResponse =
                ApiResponse.success(
                        "Logged out successfully",
                        null
                );

        return ResponseEntity.ok(apiResponse);
    }
}