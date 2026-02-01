package com.backend.promptvprompt.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import com.backend.promptvprompt.DTO.Auth.AuthResponse;
import com.backend.promptvprompt.DTO.Auth.LoginRequest;
import com.backend.promptvprompt.DTO.Auth.RegistrationRequest;
import com.backend.promptvprompt.config.CorsConfig;
import com.backend.promptvprompt.config.JwtAuthenticationFilter;
import com.backend.promptvprompt.config.SecurityConfig;
import com.backend.promptvprompt.exceptions.GlobalExceptionHandler;
import com.backend.promptvprompt.exceptions.InvalidCredentialsException;
import com.backend.promptvprompt.exceptions.UserAlreadyExistsException;
import com.backend.promptvprompt.services.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = AuthController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController Tests")
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private AuthService authService;

        @Test
        @DisplayName("Should successfully register a user with valid request")
        void register_ValidRequest_ReturnsCreated() throws Exception {

                RegistrationRequest request = new RegistrationRequest(
                                "test@example.com",
                                "testuser",
                                "password123");

                AuthResponse response = AuthResponse.builder()
                                .userId("user-123")
                                .email("test@example.com")
                                .username("testuser")
                                .message("User registered successfully")
                                .build();

                when(authService.register(any(RegistrationRequest.class), any(HttpServletResponse.class)))
                                .thenReturn(response);

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andDo(MockMvcResultHandlers.print())
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.userId").value("user-123"))
                                .andExpect(jsonPath("$.email").value("test@example.com"))
                                .andExpect(jsonPath("$.username").value("testuser"))
                                .andExpect(jsonPath("$.message").value("User registered successfully"));

                verify(authService).register(any(RegistrationRequest.class), any(HttpServletResponse.class));
        }

        @Test
        @DisplayName("Should return 400 when registration request has missing email")
        void register_MissingEmail_ReturnsBadRequest() throws Exception {

                RegistrationRequest request = new RegistrationRequest(
                                "",
                                "testuser",
                                "password123");

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());

                verify(authService, never()).register(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when registration request has invalid email format")
        void register_InvalidEmail_ReturnsBadRequest() throws Exception {

                RegistrationRequest request = new RegistrationRequest(
                                "invalid-email",
                                "testuser",
                                "password123");

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());

                verify(authService, never()).register(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when registration request has missing username")
        void register_MissingUsername_ReturnsBadRequest() throws Exception {

                RegistrationRequest request = new RegistrationRequest(
                                "test@example.com",
                                "",
                                "password123");

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());

                verify(authService, never()).register(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when registration request has username less than 5 characters")
        void register_ShortUsername_ReturnsBadRequest() throws Exception {

                RegistrationRequest request = new RegistrationRequest(
                                "test@example.com",
                                "test",
                                "password123");

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());

                verify(authService, never()).register(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when registration request has missing password")
        void register_MissingPassword_ReturnsBadRequest() throws Exception {

                RegistrationRequest request = new RegistrationRequest(
                                "test@example.com",
                                "testuser",
                                "");

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());

                verify(authService, never()).register(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when registration request has password less than 5 characters")
        void register_ShortPassword_ReturnsBadRequest() throws Exception {

                RegistrationRequest request = new RegistrationRequest(
                                "test@example.com",
                                "testuser",
                                "pass");

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());

                verify(authService, never()).register(any(), any());
        }

        @Test
        @DisplayName("Should handle UserAlreadyExistsException when email is already registered")
        void register_EmailAlreadyExists_ReturnsError() throws Exception {

                RegistrationRequest request = new RegistrationRequest(
                                "test@example.com",
                                "testuser",
                                "password123");

                when(authService.register(any(RegistrationRequest.class), any(HttpServletResponse.class)))
                                .thenThrow(new UserAlreadyExistsException("Email is already registered"));

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().is4xxClientError());

                verify(authService).register(any(RegistrationRequest.class), any(HttpServletResponse.class));
        }

        @Test
        @DisplayName("Should handle UserAlreadyExistsException when username is already taken")
        void register_UsernameAlreadyExists_ReturnsError() throws Exception {

                RegistrationRequest request = new RegistrationRequest(
                                "test@example.com",
                                "testuser",
                                "password123");

                when(authService.register(any(RegistrationRequest.class), any(HttpServletResponse.class)))
                                .thenThrow(new UserAlreadyExistsException("Username is already taken"));

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().is4xxClientError());

                verify(authService).register(any(RegistrationRequest.class), any(HttpServletResponse.class));
        }

        @Test
        @DisplayName("Should successfully login with valid credentials")
        void login_ValidRequest_ReturnsOk() throws Exception {

                LoginRequest request = new LoginRequest(
                                "testuser",
                                "password123");

                AuthResponse response = AuthResponse.builder()
                                .userId("user-123")
                                .email("test@example.com")
                                .username("testuser")
                                .message("Login successful")
                                .build();

                when(authService.login(any(LoginRequest.class), any(HttpServletResponse.class))).thenReturn(response);

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.userId").value("user-123"))
                                .andExpect(jsonPath("$.email").value("test@example.com"))
                                .andExpect(jsonPath("$.username").value("testuser"))
                                .andExpect(jsonPath("$.message").value("Login successful"));

                verify(authService).login(any(LoginRequest.class), any(HttpServletResponse.class));
        }

        @Test
        @DisplayName("Should return 400 when login request has missing username")
        void login_MissingUsername_ReturnsBadRequest() throws Exception {

                LoginRequest request = new LoginRequest(
                                "",
                                "password123");

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());

                verify(authService, never()).login(any(), any());
        }

        @Test
        @DisplayName("Should return 400 when login request has missing password")
        void login_MissingPassword_ReturnsBadRequest() throws Exception {

                LoginRequest request = new LoginRequest(
                                "testuser",
                                "");

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());

                verify(authService, never()).login(any(), any());
        }

        @Test
        @DisplayName("Should handle InvalidCredentialsException when credentials are wrong")
        void login_InvalidCredentials_ReturnsError() throws Exception {

                LoginRequest request = new LoginRequest(
                                "testuser",
                                "wrongpassword");

                when(authService.login(any(LoginRequest.class),
                                any(HttpServletResponse.class)))
                                .thenThrow(new InvalidCredentialsException("Invalid username or password"));

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().is4xxClientError());

                verify(authService).login(any(LoginRequest.class), any(HttpServletResponse.class));
        }
}