package com.ecommerce.auth.dto;

import com.ecommerce.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RefreshTokenResult {

    private User user;
    private String refreshToken;
    private long expiresIn;
}