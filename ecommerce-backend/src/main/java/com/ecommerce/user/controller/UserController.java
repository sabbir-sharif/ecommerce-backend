package com.ecommerce.user.controller;

import com.ecommerce.common.response.ApiResponse;
import com.ecommerce.user.dto.UserCreateRequest;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.dto.UserUpdateRequest;
import com.ecommerce.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            Authentication authentication) {

        String email = authentication.getName();

        UserResponse response =
                userService.getCurrentUser(email);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Current user retrieved successfully",
                        response
                )
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(
            @Valid @RequestBody UserCreateRequest request) {

        UserResponse response = userService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "User created successfully",
                                response
                        )
                );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getById(
            @PathVariable int id) {

        UserResponse response = userService.getById(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "User retrieved successfully",
                        response
                )
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAll() {

        List<UserResponse> response = userService.getAll();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Users retrieved successfully",
                        response
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable int id,
            @Valid @RequestBody UserUpdateRequest request) {

        UserResponse response =
                userService.update(id, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "User updated successfully",
                        response
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable int id) {

        userService.delete(id);

        return ResponseEntity.ok(
                ApiResponse.<Void>success(
                        "User deleted successfully",
                        null
                )
        );
    }
}

