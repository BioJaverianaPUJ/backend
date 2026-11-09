package com.biojaveriana.api.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;

import com.biojaveriana.api.s3.ObjectService;
import com.biojaveriana.api.user.dto.MeUserUpdateRequest;
import com.biojaveriana.api.user.dto.UserListResponse;
import com.biojaveriana.api.user.dto.UserResponse;
import com.biojaveriana.api.user.dto.UserUpdateRequest;
import com.biojaveriana.api.user.exceptions.InvalidMediaTypeException;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private ObjectService objectService;

    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController(userService, objectService);
    }

    @Test
    void getCurrentUser_returnsMappedResponse() {
        User user = user();
        when(objectService.generateUrl("profile.jpg")).thenReturn("https://cdn/profile.jpg");
        when(objectService.generateUrl("banner.jpg")).thenReturn("https://cdn/banner.jpg");
        user.setProfilePictureKey("profile.jpg");
        user.setBannerKey("banner.jpg");

        UserResponse response = controller.getCurrentUser(user).getBody();

        assertResponseMatches(response, user, "https://cdn/profile.jpg", "https://cdn/banner.jpg");
    }

    @Test
    void getUserById_parsesIdAndMapsResponse() {
        User user = user();
        when(userService.findById(user.getId())).thenReturn(user);

        UserResponse response = controller.getUserById(user.getId()).getBody();

        verify(userService).findById(user.getId());
        assertResponseMatches(response, user, null, null);
    }

    @Test
    void getUserByAtUsername_findsAndMapsResponse() {
        User user = user();
        when(userService.findByUsername(user.getUsername())).thenReturn(user);

        UserResponse response = controller.getUserByAtUsername(user.getUsername()).getBody();

        verify(userService).findByUsername(user.getUsername());
        assertResponseMatches(response, user, null, null);
    }

    @Test
    void findAll_buildsSortedPageRequestAndMapsPage() {
        User user = user();
        when(userService.findAll(any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(2, 15, Sort.by("username").ascending()), 31));

        UserListResponse response = controller.findAll(2, 15).getBody();

        assertThat(response.content()).singleElement().extracting("id").isEqualTo(user.getId());
        verify(userService).findAll(PageRequest.of(2, 15, Sort.by("username").ascending()));
        assertThat(response.pagination()).isNotNull();
    }

    @Test
    void update_delegatesParsedIdAndMapsResponse() {
        User user = user();
        UserUpdateRequest request = new UserUpdateRequest(null, null, "updated", null, null);
        when(userService.updateById(user.getId(), request)).thenReturn(user);

        UserResponse response = controller.update(user.getId(), request).getBody();

        verify(userService).updateById(user.getId(), request);
        assertResponseMatches(response, user, null, null);
    }

    @Test
    void updateMe_delegatesCurrentUserAndMapsResponse() {
        User user = user();
        MeUserUpdateRequest request = new MeUserUpdateRequest("New", null, null, null);
        when(userService.updateMe(user, request)).thenReturn(user);

        UserResponse response = controller.updateMe(user, request).getBody();

        verify(userService).updateMe(user, request);
        assertResponseMatches(response, user, null, null);
    }

    @Test
    void uploadMedia_replacesOldProfilePictureAndPersistsNewKey() throws Exception {
        User user = user();
        user.setProfilePictureKey("old.jpg");
        MockMultipartFile file = new MockMultipartFile(
            "file", "new.png", "image/png", "image".getBytes()
        );

        UserResponse response = controller.uploadMedia(user, "profile-picture", file).getBody();

        verify(objectService).delete("old.jpg");
        verify(objectService).upload(
            any(String.class), any(ByteArrayInputStream.class), eq((long) 5), eq("image/png")
        );
        verify(userService).save(user);
        assertThat(user.getProfilePictureKey()).startsWith("users/" + user.getId() + "/profile-picture/");
        assertThat(response.id()).isEqualTo(user.getId());
    }

    @Test
    void uploadMedia_rejectsUnknownMediaType() {
        User user = user();
        MockMultipartFile file = new MockMultipartFile("file", "x.png", "image/png", new byte[] {1});

        assertThatThrownBy(() -> controller.uploadMedia(user, "avatar", file))
            .isInstanceOf(InvalidMediaTypeException.class);
        verify(objectService, never()).upload(any(), any(), any(Long.class), any());
    }

    @Test
    void deleteMedia_removesExistingBanner() {
        User user = user();
        user.setBannerKey("old-banner.jpg");

        UserResponse response = controller.deleteMedia(user, "banner").getBody();

        verify(objectService).delete("old-banner.jpg");
        verify(userService).save(user);
        assertThat(user.getBannerKey()).isNull();
        assertThat(response.id()).isEqualTo(user.getId());
    }

    @Test
    void deleteMedia_doesNotSaveWhenMediaDoesNotExist() {
        User user = user();

        controller.deleteMedia(user, "banner");

        verify(objectService, never()).delete(any());
        verify(userService, never()).save(any());
    }

    @Test
    void deleteMedia_rejectsUnknownMediaType() {
        assertThatThrownBy(() -> controller.deleteMedia(user(), "avatar"))
            .isInstanceOf(InvalidMediaTypeException.class);
    }

    @Test
    void uploadMediaForAdmin_findsUserAndUploads() throws Exception {
        User user = user();
        MockMultipartFile file = new MockMultipartFile("file", "new.jpg", "image/jpeg", new byte[] {1, 2});
        when(userService.findByUsername("test-user")).thenReturn(user);

        controller.uploadMediaForAdmin("test-user", "profile-picture", file);

        verify(userService).findByUsername("test-user");
        verify(objectService).upload(any(String.class), any(), eq(2L), eq("image/jpeg"));
        verify(userService).save(user);
    }

    @Test
    void deleteMediaForAdmin_findsUserAndDeletesMedia() {
        User user = user();
        user.setBannerKey("banner.gif");
        when(userService.findByUsername("test-user")).thenReturn(user);

        controller.deleteMediaForAdmin("test-user", "banner");

        verify(userService).findByUsername("test-user");
        verify(objectService).delete("banner.gif");
        verify(userService).save(user);
        assertThat(user.getBannerKey()).isNull();
    }

    @Test
    void delete_delegatesParsedId() {
        UUID id = UUID.randomUUID();
        User user = user();
        User admin = user();
        when(userService.findById(id)).thenReturn(user);

        controller.delete(id, admin);

        verify(userService).deleteById(id);
    }

    private static void assertResponseMatches(
        UserResponse response, User user, String profileUrl, String bannerUrl
    ) {
        assertThat(response.id()).isEqualTo(user.getId());
        assertThat(response.firstName()).isEqualTo(user.getFirstName());
        assertThat(response.lastName()).isEqualTo(user.getLastName());
        assertThat(response.username()).isEqualTo(user.getUsername());
        assertThat(response.email()).isEqualTo(user.getEmail());
        assertThat(response.role()).isEqualTo(user.getRole());
        assertThat(response.gender()).isEqualTo(user.getGender());
        assertThat(response.profilePictureUrl()).isEqualTo(profileUrl);
        assertThat(response.bannerUrl()).isEqualTo(bannerUrl);
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
