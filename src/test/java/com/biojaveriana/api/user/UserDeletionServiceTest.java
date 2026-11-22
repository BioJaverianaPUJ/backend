package com.biojaveriana.api.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.biojaveriana.api.comment.CommentReactionRepository;
import com.biojaveriana.api.comment.CommentRepository;
import com.biojaveriana.api.gamification.repository.UserAchievementRepository;
import com.biojaveriana.api.gamification.repository.UserGamificationRepository;
import com.biojaveriana.api.gamification.repository.XpLogRepository;
import com.biojaveriana.api.observation.ObservationJobRepository;
import com.biojaveriana.api.observation.ObservationRepository;
import com.biojaveriana.api.poi.PoiRepository;
import com.biojaveriana.api.poi.PoiTelemetryRepository;
import com.biojaveriana.api.post.PostImageRepository;
import com.biojaveriana.api.post.PostRepository;
import com.biojaveriana.api.post.PostVoteRepository;
import com.biojaveriana.api.report.ReportRepository;
import com.biojaveriana.api.s3.ObjectService;
import com.biojaveriana.api.user.exceptions.InvalidCurrentPasswordException;
import com.biojaveriana.api.user.exceptions.SuperAdminDeletionException;

@ExtendWith(MockitoExtension.class)
class UserDeletionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentReactionRepository commentReactionRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private BanRepository banRepository;

    @Mock
    private PoiRepository poiRepository;

    @Mock
    private PoiTelemetryRepository poiTelemetryRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private PostVoteRepository postVoteRepository;

    @Mock
    private ObservationRepository observationRepository;

    @Mock
    private ObservationJobRepository observationJobRepository;

    @Mock
    private XpLogRepository xpLogRepository;

    @Mock
    private UserAchievementRepository userAchievementRepository;

    @Mock
    private UserGamificationRepository userGamificationRepository;

    @Mock
    private ObjectService objectService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserDeletionService userDeletionService;

    @BeforeEach
    void setUp() {
        userDeletionService = new UserDeletionService(
            userRepository,
            commentRepository,
            commentReactionRepository,
            reportRepository,
            banRepository,
            poiRepository,
            poiTelemetryRepository,
            postRepository,
            postImageRepository,
            postVoteRepository,
            observationRepository,
            observationJobRepository,
            xpLogRepository,
            userAchievementRepository,
            userGamificationRepository,
            objectService,
            passwordEncoder
        );
    }

    @Nested
    @DisplayName("deleteAccount")
    class DeleteAccountTests {

        @Test
        @DisplayName("rejects deletion of the superadmin account")
        void shouldRejectSuperAdmin() {
            User superAdmin = mock(User.class);
            when(superAdmin.getUsername()).thenReturn("superadmin");

            assertThrows(SuperAdminDeletionException.class,
                () -> userDeletionService.deleteAccount(superAdmin));

            verifyNoInteractions(userRepository);
            verifyNoInteractions(objectService);
        }

        @Test
        @DisplayName("deletes the user, all related data and S3 files in a foreign-key-safe order")
        void shouldDeleteEverything() {
            UUID userId = UUID.randomUUID();
            UUID commentId = UUID.randomUUID();
            UUID poiId = UUID.randomUUID();
            UUID postId = UUID.randomUUID();
            UUID observationId = UUID.randomUUID();

            User user = mock(User.class);
            when(user.getId()).thenReturn(userId);
            when(user.getUsername()).thenReturn("alice");
            when(user.getProfilePictureKey()).thenReturn("users/profile-key");
            when(user.getBannerKey()).thenReturn("users/banner-key");

            when(commentRepository.findIdsByAuthorId(userId)).thenReturn(List.of(commentId));
            when(poiRepository.findIdsByCreatorId(userId)).thenReturn(List.of(poiId));
            when(postRepository.findIdsByAuthorId(userId)).thenReturn(List.of(postId));
            when(observationRepository.findIdsByUserId(userId)).thenReturn(List.of(observationId));
            when(observationRepository.findReferencePictureKeysByUserId(userId)).thenReturn(List.of("observations/ref-key"));
            when(postImageRepository.findS3KeysByPostIdIn(List.of(postId))).thenReturn(List.of("posts/img-key"));

            userDeletionService.deleteAccount(user);

            // S3 cleanup
            verify(objectService).delete("users/profile-key");
            verify(objectService).delete("users/banner-key");
            verify(objectService).delete("observations/ref-key");
            verify(objectService).delete("posts/img-key");

            // Every related table is cleaned up
            verify(commentReactionRepository).deleteByUserId(userId);
            verify(commentReactionRepository).deleteByCommentIdIn(List.of(commentId));
            verify(commentRepository).deleteByParentIdIn(List.of(commentId));
            verify(commentRepository).deleteByAuthorId(userId);
            verify(reportRepository).deleteByReporterIdOrReportedUserId(userId);
            verify(banRepository).deleteByUserId(userId);
            verify(banRepository).clearCreatedBy(userId);
            verify(poiTelemetryRepository).deleteByUserId(userId);
            verify(poiTelemetryRepository).deleteByPoiIdIn(List.of(poiId));
            verify(poiRepository).deleteByCreatorId(userId);
            verify(postVoteRepository).deleteByVoterId(userId);
            verify(postVoteRepository).deleteByPostIdIn(List.of(postId));
            verify(postImageRepository).deleteByPostIdIn(List.of(postId));
            verify(postRepository).deleteByAuthorId(userId);
            verify(observationJobRepository).deleteByObservationIdIn(List.of(observationId));
            verify(observationRepository).deleteByUserId(userId);
            verify(xpLogRepository).deleteByUserId(userId);
            verify(userAchievementRepository).deleteByUserId(userId);
            verify(userGamificationRepository).deleteByUserId(userId);

            // The user row is deleted last
            verify(userRepository).delete(user);

            // Children are deleted before their parents
            InOrder inOrder = inOrder(
                commentReactionRepository, commentRepository, postRepository,
                postImageRepository, postVoteRepository,
                poiRepository, poiTelemetryRepository,
                observationRepository, observationJobRepository,
                userGamificationRepository, userRepository
            );
            inOrder.verify(commentReactionRepository).deleteByCommentIdIn(List.of(commentId));
            inOrder.verify(commentRepository).deleteByAuthorId(userId);
            inOrder.verify(poiTelemetryRepository).deleteByPoiIdIn(List.of(poiId));
            inOrder.verify(poiRepository).deleteByCreatorId(userId);
            inOrder.verify(postVoteRepository).deleteByPostIdIn(List.of(postId));
            inOrder.verify(postImageRepository).deleteByPostIdIn(List.of(postId));
            inOrder.verify(postRepository).deleteByAuthorId(userId);
            inOrder.verify(observationJobRepository).deleteByObservationIdIn(List.of(observationId));
            inOrder.verify(observationRepository).deleteByUserId(userId);
            inOrder.verify(userGamificationRepository).deleteByUserId(userId);
            inOrder.verify(userRepository).delete(user);
        }

        @Test
        @DisplayName("skips collection-based deletes when the user has no related rows")
        void shouldSkipEmptyCollections() {
            UUID userId = UUID.randomUUID();

            User user = mock(User.class);
            when(user.getId()).thenReturn(userId);
            when(user.getUsername()).thenReturn("bob");

            when(commentRepository.findIdsByAuthorId(userId)).thenReturn(List.of());
            when(poiRepository.findIdsByCreatorId(userId)).thenReturn(List.of());
            when(postRepository.findIdsByAuthorId(userId)).thenReturn(List.of());
            when(observationRepository.findIdsByUserId(userId)).thenReturn(List.of());
            when(observationRepository.findReferencePictureKeysByUserId(userId)).thenReturn(List.of());

            userDeletionService.deleteAccount(user);

            verify(commentReactionRepository, never()).deleteByCommentIdIn(any());
            verify(commentRepository, never()).deleteByParentIdIn(any());
            verify(poiTelemetryRepository, never()).deleteByPoiIdIn(any());
            verify(postVoteRepository, never()).deleteByPostIdIn(any());
            verify(postImageRepository, never()).deleteByPostIdIn(any());
            verify(observationJobRepository, never()).deleteByObservationIdIn(any());
            verify(postImageRepository, never()).findS3KeysByPostIdIn(any());

            verify(userRepository).delete(user);
        }
    }

    @Nested
    @DisplayName("deleteMyAccount")
    class DeleteMyAccountTests {

        @Test
        @DisplayName("rejects deletion when the password does not match")
        void shouldRejectWrongPassword() {
            User user = mock(User.class);
            when(user.getPasswordHash()).thenReturn("hash");
            when(passwordEncoder.matches("wrong-password", "hash")).thenReturn(false);

            assertThrows(InvalidCurrentPasswordException.class,
                () -> userDeletionService.deleteMyAccount(user, "wrong-password"));

            verifyNoInteractions(userRepository);
            verifyNoInteractions(objectService);
        }

        @Test
        @DisplayName("deletes the account when the password matches")
        void shouldDeleteAccountWithCorrectPassword() {
            UUID userId = UUID.randomUUID();

            User user = mock(User.class);
            when(user.getId()).thenReturn(userId);
            when(user.getUsername()).thenReturn("alice");
            when(user.getPasswordHash()).thenReturn("hash");
            when(passwordEncoder.matches("correct-password", "hash")).thenReturn(true);

            when(commentRepository.findIdsByAuthorId(userId)).thenReturn(List.of());
            when(poiRepository.findIdsByCreatorId(userId)).thenReturn(List.of());
            when(postRepository.findIdsByAuthorId(userId)).thenReturn(List.of());
            when(observationRepository.findIdsByUserId(userId)).thenReturn(List.of());
            when(observationRepository.findReferencePictureKeysByUserId(userId)).thenReturn(List.of());

            userDeletionService.deleteMyAccount(user, "correct-password");

            verify(passwordEncoder).matches("correct-password", "hash");
            verify(userRepository).delete(user);
        }
    }
}
