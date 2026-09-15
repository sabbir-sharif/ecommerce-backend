package com.ecommerce.user.dto;

import com.ecommerce.user.entity.UserStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class UserResponse {

    private int id;

    private String name;

    private String email;

    private String role;

    private UserStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
