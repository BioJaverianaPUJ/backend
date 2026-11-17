package com.biojaveriana.api.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.biojaveriana.api.auth.dto.LoginRequest;
import com.biojaveriana.api.auth.dto.LoginResponse;
import com.biojaveriana.api.auth.dto.RegistrationRequest;
import com.biojaveriana.api.security.CurrentUser;
import com.biojaveriana.api.user.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user authentication including registration, login, and token management")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates a user with email and password, returning a JWT token and session information")
    @ApiResponse(responseCode = "200", description = "Login successful, returns JWT token")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var session = authService.login(request.email(), request.password());
        return ResponseEntity.ok(LoginResponse.from(session));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh authentication token", description = "Refreshes the user's JWT token using the current session")
    @ApiResponse(responseCode = "200", description = "Token refreshed successfully")
    public ResponseEntity<LoginResponse> refreshToken(@CurrentUser User user) {
        var session = authService.refreshToken(user);
        return ResponseEntity.ok(LoginResponse.from(session));
    }

    @PostMapping("/register")
    @Operation(summary = "User registration", description = "Registers a new user account and returns authentication credentials")
    @ApiResponse(responseCode = "201", description = "User registered successfully")
    @ApiResponse(responseCode = "400", description = "Invalid registration data or email already in use")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegistrationRequest request) {
        var session = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(LoginResponse.from(session));
    }
}
