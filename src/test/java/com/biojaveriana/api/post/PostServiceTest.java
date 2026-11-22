package com.biojaveriana.api.post;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.biojaveriana.api.comment.CommentReactionRepository;
import com.biojaveriana.api.comment.CommentRepository;
import com.biojaveriana.api.gamification.event.EventDispatcher;
import com.biojaveriana.api.observation.Observation;
import com.biojaveriana.api.observation.ObservationRepository;
import com.biojaveriana.api.observation.exceptions.ObservationForbiddenException;
import com.biojaveriana.api.observation.exceptions.ObservationNotFoundException;
import com.biojaveriana.api.post.dto.PostCreateRequest;
import com.biojaveriana.api.post.dto.PostResponse;
import com.biojaveriana.api.post.dto.PostUpdateRequest;
import com.biojaveriana.api.post.exceptions.PostForbiddenException;
import com.biojaveriana.api.post.exceptions.PostImageLimitException;
import com.biojaveriana.api.post.exceptions.PostNotFoundException;
import com.biojaveriana.api.report.ReportRepository;
import com.biojaveriana.api.s3.ObjectService;
import com.biojaveriana.api.s3.S3UrlGenerator;
import com.biojaveriana.api.user.Gender;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private PostVoteRepository postVoteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObservationRepository observationRepository;

    @Mock
    private S3UrlGenerator s3UrlGenerator;

    @Mock
    private ObjectService objectService;

    @Mock
    private EventDispatcher eventDispatcher;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentReactionRepository commentReactionRepository;

    private PostService postService;

    private User user;

    @BeforeEach
    void setUp() {
        postService = new PostService(
            postRepository,
            postImageRepository,
            postVoteRepository,
            userRepository,
            observationRepository,
            s3UrlGenerator,
            objectService,
            eventDispatcher,
            reportRepository,
            commentRepository,
            commentReactionRepository
        );

        user = User.builder()
            .id(UUID.randomUUID())
            .username("jane")
            .email("jane@example.com")
            .passwordHash("hash")
            .firstName("Jane")
            .lastName("Doe")
            .gender(Gender.OTHER)
            .role(Role.USER)
            .build();
    }

    @Test
    @DisplayName("create: response must include non-null createdAt and updatedAt")
    void createShouldReturnPostWithTimestamps() {
        PostCreateRequest request = PostCreateRequest.builder()
            .title("Araña bajo la roca")
            .description("Found a spider under a rock in the mangrove")
            .build();

        when(postRepository.save(any(Post.class))).thenAnswer(inv -> {
            Post post = inv.getArgument(0);
            post.setId(UUID.randomUUID());
            return post;
        });

        PostResponse response = postService.create(user, request);

        assertNotNull(response.createdAt(), "createdAt must not be null in the create response");
        assertNotNull(response.updatedAt(), "updatedAt must not be null in the create response");
        assertEquals(request.getTitle(), response.title());
        assertEquals(request.getDescription(), response.description());
        assertEquals(user.getId(), response.author().id());
        verify(postRepository).save(any(Post.class));
        verify(eventDispatcher).dispatchPostCreated(eq(user.getId()), any(UUID.class));
    }

    @Test
    @DisplayName("create: image keys are persisted in the requested order")
    void createShouldPersistImagesInOrder() {
        PostCreateRequest request = PostCreateRequest.builder()
            .title("Post with images")
            .description("Two pictures")
            .imageKeys(new String[] { "posts/user/key1.jpg", "posts/user/key2.png" })
            .build();

        when(postRepository.save(any(Post.class))).thenAnswer(inv -> {
            Post post = inv.getArgument(0);
            post.setId(UUID.randomUUID());
            return post;
        });

        postService.create(user, request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PostImage>> captor = ArgumentCaptor.forClass(List.class);
        verify(postImageRepository).saveAll(captor.capture());
        List<PostImage> images = captor.getValue();

        assertEquals(2, images.size());
        assertEquals("posts/user/key1.jpg", images.get(0).getS3Key());
        assertEquals(0, images.get(0).getOrderIndex());
        assertEquals("posts/user/key2.png", images.get(1).getS3Key());
        assertEquals(1, images.get(1).getOrderIndex());
    }

    @Test
    @DisplayName("create: more than 10 images is rejected before saving")
    void createShouldRejectTooManyImages() {
        PostCreateRequest request = PostCreateRequest.builder()
            .title("Too many images")
            .description("Eleven pictures")
            .imageKeys(new String[11])
            .build();

        assertThrows(PostImageLimitException.class, () -> postService.create(user, request));
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("create: linking an own observation embeds its summary in the response")
    void createShouldLinkOwnObservation() {
        Observation observation = buildObservation(user);
        when(observationRepository.findById(observation.getId()))
            .thenReturn(Optional.of(observation));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> {
            Post post = inv.getArgument(0);
            post.setId(UUID.randomUUID());
            return post;
        });

        PostCreateRequest request = PostCreateRequest.builder()
            .title("With observation")
            .description("Linked to an observation")
            .observationId(observation.getId())
            .build();

        PostResponse response = postService.create(user, request);

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        assertSame(observation, captor.getValue().getObservation());

        assertNotNull(response.observation());
        assertEquals(observation.getId(), response.observation().id());
    }

    @Test
    @DisplayName("create: unknown observation id is rejected before saving")
    void createShouldRejectUnknownObservation() {
        UUID unknownObservationId = UUID.randomUUID();
        when(observationRepository.findById(unknownObservationId)).thenReturn(Optional.empty());

        PostCreateRequest request = PostCreateRequest.builder()
            .title("Bad observation")
            .description("Points to a missing observation")
            .observationId(unknownObservationId)
            .build();

        assertThrows(ObservationNotFoundException.class, () -> postService.create(user, request));
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("create: observation owned by another user is rejected before saving")
    void createShouldRejectForeignObservation() {
        User other = User.builder()
            .id(UUID.randomUUID())
            .username("john")
            .email("john@example.com")
            .passwordHash("hash")
            .firstName("John")
            .lastName("Doe")
            .gender(Gender.OTHER)
            .role(Role.USER)
            .build();
        Observation foreignObservation = buildObservation(other);
        when(observationRepository.findById(foreignObservation.getId()))
            .thenReturn(Optional.of(foreignObservation));

        PostCreateRequest request = PostCreateRequest.builder()
            .title("Foreign observation")
            .description("Tries to link someone else's observation")
            .observationId(foreignObservation.getId())
            .build();

        assertThrows(ObservationForbiddenException.class, () -> postService.create(user, request));
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("update: observationId re-links the post to a new observation")
    void updateShouldReLinkObservation() {
        UUID postId = UUID.randomUUID();
        Post existing = Post.builder()
            .id(postId)
            .author(user)
            .title("Old title")
            .description("Old description")
            .totalLikes(0)
            .totalDislikes(0)
            .hidden(false)
            .banned(false)
            .build();
        when(postRepository.findById(postId)).thenReturn(Optional.of(existing));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        Observation observation = buildObservation(user);
        when(observationRepository.findById(observation.getId()))
            .thenReturn(Optional.of(observation));

        PostResponse response = postService.update(postId, user,
            PostUpdateRequest.builder().observationId(observation.getId()).build());

        assertSame(observation, existing.getObservation());
        assertNotNull(response.observation());
        assertEquals(observation.getId(), response.observation().id());
    }

    @Test
    @DisplayName("toggleHidden: a superadmin can hide or unhide any post")
    void superadminCanToggleHidden() {
        User superadmin = buildUser(Role.SUPERADMIN);
        UUID postId = UUID.randomUUID();
        Post post = buildPost(postId, user, false, false);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        PostResponse response = postService.toggleHidden(postId, superadmin);

        assertTrue(response.hidden());
    }

    @Test
    @DisplayName("toggleHidden: a regular user is rejected before any repository access")
    void regularUserCannotToggleHidden() {
        UUID postId = UUID.randomUUID();

        assertThrows(PostForbiddenException.class, () -> postService.toggleHidden(postId, user));

        verify(postRepository, never()).findById(any(UUID.class));
    }

    @Test
    @DisplayName("toggleBanned: a superadmin can ban or unban any post")
    void superadminCanToggleBanned() {
        User superadmin = buildUser(Role.SUPERADMIN);
        UUID postId = UUID.randomUUID();
        Post post = buildPost(postId, user, false, false);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        PostResponse response = postService.toggleBanned(postId, superadmin);

        assertTrue(response.banned());
    }

    @Test
    @DisplayName("update: a superadmin can update a post authored by someone else")
    void superadminCanUpdateForeignPost() {
        User superadmin = buildUser(Role.SUPERADMIN);
        UUID postId = UUID.randomUUID();
        Post foreignPost = buildPost(postId, user, false, false);
        when(postRepository.findById(postId)).thenReturn(Optional.of(foreignPost));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        PostResponse response = postService.update(postId, superadmin,
            PostUpdateRequest.builder().title("Moderated title").build());

        assertEquals("Moderated title", response.title());
    }

    @Test
    @DisplayName("delete: a superadmin can delete a post authored by someone else")
    void superadminCanDeleteForeignPost() {
        User superadmin = buildUser(Role.SUPERADMIN);
        UUID postId = UUID.randomUUID();
        Post foreignPost = buildPost(postId, user, false, false);
        when(postRepository.findById(postId)).thenReturn(Optional.of(foreignPost));

        postService.delete(postId, superadmin);

        verify(postImageRepository).deleteByPostId(postId);
        verify(postRepository).delete(foreignPost);
    }

    @Test
    @DisplayName("list: a superadmin sees the full moderation feed, including hidden and banned posts")
    void superadminListSeesAllPosts() {
        User superadmin = buildUser(Role.SUPERADMIN);
        when(postRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        postService.list(superadmin, 0, 50);

        verify(postRepository).findAll(any(Pageable.class));
        verify(postRepository, never()).findByHiddenFalseAndBannedFalseOrderByCreatedAtDesc(any(Pageable.class));
    }

    @Test
    @DisplayName("getById: a superadmin can view a banned post authored by someone else")
    void superadminCanViewBannedPost() {
        User superadmin = buildUser(Role.SUPERADMIN);
        UUID postId = UUID.randomUUID();
        Post bannedPost = buildPost(postId, user, false, true);
        when(postRepository.findById(postId)).thenReturn(Optional.of(bannedPost));

        PostResponse response = postService.getById(postId, superadmin);

        assertTrue(response.banned());
    }

    @Test
    @DisplayName("getById: a regular user who is not the author cannot view a banned post")
    void regularUserCannotViewForeignBannedPost() {
        UUID postId = UUID.randomUUID();
        Post bannedPost = buildPost(postId, user, false, true);
        when(postRepository.findById(postId)).thenReturn(Optional.of(bannedPost));
        User other = buildUser(Role.USER);

        assertThrows(PostNotFoundException.class, () -> postService.getById(postId, other));
    }

    @Test
    @DisplayName("create: a superadmin can link another user's observation")
    void superadminCanLinkForeignObservation() {
        User superadmin = buildUser(Role.SUPERADMIN);
        User other = buildUser(Role.USER);
        Observation foreignObservation = buildObservation(other);
        when(observationRepository.findById(foreignObservation.getId()))
            .thenReturn(Optional.of(foreignObservation));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> {
            Post post = inv.getArgument(0);
            post.setId(UUID.randomUUID());
            return post;
        });

        PostCreateRequest request = PostCreateRequest.builder()
            .title("Superadmin post")
            .description("Links someone else's observation")
            .observationId(foreignObservation.getId())
            .build();

        PostResponse response = postService.create(superadmin, request);

        assertNotNull(response.observation());
        assertEquals(foreignObservation.getId(), response.observation().id());
    }

    @Test
    @DisplayName("delete: cascades to comments, reactions, votes and reports in a foreign-key-safe order")
    void deleteCascadesDependentData() {
        UUID postId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        Post post = buildPost(postId, user, false, false);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.findIdsByPostId(postId)).thenReturn(List.of(commentId));

        postService.delete(postId, user);

        InOrder inOrder = inOrder(reportRepository, commentReactionRepository, commentRepository,
            postImageRepository, postVoteRepository, postRepository);
        inOrder.verify(commentReactionRepository).deleteByCommentIdIn(List.of(commentId));
        inOrder.verify(reportRepository).deleteByReportedCommentIdIn(List.of(commentId));
        inOrder.verify(reportRepository).deleteByReportedPostId(postId);
        inOrder.verify(commentRepository).deleteByPostId(postId);
        inOrder.verify(postImageRepository).deleteByPostId(postId);
        inOrder.verify(postVoteRepository).deleteByPostId(postId);
        inOrder.verify(postRepository).delete(post);
    }

    @Test
    @DisplayName("delete: skips comment-related cleanup when the post has no comments")
    void deleteWithoutCommentsSkipsCommentCascade() {
        UUID postId = UUID.randomUUID();
        Post post = buildPost(postId, user, false, false);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.findIdsByPostId(postId)).thenReturn(List.of());

        postService.delete(postId, user);

        verify(commentReactionRepository, never()).deleteByCommentIdIn(any());
        verify(reportRepository, never()).deleteByReportedCommentIdIn(any());
        verify(reportRepository).deleteByReportedPostId(postId);
        verify(commentRepository).deleteByPostId(postId);
        verify(postVoteRepository).deleteByPostId(postId);
        verify(postRepository).delete(post);
    }

    private User buildUser(Role role) {
        return User.builder()
            .id(UUID.randomUUID())
            .username("user-" + role.name().toLowerCase())
            .email("user-" + role.name().toLowerCase() + "@example.com")
            .passwordHash("hash")
            .firstName("Test")
            .lastName("User")
            .gender(Gender.OTHER)
            .role(role)
            .build();
    }

    private Post buildPost(UUID id, User author, boolean hidden, boolean banned) {
        return Post.builder()
            .id(id)
            .author(author)
            .title("title")
            .description("description")
            .totalLikes(0)
            .totalDislikes(0)
            .hidden(hidden)
            .banned(banned)
            .build();
    }

    private Observation buildObservation(User owner) {
        return Observation.builder()
            .id(UUID.randomUUID())
            .user(owner)
            .referencePictureKey("observations/" + owner.getId() + "/ref.jpg")
            .latitude(1.25)
            .longitude(-86.25)
            .checked(true)
            .hidden(false)
            .revisionRequested(false)
            .build();
    }
}
