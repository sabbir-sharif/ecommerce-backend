package com.ecommerce.auth.service;

import com.ecommerce.auth.dto.RefreshTokenResult;
import com.ecommerce.auth.entity.RefreshToken;
import com.ecommerce.auth.repository.RefreshTokenRepository;
import com.ecommerce.common.exception.InvalidRefreshTokenException;
import com.ecommerce.user.entity.User;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;

    @Transactional
    public RefreshTokenResult create(User user) {

        String rawToken = generateRawToken();

        saveToken(user, rawToken);

        return new RefreshTokenResult(
                user,
                rawToken,
                refreshTokenExpiration / 1000
        );
    }

    @Transactional
    public RefreshTokenResult rotate(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRefreshTokenException(
                    "Refresh token is invalid"
            );
        }

        String tokenHash = hashToken(rawToken);

        RefreshToken existingToken =
                refreshTokenRepository
                        .findByTokenHashForUpdate(tokenHash)
                        .orElseThrow(() ->
                                new InvalidRefreshTokenException(
                                        "Refresh token is invalid"
                                )
                        );

        if (existingToken.isRevoked()) {
            throw new InvalidRefreshTokenException(
                    "Refresh token has already been used or revoked"
            );
        }

        if (existingToken.isExpired()) {
            throw new InvalidRefreshTokenException(
                    "Refresh token has expired"
            );
        }

        User user = existingToken.getUser();

        // Revoke the old refresh token.
        existingToken.setRevokedAt(LocalDateTime.now());

        refreshTokenRepository.save(existingToken);

        // Generate a replacement refresh token.
        String newRawToken = generateRawToken();

        saveToken(user, newRawToken);

        return new RefreshTokenResult(
                user,
                newRawToken,
                refreshTokenExpiration / 1000
        );
    }

    @Transactional
    public void revoke(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRefreshTokenException(
                    "Refresh token is invalid"
            );
        }

        String tokenHash = hashToken(rawToken);

        refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .ifPresent(token -> {

                    if (!token.isRevoked()) {
                        token.setRevokedAt(LocalDateTime.now());
                        refreshTokenRepository.save(token);
                    }
                });
    }

    private void saveToken(User user, String rawToken) {

        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashToken(rawToken));

        refreshToken.setExpiresAt(
                LocalDateTime.now()
                        .plusSeconds(refreshTokenExpiration / 1000)
        );

        refreshTokenRepository.save(refreshToken);
    }

    private String generateRawToken() {

        byte[] randomBytes = new byte[64];

        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    private String hashToken(String token) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (Exception ex) {

            throw new IllegalStateException(
                    "Unable to hash refresh token",
                    ex
            );
        }
    }
}