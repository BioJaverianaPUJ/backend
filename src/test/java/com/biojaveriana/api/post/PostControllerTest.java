package com.biojaveriana.api.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.biojaveriana.api.post.dto.PostCreateRequest;
import com.biojaveriana.api.post.dto.PostImageUploadPresignRequest;
import com.biojaveriana.api.post.dto.PostListResponse;
import com.biojaveriana.api.post.dto.PostResponse;
import com.biojaveriana.api.post.dto.PostUpdateRequest;
import com.biojaveriana.api.post.dto.PostVoteRequest;
import com.biojaveriana.api.s3.UploadPresign;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {
	@Mock
	private PostService postService;

	private PostController controller;

	@BeforeEach
	void setUp() {
		controller = new PostController(postService);
	}

	@Test
	void createPresign_forwardsContentType() {
		User user = user(Role.USER);
		PostImageUploadPresignRequest request = new PostImageUploadPresignRequest("image/jpeg");
		UploadPresign presign = new UploadPresign("key", "", "PUT", "image/jpeg", null);
		when(postService.createPresign(user, "image/jpeg")).thenReturn(presign);

		var response = controller.createPresign(user, request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isSameAs(presign);
	}

	@Test
	void create_returnsCreatedPost() {
		User user = user(Role.USER);
		PostCreateRequest request = new PostCreateRequest("Title", "Description", null, null);
		PostResponse result = response();
		when(postService.create(user, request)).thenReturn(result);

		var response = controller.create(user, request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isSameAs(result);
	}

	@Test
	void updateAndGetById_forwardIdAndUser() {
		UUID postId = UUID.randomUUID();
		User user = user(Role.USER);
		PostUpdateRequest request = new PostUpdateRequest("Updated", null, null);
		PostResponse result = response();
		when(postService.update(postId, user, request)).thenReturn(result);
		when(postService.getById(postId, user)).thenReturn(result);

		assertThat(controller.update(postId, user, request).getBody()).isSameAs(result);
		assertThat(controller.getById(postId, user).getBody()).isSameAs(result);
	}

	@Test
	void delete_returnsNoContentAndDelegates() {
		UUID postId = UUID.randomUUID();
		User user = user(Role.USER);

		var response = controller.delete(postId, user);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(postService).delete(postId, user);
	}

	@Test
	void listAndListByAuthor_forwardPagination() {
		UUID authorId = UUID.randomUUID();
		User user = user(Role.USER);
		PostListResponse result = new PostListResponse(List.of(), null);
		when(postService.list(user, 1, 10)).thenReturn(result);
		when(postService.listByAuthor(authorId, user, 1, 10)).thenReturn(result);

		assertThat(controller.list(user, 1, 10).getBody()).isSameAs(result);
		assertThat(controller.listByAuthor(authorId, user, 1, 10).getBody()).isSameAs(result);
	}

	@Test
	void voteAndRemoveVote_delegateToService() {
		UUID postId = UUID.randomUUID();
		User user = user(Role.USER);
		PostVoteRequest request = new PostVoteRequest(VoteType.LIKE);
		PostResponse result = response();
		when(postService.vote(postId, user, request)).thenReturn(result);
		when(postService.removeVote(postId, user)).thenReturn(result);

		assertThat(controller.vote(postId, user, request).getBody()).isSameAs(result);
		assertThat(controller.removeVote(postId, user).getBody()).isSameAs(result);
	}

	@Test
	void adminListAndModerationEndpoints_delegate() {
		User admin = user(Role.ADMIN);
		UUID postId = UUID.randomUUID();
		PostListResponse list = new PostListResponse(List.of(), null);
		PostResponse post = response();
		when(postService.listAllForAdmin(0, 50)).thenReturn(list);
		when(postService.toggleHidden(postId, admin)).thenReturn(post);
		when(postService.toggleBanned(postId, admin)).thenReturn(post);

		assertThat(controller.listAllForAdmin(0, 50).getBody()).isSameAs(list);
		assertThat(controller.toggleHidden(postId, admin).getBody()).isSameAs(post);
		assertThat(controller.toggleBanned(postId, admin).getBody()).isSameAs(post);
	}

	private static PostResponse response() {
		return new PostResponse(UUID.randomUUID(), null, "Title", "Description", null,
			List.of(), 0, 0, false, null, false, false, null, null);
	}

	private static User user(Role role) {
		return User.builder().id(UUID.randomUUID()).username("tester").role(role).build();
	}
}
