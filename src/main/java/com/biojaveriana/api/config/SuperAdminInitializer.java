package com.biojaveriana.api.config;

import java.security.SecureRandom;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Initializes the undeletable superadmin account on application startup.
 * The superadmin has a fixed username ("superadmin") and a randomly
 * generated password that is logged to the console on first startup.
 */
@Component
@RequiredArgsConstructor
public class SuperAdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminInitializer.class);

    private static final String SUPERADMIN_USERNAME = "superadmin";
    private static final String SUPERADMIN_EMAIL_PREFIX = "superadmin-";
    private static final int PASSWORD_LENGTH = 16;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        userRepository.findByRole(Role.SUPERADMIN).ifPresentOrElse(
            existing -> log.info("Superadmin account already exists with username: {}", existing.getUsername()),
            () -> createSuperadmin()
        );
    }

    private void createSuperadmin() {
        String randomPassword = generateRandomPassword();
        String email = SUPERADMIN_EMAIL_PREFIX + generateRandomSuffix(8) + "@superadmin.local";

        User superadmin = User.builder()
            .username(SUPERADMIN_USERNAME)
            .email(email)
            .passwordHash(passwordEncoder.encode(randomPassword))
            .firstName("System")
            .lastName("Superadmin")
            .gender(com.biojaveriana.api.user.Gender.OTHER)
            .role(Role.SUPERADMIN)
            .build();

        userRepository.save(superadmin);

        log.warn("=================================================================");
        log.warn("SUPERADMIN ACCOUNT CREATED");
        log.warn("  Username : {}", SUPERADMIN_USERNAME);
        log.warn("  Email    : {}", email);
        log.warn("  Password : {}", randomPassword);
        log.warn("=================================================================");
        log.warn("  !!! CHANGE THIS PASSWORD IMMEDIATELY !!!");
        log.warn("=================================================================");
    }

    private String generateRandomPassword() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] bytes = new byte[PASSWORD_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateRandomSuffix(int length) {
        SecureRandom secureRandom = new SecureRandom();
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
