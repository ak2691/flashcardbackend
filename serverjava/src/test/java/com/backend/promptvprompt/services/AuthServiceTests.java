package com.backend.promptvprompt.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.backend.promptvprompt.DTO.Auth.AuthResponse;
import com.backend.promptvprompt.DTO.Auth.LoginRequest;
import com.backend.promptvprompt.DTO.Auth.RegistrationRequest;
import com.backend.promptvprompt.exceptions.InvalidCredentialsException;
import com.backend.promptvprompt.exceptions.UserAlreadyExistsException;
import com.backend.promptvprompt.models.RefreshToken;
import com.backend.promptvprompt.models.User;
import com.backend.promptvprompt.models.UserProfile;
import com.backend.promptvprompt.repos.UserProfileRepo;
import com.backend.promptvprompt.repos.UserRepo;
import com.backend.promptvprompt.services.AuthService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

        @Mock
        private UserRepo userRepo;

        @Mock
        private UserProfileRepo userProfileRepo;

        @Mock
        private BCryptPasswordEncoder passwordEncoder;

        @Mock
        private JwtService jwtService;

        @Mock
        private RefreshTokenService refreshTokenService;

        @Mock
        private HttpServletResponse response;

        @Mock
        private HttpServletRequest request;

        @InjectMocks
        @Spy
        private AuthService authService;

        private User user;
        private UserProfile userProfile;
        private RefreshToken refreshToken;
        private RegistrationRequest registrationRequest;
        private LoginRequest loginRequest;

        @BeforeEach
        void setUp() {
                user = User.builder()
                                .id("user-123")
                                .email("test@example.com")
                                .passwordHash("hashedPassword")
                                .isEmailVerified(false)
                                .build();

                userProfile = UserProfile.builder()
                                .id("profile-123")
                                .user(user)
                                .displayName("testuser")
                                .gamesPlayed(0)
                                .wins(0)
                                .losses(0)
                                .draws(0)
                                .dailyGamesPlayed(0)
                                .build();

                user.setProfile(userProfile);

                refreshToken = RefreshToken.builder()
                                .id("token-123")
                                .token("refresh-token-value")
                                .user(user)
                                .expiresAt(LocalDateTime.now().plusDays(7))
                                .revoked(false)
                                .build();

                registrationRequest = new RegistrationRequest(
                                "test@example.com",
                                "testuser",
                                "password123");

                loginRequest = new LoginRequest("testuser", "password123");
        }

        // ========== REGISTER TESTS ==========

        @Test
        @DisplayName("Should create access token, refresh token, and set cookie during registration")
        void register_CreatesTokensAndSetsCookie() {
                // Arrange
                String accessToken = "generated-access-token";

                when(userRepo.existsByEmail(registrationRequest.getEmail())).thenReturn(false);
                when(userProfileRepo.existsByDisplayName(registrationRequest.getUsername())).thenReturn(false);
                when(passwordEncoder.encode(registrationRequest.getPassword())).thenReturn("hashedPassword");
                when(userRepo.save(any(User.class))).thenReturn(user);
                when(userProfileRepo.save(any(UserProfile.class))).thenReturn(userProfile);
                when(jwtService.generateAccessToken(user.getId(), user.getEmail())).thenReturn(accessToken);
                when(refreshTokenService.createRefreshToken(user.getId())).thenReturn(refreshToken);

                ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);

                // Act
                AuthResponse authResponse = authService.register(registrationRequest, response);

                // Assert
                assertNotNull(authResponse);
                assertEquals(accessToken, authResponse.getAccessToken());
                assertEquals("User registered successfully", authResponse.getMessage());

                // Verify JWT and refresh token were created
                verify(jwtService).generateAccessToken(user.getId(), user.getEmail());
                verify(refreshTokenService).createRefreshToken(user.getId());

                // Verify cookie was set with refresh token
                verify(response).addCookie(cookieCaptor.capture());
                Cookie capturedCookie = cookieCaptor.getValue();
                assertEquals("refreshToken", capturedCookie.getName());
                assertEquals(refreshToken.getToken(), capturedCookie.getValue());
                assertTrue(capturedCookie.isHttpOnly());
                assertEquals("/api/auth", capturedCookie.getPath());
        }

        // ========== LOGIN TESTS ==========

        @Test
        @DisplayName("Should create access token, refresh token, and set cookie during login")
        void login_CreatesTokensAndSetsCookie() {
                // Arrange
                String accessToken = "generated-access-token";

                when(userProfileRepo.findByDisplayName(loginRequest.getUsername()))
                                .thenReturn(Optional.of(userProfile));
                when(passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())).thenReturn(true);
                when(jwtService.generateAccessToken(user.getId(), user.getEmail())).thenReturn(accessToken);
                when(refreshTokenService.createRefreshToken(user.getId())).thenReturn(refreshToken);

                ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);

                // Act
                AuthResponse authResponse = authService.login(loginRequest, response);

                // Assert
                assertNotNull(authResponse);
                assertEquals(accessToken, authResponse.getAccessToken());
                assertEquals("Login successful", authResponse.getMessage());

                // Verify JWT and refresh token were created
                verify(jwtService).generateAccessToken(user.getId(), user.getEmail());
                verify(refreshTokenService).createRefreshToken(user.getId());

                // Verify cookie was set with refresh token
                verify(response).addCookie(cookieCaptor.capture());
                Cookie capturedCookie = cookieCaptor.getValue();
                assertEquals("refreshToken", capturedCookie.getName());
                assertEquals(refreshToken.getToken(), capturedCookie.getValue());
                assertTrue(capturedCookie.isHttpOnly());
                assertEquals("/api/auth", capturedCookie.getPath());
        }

        // ========== LOGOUT TESTS ==========

        @Test
        @DisplayName("Should revoke refresh token and clear cookie during logout when token exists")
        void logout_WithValidToken_RevokesTokenAndClearsCookie() {
                // Arrange
                String refreshTokenValue = "valid-refresh-token";
                Cookie refreshTokenCookie = new Cookie("refreshToken", refreshTokenValue);
                Cookie[] cookies = { refreshTokenCookie };

                when(request.getCookies()).thenReturn(cookies);
                ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);

                // Act
                authService.logout(request, response);

                // Assert
                verify(request, times(2)).getCookies();
                verify(refreshTokenService).revokeRefreshToken(refreshTokenValue);
                verify(response).addCookie(cookieCaptor.capture());

                // Verify cookie was cleared
                Cookie clearedCookie = cookieCaptor.getValue();
                assertEquals("refreshToken", clearedCookie.getName());
                assertNull(clearedCookie.getValue());
                assertEquals(0, clearedCookie.getMaxAge());
        }

        @Test
        @DisplayName("Should only clear cookie during logout when no refresh token present")
        void logout_NoToken_OnlyClearsCookie() {
                // Arrange
                when(request.getCookies()).thenReturn(null);
                ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);

                // Act
                authService.logout(request, response);

                // Assert
                verify(request).getCookies();
                verify(refreshTokenService, never()).revokeRefreshToken(any());
                verify(response).addCookie(cookieCaptor.capture());

                // Verify cookie was cleared
                Cookie clearedCookie = cookieCaptor.getValue();
                assertEquals("refreshToken", clearedCookie.getName());
                assertNull(clearedCookie.getValue());
                assertEquals(0, clearedCookie.getMaxAge());
        }

        @Test
        @DisplayName("Should only clear cookie when refresh token cookie not found in request")
        void logout_NoRefreshTokenCookie_OnlyClearsCookie() {
                // Arrange
                Cookie otherCookie = new Cookie("sessionId", "session-value");
                Cookie[] cookies = { otherCookie };

                when(request.getCookies()).thenReturn(cookies);
                ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);

                // Act
                authService.logout(request, response);

                // Assert
                verify(request, times(2)).getCookies();
                verify(refreshTokenService, never()).revokeRefreshToken(any());
                verify(response).addCookie(cookieCaptor.capture());

                // Verify cookie was cleared
                Cookie clearedCookie = cookieCaptor.getValue();
                assertEquals("refreshToken", clearedCookie.getName());
                assertNull(clearedCookie.getValue());
                assertEquals(0, clearedCookie.getMaxAge());
        }

        // ========== REFRESH ACCESS TOKEN TESTS ==========

        @Test
        @DisplayName("Should successfully refresh access token with valid refresh token from cookie")
        void refreshAccessToken_ValidToken_ReturnsNewAccessToken() {
                // Arrange
                String refreshTokenValue = "valid-refresh-token";
                String newAccessToken = "new-access-token";

                Cookie refreshTokenCookie = new Cookie("refreshToken", refreshTokenValue);
                Cookie[] cookies = { refreshTokenCookie };

                when(request.getCookies()).thenReturn(cookies);
                when(refreshTokenService.verifyRefreshToken(refreshTokenValue)).thenReturn(refreshToken);
                when(jwtService.generateAccessToken(user.getId(), user.getEmail())).thenReturn(newAccessToken);

                // Act
                AuthResponse authResponse = authService.refreshAccessToken(request, response);

                // Assert
                assertNotNull(authResponse);
                assertEquals(user.getId(), authResponse.getUserId());
                assertEquals(user.getEmail(), authResponse.getEmail());
                assertEquals(userProfile.getDisplayName(), authResponse.getUsername());
                assertEquals(newAccessToken, authResponse.getAccessToken());
                assertEquals("Token refreshed successfully", authResponse.getMessage());

                // Verify interactions
                verify(request, times(2)).getCookies();
                verify(refreshTokenService).verifyRefreshToken(refreshTokenValue);
                verify(jwtService).generateAccessToken(user.getId(), user.getEmail());
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException when refresh token not found in cookie")
        void refreshAccessToken_NoTokenInCookie_ThrowsException() {
                // Arrange
                when(request.getCookies()).thenReturn(null);

                // Act & Assert
                InvalidCredentialsException exception = assertThrows(
                                InvalidCredentialsException.class,
                                () -> authService.refreshAccessToken(request, response));

                assertEquals("Refresh token not found", exception.getMessage());

                // Verify interactions
                verify(request).getCookies();
                verify(refreshTokenService, never()).verifyRefreshToken(any());
                verify(jwtService, never()).generateAccessToken(any(), any());
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException when cookies array is empty")
        void refreshAccessToken_EmptyCookies_ThrowsException() {
                // Arrange
                Cookie[] cookies = {};
                when(request.getCookies()).thenReturn(cookies);

                // Act & Assert
                InvalidCredentialsException exception = assertThrows(
                                InvalidCredentialsException.class,
                                () -> authService.refreshAccessToken(request, response));

                assertEquals("Refresh token not found", exception.getMessage());

                // Verify interactions
                verify(request, times(2)).getCookies();
                verify(refreshTokenService, never()).verifyRefreshToken(any());
                verify(jwtService, never()).generateAccessToken(any(), any());
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException when refresh token cookie exists but is not named refreshToken")
        void refreshAccessToken_WrongCookieName_ThrowsException() {
                // Arrange
                Cookie wrongCookie = new Cookie("wrongName", "some-token");
                Cookie[] cookies = { wrongCookie };

                when(request.getCookies()).thenReturn(cookies);

                // Act & Assert
                InvalidCredentialsException exception = assertThrows(
                                InvalidCredentialsException.class,
                                () -> authService.refreshAccessToken(request, response));

                assertEquals("Refresh token not found", exception.getMessage());

                // Verify interactions
                verify(request, times(2)).getCookies();
                verify(refreshTokenService, never()).verifyRefreshToken(any());
                verify(jwtService, never()).generateAccessToken(any(), any());
        }

        @Test
        @DisplayName("Should propagate exception when refresh token verification fails")
        void refreshAccessToken_InvalidToken_ThrowsException() {
                // Arrange
                String refreshTokenValue = "invalid-refresh-token";

                Cookie refreshTokenCookie = new Cookie("refreshToken", refreshTokenValue);
                Cookie[] cookies = { refreshTokenCookie };

                when(request.getCookies()).thenReturn(cookies);
                when(refreshTokenService.verifyRefreshToken(refreshTokenValue))
                                .thenThrow(new InvalidCredentialsException("Invalid or expired refresh token"));

                // Act & Assert
                InvalidCredentialsException exception = assertThrows(
                                InvalidCredentialsException.class,
                                () -> authService.refreshAccessToken(request, response));

                assertEquals("Invalid or expired refresh token", exception.getMessage());

                // Verify interactions
                verify(request, times(2)).getCookies();
                verify(refreshTokenService).verifyRefreshToken(refreshTokenValue);
                verify(jwtService, never()).generateAccessToken(any(), any());
        }

        @Test
        @DisplayName("Should extract correct refresh token when multiple cookies present")
        void refreshAccessToken_MultipleCookies_ExtractsCorrectToken() {
                // Arrange
                String refreshTokenValue = "valid-refresh-token";
                String newAccessToken = "new-access-token";

                Cookie sessionCookie = new Cookie("sessionId", "session-123");
                Cookie refreshTokenCookie = new Cookie("refreshToken", refreshTokenValue);
                Cookie prefCookie = new Cookie("preferences", "dark-mode");
                Cookie[] cookies = { sessionCookie, refreshTokenCookie, prefCookie };

                when(request.getCookies()).thenReturn(cookies);
                when(refreshTokenService.verifyRefreshToken(refreshTokenValue)).thenReturn(refreshToken);
                when(jwtService.generateAccessToken(user.getId(), user.getEmail())).thenReturn(newAccessToken);

                // Act
                AuthResponse authResponse = authService.refreshAccessToken(request, response);

                // Assert
                assertNotNull(authResponse);
                assertEquals(newAccessToken, authResponse.getAccessToken());

                // Verify correct token was extracted and verified
                verify(request, times(2)).getCookies();
                verify(refreshTokenService).verifyRefreshToken(refreshTokenValue);
                verify(jwtService).generateAccessToken(user.getId(), user.getEmail());
        }
};