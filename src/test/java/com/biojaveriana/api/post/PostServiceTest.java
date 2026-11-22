package com.biojaveriana.api.post;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.biojaveriana.api.gamification.event.EventDispatcher;
import com.biojaveriana.api.post.dto.PostCreateRequest;
import com.biojaveriana.api.post.dto.PostResponse;
import com.biojaveriana.api.post.exceptions.PostImageLimitException;
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
    private S3UrlGenerator s3UrlGenerator;

    @Mock
    private ObjectService objectService;

    @Mock
    private EventDispatcher eventDispatcher;

    private PostService postService;

    private User user;

    @BeforeEach
    void setUp() {
        postService = new PostService(
            postRepository,
            postImageRepository,
            postVoteRepository,
            userRepository,
            s3UrlGenerator,
            objectService,
            eventDispatcher
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
            .observation("Spotted at 10:30 AM")
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
}
