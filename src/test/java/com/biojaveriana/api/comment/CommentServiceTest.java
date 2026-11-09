package com.biojaveriana.api.comment;

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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.biojaveriana.api.comment.dto.CommentCreateRequest;
import com.biojaveriana.api.comment.dto.CommentUpdateRequest;
import com.biojaveriana.api.comment.exceptions.CommentForbiddenException;
import com.biojaveriana.api.comment.exceptions.CommentNotFoundException;
import com.biojaveriana.api.post.Post;
import com.biojaveriana.api.post.PostRepository;
import com.biojaveriana.api.s3.S3UrlGenerator;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {
	@Mock
	private CommentRepository commentRepository;
	@Mock
	private CommentReactionRepository commentReactionRepository;
	@Mock
	private PostRepository postRepository;
	@Mock
	private S3UrlGenerator s3UrlGenerator;

	@InjectMocks
	private CommentService service;

	@Test
	void create_savesRootCommentAndMapsResponse() {
		User author = user(Role.USER);
		Post post = post();
		when(postRepository.findByIdAndHiddenFalseAndBannedFalse(post.getId())).thenReturn(Optional.of(post));
		when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
			Comment comment = invocation.getArgument(0);
			comment.setId(UUID.randomUUID());
			return comment;
		});

		var response = service.create(post.getId(), author, new CommentCreateRequest("A useful comment", null));

		assertThat(response.postId()).isEqualTo(post.getId());
		assertThat(response.parentId()).isNull();
		assertThat(response.content()).isEqualTo("A useful comment");
		assertThat(response.totalReactions()).isZero();
		verify(commentRepository).save(any(Comment.class));
	}

	@Test
	void create_rejectsReplyToReply() {
		User author = user(Role.USER);
		Post post = post();
		Comment root = comment(post, user(Role.USER));
		Comment parentReply = comment(post, user(Role.USER));
		parentReply.setParent(root);
		when(postRepository.findByIdAndHiddenFalseAndBannedFalse(post.getId())).thenReturn(Optional.of(post));
		when(commentRepository.findById(parentReply.getId())).thenReturn(Optional.of(parentReply));

		assertThatThrownBy(() -> service.create(post.getId(), author,
			new CommentCreateRequest("Reply", parentReply.getId())))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Cannot reply to a reply");
		verify(commentRepository, never()).save(any());
	}

	@Test
	void update_requiresAuthorOrAdmin() {
		User author = user(Role.USER);
		User other = user(Role.USER);
		Comment comment = comment(post(), author);
		when(commentRepository.findByIdAndHiddenFalseAndBannedFalse(comment.getId())).thenReturn(Optional.of(comment));

		assertThatThrownBy(() -> service.update(comment.getId(), other, new CommentUpdateRequest("Changed")))
			.isInstanceOf(CommentForbiddenException.class);
		verify(commentRepository, never()).save(any());
	}

	@Test
	void update_allowsAdminAndSavesContent() {
		User author = user(Role.USER);
		User admin = user(Role.ADMIN);
		Comment comment = comment(post(), author);
		when(commentRepository.findByIdAndHiddenFalseAndBannedFalse(comment.getId())).thenReturn(Optional.of(comment));
		when(commentRepository.save(comment)).thenReturn(comment);

		var response = service.update(comment.getId(), admin, new CommentUpdateRequest("Updated"));

		assertThat(response.content()).isEqualTo("Updated");
		assertThat(comment.getContent()).isEqualTo("Updated");
	}

	@Test
	void toggleReaction_addsReactionAndIncrementsCount() {
		User user = user(Role.USER);
		Comment comment = comment(post(), user(Role.USER));
		when(commentRepository.findByIdAndHiddenFalseAndBannedFalse(comment.getId())).thenReturn(Optional.of(comment));
		when(commentReactionRepository.findByCommentIdAndUserId(comment.getId(), user.getId()))
			.thenReturn(Optional.empty());
		when(commentRepository.save(comment)).thenReturn(comment);

		var response = service.toggleReaction(comment.getId(), user, ReactionType.LIKE);

		assertThat(comment.getTotalReactions()).isEqualTo(1);
		assertThat(response.totalReactions()).isEqualTo(1);
		verify(commentReactionRepository).save(any(CommentReaction.class));
	}

	@Test
	void toggleReaction_removesMatchingReactionAndDoesNotGoBelowZero() {
		User user = user(Role.USER);
		Comment comment = comment(post(), user(Role.USER));
		CommentReaction reaction = CommentReaction.builder()
			.comment(comment).user(user).emoji(ReactionType.LIKE).build();
		when(commentRepository.findByIdAndHiddenFalseAndBannedFalse(comment.getId())).thenReturn(Optional.of(comment));
		when(commentReactionRepository.findByCommentIdAndUserId(comment.getId(), user.getId()))
			.thenReturn(Optional.of(reaction));
		when(commentRepository.save(comment)).thenReturn(comment);

		service.toggleReaction(comment.getId(), user, ReactionType.LIKE);

		assertThat(comment.getTotalReactions()).isZero();
		verify(commentReactionRepository).delete(reaction);
	}

	@Test
	void toggleReaction_changesExistingEmojiWithoutChangingCount() {
		User user = user(Role.USER);
		Comment comment = comment(post(), user(Role.USER));
		comment.setTotalReactions(1);
		CommentReaction reaction = CommentReaction.builder()
			.comment(comment).user(user).emoji(ReactionType.LIKE).build();
		when(commentRepository.findByIdAndHiddenFalseAndBannedFalse(comment.getId())).thenReturn(Optional.of(comment));
		when(commentReactionRepository.findByCommentIdAndUserId(comment.getId(), user.getId()))
			.thenReturn(Optional.of(reaction));

		service.toggleReaction(comment.getId(), user, ReactionType.LOVE);

		assertThat(reaction.getEmoji()).isEqualTo(ReactionType.LOVE);
		assertThat(comment.getTotalReactions()).isEqualTo(1);
		verify(commentReactionRepository).save(reaction);
	}

	@Test
	void listByPost_usesNewestFirstAndMapsResults() {
		Post post = post();
		Comment comment = comment(post, user(Role.USER));
		when(commentRepository.findRootCommentsByPostId(eq(post.getId()), any(PageRequest.class)))
			.thenReturn(new PageImpl<>(List.of(comment), PageRequest.of(0, 10), 1));

		var response = service.listByPost(post.getId(), 0, 10);

		assertThat(response.content()).hasSize(1);
		assertThat(response.content().get(0).content()).isEqualTo(comment.getContent());
		verify(commentRepository).findRootCommentsByPostId(
			post.getId(), PageRequest.of(0, 10, Sort.by("createdAt").descending())
		);
	}

	@Test
	void getById_throwsForHiddenOrMissingComment() {
		UUID id = UUID.randomUUID();
		when(commentRepository.findByIdAndHiddenFalseAndBannedFalse(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getById(id, user(Role.USER)))
			.isInstanceOf(CommentNotFoundException.class);
	}

	@Test
	void countByPostId_delegatesToRepository() {
		UUID postId = UUID.randomUUID();
		when(commentRepository.countByPostId(postId)).thenReturn(4L);

		assertThat(service.countByPostId(postId)).isEqualTo(4L);
	}

	private static Comment comment(Post post, User author) {
		return Comment.builder()
			.id(UUID.randomUUID())
			.post(post)
			.author(author)
			.content("Comment text")
			.totalReactions(0)
			.hidden(false)
			.banned(false)
			.build();
	}

	private static Post post() {
		return Post.builder().id(UUID.randomUUID()).build();
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
