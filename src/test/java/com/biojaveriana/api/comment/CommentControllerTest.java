package com.biojaveriana.api.comment;

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

import com.biojaveriana.api.comment.dto.CommentCreateRequest;
import com.biojaveriana.api.comment.dto.CommentListResponse;
import com.biojaveriana.api.comment.dto.CommentReactionRequest;
import com.biojaveriana.api.comment.dto.CommentResponse;
import com.biojaveriana.api.comment.dto.CommentUpdateRequest;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {
	@Mock
	private CommentService commentService;

	private CommentController controller;

	@BeforeEach
	void setUp() {
		controller = new CommentController(commentService);
	}

	@Test
	void create_delegatesAndReturnsCreated() {
		UUID postId = UUID.randomUUID();
		User user = user();
		CommentCreateRequest request = new CommentCreateRequest("Hello", null);
		CommentResponse result = response();
		when(commentService.create(postId, user, request)).thenReturn(result);

		var response = controller.create(postId, user, request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isSameAs(result);
	}

	@Test
	void updateAndGetById_forwardCommentIdAndUser() {
		UUID commentId = UUID.randomUUID();
		User user = user();
		CommentUpdateRequest request = new CommentUpdateRequest("Updated");
		CommentResponse result = response();
		when(commentService.update(commentId, user, request)).thenReturn(result);
		when(commentService.getById(commentId, user)).thenReturn(result);

		assertThat(controller.update(commentId, user, request).getBody()).isSameAs(result);
		assertThat(controller.getById(commentId, user).getBody()).isSameAs(result);
	}

	@Test
	void delete_returnsNoContent() {
		UUID commentId = UUID.randomUUID();
		User user = user();
		var response = controller.delete(commentId, user);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(commentService).delete(commentId, user);
	}

	@Test
	void listAndCount_forwardPaginationAndPostId() {
		UUID postId = UUID.randomUUID();
		CommentListResponse list = new CommentListResponse(List.of(), null);
		when(commentService.listByPost(postId, 1, 15)).thenReturn(list);
		when(commentService.countByPostId(postId)).thenReturn(3L);

		assertThat(controller.listByPost(postId, 1, 15).getBody()).isSameAs(list);
		assertThat(controller.countByPost(postId).getBody()).isEqualTo(3L);
		verify(commentService).listByPost(postId, 1, 15);
	}

	@Test
	void listReplies_forwardsParentAndPagination() {
		UUID parentId = UUID.randomUUID();
		CommentListResponse list = new CommentListResponse(List.of(), null);
		when(commentService.listReplies(parentId, 0, 20)).thenReturn(list);

		assertThat(controller.listReplies(parentId, 0, 20).getBody()).isSameAs(list);
	}

	@Test
	void reactionAndModerationEndpointsDelegate() {
		UUID commentId = UUID.randomUUID();
		User user = user();
		CommentResponse result = response();
		when(commentService.toggleReaction(commentId, user, ReactionType.LIKE)).thenReturn(result);
		when(commentService.toggleHidden(commentId, user)).thenReturn(result);
		when(commentService.toggleBanned(commentId, user)).thenReturn(result);

		assertThat(controller.toggleReaction(commentId, user, new CommentReactionRequest(ReactionType.LIKE)).getBody())
			.isSameAs(result);
		assertThat(controller.toggleHidden(commentId, user).getBody()).isSameAs(result);
		assertThat(controller.toggleBanned(commentId, user).getBody()).isSameAs(result);
		controller.removeReaction(commentId, user);
		verify(commentService).removeReaction(commentId, user);
	}

	private static CommentResponse response() {
		return new CommentResponse(UUID.randomUUID(), null, UUID.randomUUID(), null,
			"Comment", 0, List.of(), false, null, null);
	}

	private static User user() {
		return User.builder().id(UUID.randomUUID()).username("tester").role(Role.USER).build();
	}
}
