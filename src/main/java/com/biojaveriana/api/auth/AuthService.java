package com.biojaveriana.api.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.biojaveriana.api.auth.dto.RegistrationRequest;
import com.biojaveriana.api.auth.exceptions.InvalidCredentialsException;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.UserRepository;
import com.biojaveriana.api.user.exceptions.EmailAlreadyLinkedException;
import com.biojaveriana.api.user.exceptions.UsernameAlreadyExistsException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public Session login(String email, String password) {
        var user = userRepository
            .findByEmail(email)
            .orElseThrow(() -> new InvalidCredentialsException());

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        var token = jwtService.generateToken(user);
        return new Session(user, token);
    }

    public Session refreshToken(User user) {
        var token = jwtService.generateToken(user);
        return new Session(user, token);
    }

    public Session register(RegistrationRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException(request.username());
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyLinkedException(request.email());
        }

        var passwordHash = passwordEncoder.encode(request.password());
        var user = User.builder()
            .firstName(request.firstName())
            .lastName(request.lastName())
            .username(request.username())
            .email(request.email())
            .passwordHash(passwordHash)
            .gender(request.gender())
            .role(Role.USER)
            .build();

        userRepository.save(user);

        var token = jwtService.generateToken(user);
        return new Session(user, token);
    }
}
