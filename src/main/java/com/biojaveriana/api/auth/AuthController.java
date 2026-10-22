package com.biojaveriana.api.auth;

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

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        var session = authService.login(request.email(), request.password());
        return LoginResponse.from(session);
    }

    @PostMapping("/refresh-token")
    public LoginResponse refreshToken(@CurrentUser User user) {
        var session = authService.refreshToken(user);
        return LoginResponse.from(session);
    }

    @PostMapping("/register")
    public LoginResponse register(@Valid @RequestBody RegistrationRequest request) {
        var session = authService.register(request);
        return LoginResponse.from(session);
    }
}
