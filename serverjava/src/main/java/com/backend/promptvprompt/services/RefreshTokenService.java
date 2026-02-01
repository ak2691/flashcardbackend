package com.backend.promptvprompt.services;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend.promptvprompt.exceptions.InvalidCredentialsException;
import com.backend.promptvprompt.models.RefreshToken;
import com.backend.promptvprompt.models.User;
import com.backend.promptvprompt.repos.RefreshTokenRepo;
import com.backend.promptvprompt.repos.UserRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepo refreshTokenRepo;
    private final UserRepo userRepo;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration; // 7 days in milliseconds

    @Transactional
    public RefreshToken createRefreshToken(String userId) {
        // Optional: Delete old refresh tokens for this user
        refreshTokenRepo.deleteByUserId(userId);

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .revoked(false)
                .build();

        return refreshTokenRepo.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public RefreshToken verifyRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepo.findByToken(token)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

        if (refreshToken.getRevoked()) {
            throw new InvalidCredentialsException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidCredentialsException("Refresh token has expired");
        }

        return refreshToken;
    }

    @Transactional
    public void revokeRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepo.findByToken(token)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

        refreshToken.setRevoked(true);
        refreshTokenRepo.save(refreshToken);
    }

    @Transactional
    public void revokeAllUserTokens(String userId) {
        refreshTokenRepo.deleteByUserId(userId);
    }

    @Scheduled(cron = "0 0 2 * * ?") // Run daily at 2 AM
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepo.deleteExpiredTokens(LocalDateTime.now());
    }
}
