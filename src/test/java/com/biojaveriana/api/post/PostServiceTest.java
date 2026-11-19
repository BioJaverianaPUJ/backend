package com.biojaveriana.api.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.biojaveriana.api.post.dto.PostCreateRequest;
import com.biojaveriana.api.post.dto.PostUpdateRequest;
import com.biojaveriana.api.post.dto.PostVoteRequest;
import com.biojaveriana.api.post.exceptions.PostForbiddenException;
import com.biojaveriana.api.post.exceptions.PostImageLimitException;
import com.biojaveriana.api.post.exceptions.PostNotFoundException;
import com.biojaveriana.api.s3.S3UrlGenerator;
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

	@InjectMocks
	private PostService service;

	@Test
	void createPresign_returnsUserScopedImageKey() {
		User user = user(Role.USER);

		var presign = service.createPresign(user, "image/png");

		assertThat(presign.key()).startsWith("posts/" + user.getId() + "/");
		assertThat(presign.key()).endsWith(".png");
		assertThat(presign.contentType()).isEqualTo("image/png");
		assertThat(presign.method()).isEqualTo("PUT");
	}

	@Test
	void create_savesPostAndOrderedImages() {
		User author = user(Role.USER);
		PostCreateRequest request = new PostCreateRequest(
			"Field notes", "A description", "Observation text", new String[] {"first.jpg", "second.jpg"}
		);
		when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
			Post post = invocation.getArgument(0);
			post.setId(UUID.randomUUID());
			return post;
		});

		var response = service.create(author, request);

		assertThat(response.title()).isEqualTo("Field notes");
		assertThat(response.totalLikes()).isZero();
		assertThat(response.images()).hasSize(0);
		ArgumentCaptor<List<PostImage>> images = ArgumentCaptor.forClass(List.class);
		verify(postImageRepository).saveAll(images.capture());
		assertThat(images.getValue()).extracting(PostImage::getS3Key).containsExactly("first.jpg", "second.jpg");
		assertThat(images.getValue()).extracting(PostImage::getOrderIndex).containsExactly(0, 1);
	}

	@Test
	void create_rejectsMoreThanTenImages() {
		String[] imageKeys = new String[11];
		PostCreateRequest request = new PostCreateRequest("Title", "Description", null, imageKeys);

		assertThatThrownBy(() -> service.create(user(Role.USER), request))
			.isInstanceOf(PostImageLimitException.class);
		verify(postRepository, never()).save(any());
	}

	@Test
	void update_allowsAuthorAndAppliesProvidedFields() {
		User author = user(Role.USER);
		Post post = post(author);
		when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
		when(postRepository.save(post)).thenReturn(post);

		var response = service.update(post.getId(), author,
			new PostUpdateRequest("New title", null, "Updated observation"));

		assertThat(response.title()).isEqualTo("New title");
		assertThat(post.getDescription()).isEqualTo("Description");
		assertThat(post.getObservation()).isEqualTo("Updated observation");
	}

	@Test
	void update_rejectsNonAuthorWhoIsNotAdmin() {
		Post post = post(user(Role.USER));
		when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

		assertThatThrownBy(() -> service.update(post.getId(), user(Role.USER),
			new PostUpdateRequest("Changed", null, null)))
			.isInstanceOf(PostForbiddenException.class);
		verify(postRepository, never()).save(any());
	}

	@Test
	void delete_allowsAdminAndDeletesImagesBeforePost() {
		Post post = post(user(Role.USER));
		User admin = user(Role.ADMIN);
		when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

		service.delete(post.getId(), admin);

		verify(postImageRepository).deleteByPostId(post.getId());
		verify(postRepository).delete(post);
	}

	@Test
	void getById_hidesBannedPostFromOtherUsersButAllowsAuthor() {
		User author = user(Role.USER);
		Post post = post(author);
		post.setBanned(true);
		when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
		when(postVoteRepository.findByPostIdAndVoterId(eq(post.getId()), any(UUID.class)))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getById(post.getId(), user(Role.USER)))
			.isInstanceOf(PostNotFoundException.class);
		assertThat(service.getById(post.getId(), author).id()).isEqualTo(post.getId());
	}

	@Test
	void list_usesAllPostsForAdminAndActivePostsForUsers() {
		User admin = user(Role.ADMIN);
		User regular = user(Role.USER);
		when(postRepository.findAll(any(PageRequest.class)))
			.thenReturn(new PageImpl<>(List.of(post(regular))));
		when(postRepository.findByHiddenFalseAndBannedFalseOrderByCreatedAtDesc(any(PageRequest.class)))
			.thenReturn(new PageImpl<>(List.of()));

		service.list(admin, 0, 10);
		service.list(regular, 0, 10);

		verify(postRepository).findAll(PageRequest.of(0, 10, Sort.by("createdAt").descending()));
		verify(postRepository).findByHiddenFalseAndBannedFalseOrderByCreatedAtDesc(
			PageRequest.of(0, 10, Sort.by("createdAt").descending())
		);
	}

	@Test
	void vote_addsLikeAndIncrementsCounter() {
		User voter = user(Role.USER);
		Post post = post(user(Role.USER));
		when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
		when(postVoteRepository.findByPostIdAndVoterId(post.getId(), voter.getId())).thenReturn(Optional.empty());
		when(postRepository.save(post)).thenReturn(post);

		var response = service.vote(post.getId(), voter, new PostVoteRequest(VoteType.LIKE));

		assertThat(post.getTotalLikes()).isEqualTo(1);
		assertThat(response.totalLikes()).isEqualTo(1);
		verify(postVoteRepository).save(any(PostVote.class));
	}

	@Test
	void vote_switchesExistingVoteAndAdjustsBothCounters() {
		User voter = user(Role.USER);
		Post post = post(user(Role.USER));
		post.setTotalLikes(1);
		PostVote vote = PostVote.builder().post(post).voter(voter).voteType(VoteType.LIKE).build();
		when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
		when(postVoteRepository.findByPostIdAndVoterId(post.getId(), voter.getId())).thenReturn(Optional.of(vote));
		when(postRepository.save(post)).thenReturn(post);

		service.vote(post.getId(), voter, new PostVoteRequest(VoteType.DISLIKE));

		assertThat(vote.getVoteType()).isEqualTo(VoteType.DISLIKE);
		assertThat(post.getTotalLikes()).isZero();
		assertThat(post.getTotalDislikes()).isEqualTo(1);
		verify(postVoteRepository).save(vote);
	}

	@Test
	void toggleHidden_requiresAdminAndChangesVisibility() {
		User admin = user(Role.ADMIN);
		Post post = post(user(Role.USER));
		when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
		when(postRepository.save(post)).thenReturn(post);

		var response = service.toggleHidden(post.getId(), admin);

		assertThat(post.getHidden()).isTrue();
		assertThat(response.hidden()).isTrue();
		assertThatThrownBy(() -> service.toggleHidden(post.getId(), user(Role.USER)))
			.isInstanceOf(PostForbiddenException.class);
	}

	private static Post post(User author) {
		return Post.builder()
			.id(UUID.randomUUID())
			.author(author)
			.title("Title")
			.description("Description")
			.totalLikes(0)
			.totalDislikes(0)
			.hidden(false)
			.banned(false)
			.build();
	}

	private static User user(Role role) {
		return User.builder()
			.id(UUID.randomUUID())
			.username("tester")
			.firstName("Test")
			.lastName("User")
			.role(role)
			.build();
	}
}
