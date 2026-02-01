package com.backend.promptvprompt.services;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend.promptvprompt.DTO.Auth.AuthResponse;
import com.backend.promptvprompt.DTO.Auth.LoginRequest;
import com.backend.promptvprompt.DTO.Auth.RegistrationRequest;
import com.backend.promptvprompt.controllers.GameSocketController;
import com.backend.promptvprompt.exceptions.InvalidCredentialsException;
import com.backend.promptvprompt.exceptions.UserAlreadyExistsException;
import com.backend.promptvprompt.models.RefreshToken;
import com.backend.promptvprompt.models.User;
import com.backend.promptvprompt.models.UserProfile;
import com.backend.promptvprompt.repos.UserProfileRepo;
import com.backend.promptvprompt.repos.UserRepo;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepo userRepo;
    private final UserProfileRepo userProfileRepo;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Transactional
    public AuthResponse register(RegistrationRequest request, HttpServletResponse response) {

        if (userRepo.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email is already registered");
        }

        if (userProfileRepo.existsByDisplayName(request.getUsername())) {
            throw new UserAlreadyExistsException("Username is already taken");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .isEmailVerified(false)
                .build();

        User savedUser = userRepo.save(user);

        UserProfile userProfile = UserProfile.builder()
                .user(savedUser)
                .displayName(request.getUsername())
                .gamesPlayed(0)
                .wins(0)
                .losses(0)
                .draws(0)
                .dailyGamesPlayed(0)
                .build();

        userProfileRepo.save(userProfile);
        String accessToken = jwtService.generateAccessToken(savedUser.getId(), savedUser.getEmail());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser.getId());

        addRefreshTokenCookie(response, refreshToken.getToken());

        return AuthResponse.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .username(userProfile.getDisplayName())
                .accessToken(accessToken)
                .message("User registered successfully")
                .build();

    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        // Find user profile by username
        UserProfile userProfile = userProfileRepo.findByDisplayName(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        // Get the associated user
        User user = userProfile.getUser();

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());
        addRefreshTokenCookie(response, refreshToken.getToken());

        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .username(userProfile.getDisplayName())
                .accessToken(accessToken)
                .message("Login successful")
                .build();
    }

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {

        String refreshTokenValue = extractRefreshTokenFromCookie(request);
        if (refreshTokenValue != null) {
            refreshTokenService.revokeRefreshToken(refreshTokenValue);
        }
        clearRefreshTokenCookie(response);
    }

    public AuthResponse refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshTokenValue = extractRefreshTokenFromCookie(request);

        if (refreshTokenValue == null) {
            throw new InvalidCredentialsException("Refresh token not found");
        }

        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(refreshTokenValue);
        User user = refreshToken.getUser();

        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getProfile().getDisplayName())
                .accessToken(newAccessToken)
                .message("Token refreshed successfully")
                .build();

    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/api/auth");
        cookie.setMaxAge((int) refreshTokenExpiration / 1000);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> "refreshToken".equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Strict");

        response.addCookie(cookie);
    }
}
