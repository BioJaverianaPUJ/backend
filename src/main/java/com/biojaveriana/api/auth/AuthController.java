package com.biojaveriana.api.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.biojaveriana.api.auth.dto.LoginRequest;
import com.biojaveriana.api.auth.dto.LoginResponse;
import com.biojaveriana.api.auth.dto.RegistrationRequest;
import com.biojaveriana.api.common.dto.ErrorResponse;
import com.biojaveriana.api.security.CurrentUser;
import com.biojaveriana.api.user.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Public authentication endpoints: registration, login and token refresh.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Account registration, login and token refresh. " +
    "Successful calls return a JWT that must be sent as an HTTP Bearer token " +
    "(`Authorization: Bearer <token>`) on every protected endpoint.")
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "Log in",
        description = "Validates the email/password pair and returns a fresh JWT for the account. " +
            "Deactivated or banned accounts cannot log in.",
        security = {})
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Credentials valid; a fresh access token is returned"),
        @ApiResponse(responseCode = "400", description = "Validation failed (malformed email or invalid password length)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unknown email or wrong password",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var session = authService.login(request.email(), request.password());
        return ResponseEntity.ok(LoginResponse.from(session));
    }

    @Operation(summary = "Refresh access token",
        description = "Exchanges the current JWT for a fresh one. The token must be present and " +
            "not expired; this endpoint is authenticated even though it lives under /auth.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "A fresh access token is returned"),
        @ApiResponse(responseCode = "401", description = "Missing, expired or invalid token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PostMapping("/refresh-token")
    public ResponseEntity<LoginResponse> refreshToken(@Parameter(hidden = true) @CurrentUser User user) {
        var session = authService.refreshToken(user);
        return ResponseEntity.ok(LoginResponse.from(session));
    }

    @Operation(summary = "Register a new account",
        description = "Creates a new `USER` account and immediately returns its first access token. " +
            "The username and email must not be taken by another account.",
        security = {})
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Account created; an initial access token is returned"),
        @ApiResponse(responseCode = "400", description = "Validation failed (bad email, short password, ...)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Username already taken or email already linked",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegistrationRequest request) {
        var session = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(LoginResponse.from(session));
    }
}
