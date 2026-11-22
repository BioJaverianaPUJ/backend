package com.biojaveriana.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.biojaveriana.api.user.Gender;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class SuperAdminInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private SuperAdminInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new SuperAdminInitializer(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("run: an existing superadmin account is left untouched")
    void existingSuperadminIsNotRecreated() {
        User existing = User.builder().username("superadmin").role(Role.SUPERADMIN).build();
        when(userRepository.findByRole(Role.SUPERADMIN)).thenReturn(Optional.of(existing));

        initializer.run();

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("run: a missing superadmin is created with an encoded random password")
    void missingSuperadminIsCreated() {
        when(userRepository.findByRole(Role.SUPERADMIN)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-hash");

        initializer.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User created = captor.getValue();
        assertEquals("superadmin", created.getUsername());
        assertEquals(Role.SUPERADMIN, created.getRole());
        assertEquals("encoded-hash", created.getPasswordHash());
        assertEquals(Gender.OTHER, created.getGender());
        assertTrue(created.getEmail().startsWith("superadmin-"));
        assertTrue(created.getEmail().endsWith("@superadmin.local"));
    }
}
