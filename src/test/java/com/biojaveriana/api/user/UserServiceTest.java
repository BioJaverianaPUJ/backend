package com.biojaveriana.api.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.biojaveriana.api.user.dto.MeUserUpdateRequest;
import com.biojaveriana.api.user.dto.UserUpdateRequest;
import com.biojaveriana.api.user.exceptions.UserNotFoundException;
import com.biojaveriana.api.user.exceptions.UsernameAlreadyExistsException;
import com.biojaveriana.api.user.exceptions.UsernameChangeCooldownException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    void findAll_delegatesToRepository() {
        Pageable pageable = PageRequest.of(1, 10);
        PageImpl<User> page = new PageImpl<>(java.util.List.of(user()));
        when(userRepository.findAll(pageable)).thenReturn(page);

        assertThat(userService.findAll(pageable)).isSameAs(page);
        verify(userRepository).findAll(pageable);
    }

    @Test
    void findById_returnsUser() {
        User user = user();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThat(userService.findById(user.getId())).isSameAs(user);
    }

    @Test
    void findById_throwsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(id))
            .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void findByUsername_returnsUser() {
        User user = user();
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

        assertThat(userService.findByUsername(user.getUsername())).isSameAs(user);
    }

    @Test
    void findByUsername_throwsWhenMissing() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByUsername("missing"))
            .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updateById_updatesProvidedFieldsAndSaves() {
        User user = user();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("new-user")).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.updateById(user.getId(), new UserUpdateRequest(
            null, null, "new-user", Role.ADMIN, Gender.FEMALE, false
        ));

        assertThat(result).isSameAs(user);
        assertThat(user.getUsername()).isEqualTo("new-user");
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        assertThat(user.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(user.isDeactivated()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void updateById_doesNotCheckUsernameWhenUnchanged() {
        User user = user();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        userService.updateById(user.getId(), new UserUpdateRequest(
            null, null, user.getUsername(), null, null, null
        ));

        verify(userRepository).save(user);
        verify(userRepository, never()).existsByUsername(any());
    }

    @Test
    void updateById_rejectsExistingUsername() {
        User user = user();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateById(user.getId(), new UserUpdateRequest(
            null, null, "taken", null, null, null
        ))).isInstanceOf(UsernameAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteById_findsAndDeletesUser() {
        User user = user();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        userService.deleteById(user.getId());

        verify(userRepository).delete(user);
    }

    @Test
    void disableUser_deactivatesAndSavesUser() {
        User user = user();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        assertThat(userService.disableUser(user.getId()).isDeactivated()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void updateMe_updatesProfileFields() {
        User user = user();
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.updateMe(user, new MeUserUpdateRequest(
            "Ana", "Gomez", null, Gender.FEMALE
        ));

        assertThat(result).isSameAs(user);
        assertThat(user.getFirstName()).isEqualTo("Ana");
        assertThat(user.getLastName()).isEqualTo("Gomez");
        assertThat(user.getGender()).isEqualTo(Gender.FEMALE);
        verify(userRepository).save(user);
    }

    @Test
    void updateMe_changesUsernameAfterCooldown() {
        User user = user();
        user.setUsernameLastUpdate(Instant.now().minusSeconds(14 * 24 * 60 * 60 + 1));
        when(userRepository.existsByUsername("new-user")).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        userService.updateMe(user, new MeUserUpdateRequest(null, null, "new-user", null));

        assertThat(user.getUsername()).isEqualTo("new-user");
        assertThat(user.getUsernameLastUpdate()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void updateMe_rejectsExistingUsername() {
        User user = user();
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateMe(user, new MeUserUpdateRequest(
            null, null, "taken", null
        ))).isInstanceOf(UsernameAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateMe_rejectsUsernameChangedTooSoon() {
        User user = user();
        user.setUsernameLastUpdate(Instant.now().minusSeconds(13 * 24 * 60 * 60));
        when(userRepository.existsByUsername("new-user")).thenReturn(false);

        assertThatThrownBy(() -> userService.updateMe(user, new MeUserUpdateRequest(
            null, null, "new-user", null
        ))).isInstanceOf(UsernameChangeCooldownException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void setProfilePicture_updatesKeyAndSaves() {
        User user = user();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        userService.setProfilePicture(user.getId(), "profile.jpg");

        assertThat(user.getProfilePictureKey()).isEqualTo("profile.jpg");
        verify(userRepository).save(user);
    }

    @Test
    void setBanner_updatesKeyAndSaves() {
        User user = user();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        userService.setBanner(user.getId(), "banner.jpg");

        assertThat(user.getBannerKey()).isEqualTo("banner.jpg");
        verify(userRepository).save(user);
    }

    @Test
    void save_delegatesToRepository() {
        User user = user();
        when(userRepository.save(user)).thenReturn(user);

        assertThat(userService.save(user)).isSameAs(user);
        verify(userRepository).save(user);
    }

    private static User user() {
        return User.builder()
            .id(UUID.randomUUID())
            .username("test-user")
            .email("test@example.com")
            .passwordHash("hash")
            .firstName("Test")
            .lastName("User")
            .gender(Gender.MALE)
            .role(Role.USER)
            .build();
    }
}
