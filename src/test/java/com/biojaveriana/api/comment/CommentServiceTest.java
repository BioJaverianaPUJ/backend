package com.biojaveriana.api.comment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.biojaveriana.api.comment.dto.CommentCreateRequest;
import com.biojaveriana.api.comment.dto.CommentListResponse;
import com.biojaveriana.api.comment.dto.CommentResponse;
import com.biojaveriana.api.comment.dto.CommentUpdateRequest;
import com.biojaveriana.api.comment.exceptions.CommentForbiddenException;
import com.biojaveriana.api.comment.exceptions.CommentNotFoundException;
import com.biojaveriana.api.gamification.event.EventDispatcher;
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

    @Mock
    private EventDispatcher eventDispatcher;

    private CommentService commentService;
    private User author;
    private User other;
    private User admin;
    private Post post;
    private UUID postId;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(commentRepository, commentReactionRepository, postRepository, s3UrlGenerator, eventDispatcher);
        author = User.builder().id(UUID.randomUUID()).username("jane").role(Role.USER).build();
        other = User.builder().id(UUID.randomUUID()).username("bob").role(Role.USER).build();
        admin = User.builder().id(UUID.randomUUID()).username("admin").role(Role.ADMIN).build();
        postId = UUID.randomUUID();
        post = Post.builder().id(postId).author(author).title("title").description("description").build();
        lenient().when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(UUID.randomUUID());
            }
            return c;
        });
        lenient().when(commentReactionRepository.save(any(CommentReaction.class))).thenAnswer(inv -> {
            CommentReaction r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(UUID.randomUUID());
            }
            return r;
        });
    }

    private Comment buildComment(UUID id, User commentAuthor, Integer totalReactions) {
        return Comment.builder()
            .id(id)
            .post(post)
            .author(commentAuthor)
            .content("hello")
            .totalReactions(totalReactions)
            .hidden(false)
            .banned(false)
            .build();
    }

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("a root comment is saved and a gamification event is dispatched")
        void createRootComment() {
            when(postRepository.findByIdAndHiddenFalseAndBannedFalse(postId)).thenReturn(Optional.of(post));

            CommentResponse response = commentService.create(postId, author, new CommentCreateRequest("hello world", null));

            assertNotNull(response.id());
            assertEquals(postId, response.postId());
            assertNull(response.parentId());
            assertEquals("hello world", response.content());
            assertEquals(0, response.totalReactions());
            assertFalse(response.banned());
            verify(eventDispatcher).dispatchCommentCreated(author.getId(), response.id());
        }

        @Test
        @DisplayName("a reply links the parent comment")
        void createReply() {
            when(postRepository.findByIdAndHiddenFalseAndBannedFalse(postId)).thenReturn(Optional.of(post));
            Comment parent = buildComment(UUID.randomUUID(), author, 0);
            when(commentRepository.findById(parent.getId())).thenReturn(Optional.of(parent));

            CommentResponse response = commentService.create(postId, author, new CommentCreateRequest("a reply", parent.getId()));

            assertEquals(parent.getId(), response.parentId());
        }

        @Test
        @DisplayName("a hidden or banned post cannot be commented on")
        void createOnInactivePostThrows() {
            when(postRepository.findByIdAndHiddenFalseAndBannedFalse(postId)).thenReturn(Optional.empty());

            assertThrows(CommentNotFoundException.class,
                () -> commentService.create(postId, author, new CommentCreateRequest("hi", null)));

            verify(commentRepository, never()).save(any(Comment.class));
        }
        @Test
        @DisplayName("a reply to a reply is rejected")
        void replyToReplyRejected() {
            when(postRepository.findByIdAndHiddenFalseAndBannedFalse(postId)).thenReturn(Optional.of(post));
            Comment parent = buildComment(UUID.randomUUID(), author, 0);
            parent.setParent(buildComment(UUID.randomUUID(), author, 0));
            when(commentRepository.findById(parent.getId())).thenReturn(Optional.of(parent));

            assertThrows(IllegalArgumentException.class,
                () -> commentService.create(postId, author, new CommentCreateRequest("nested", parent.getId())));
        }

        @Test
        @DisplayName("a reply whose parent belongs to another post is rejected")
        void replyWithForeignParentRejected() {
            when(postRepository.findByIdAndHiddenFalseAndBannedFalse(postId)).thenReturn(Optional.of(post));
            Post otherPost = Post.builder().id(UUID.randomUUID()).author(author).title("x").description("y").build();
            Comment parent = buildComment(UUID.randomUUID(), author, 0);
            parent.setPost(otherPost);
            when(commentRepository.findById(parent.getId())).thenReturn(Optional.of(parent));

            assertThrows(IllegalArgumentException.class,
                () -> commentService.create(postId, author, new CommentCreateRequest("nested", parent.getId())));
        }

        @Test
        @DisplayName("a reply to a hidden parent is treated as not found")
        void replyToHiddenParentThrows() {
            when(postRepository.findByIdAndHiddenFalseAndBannedFalse(postId)).thenReturn(Optional.of(post));
            Comment parent = buildComment(UUID.randomUUID(), author, 0);
            parent.setHidden(true);
            when(commentRepository.findById(parent.getId())).thenReturn(Optional.of(parent));

            assertThrows(CommentNotFoundException.class,
                () -> commentService.create(postId, author, new CommentCreateRequest("nested", parent.getId())));
        }
    }

    @Nested
    @DisplayName("update and delete")
    class UpdateDeleteTests {

        @Test
        @DisplayName("the author can update their own comment")
        void authorCanUpdate() {
            Comment comment = buildComment(UUID.randomUUID(), author, 0);
            when(commentRepository.findByIdAndHiddenFalseAndBannedFalse(comment.getId())).thenReturn(Optional.of(comment));

            CommentResponse response = commentService.update(comment.getId(), author, new CommentUpdateRequest("new content"));

            assertEquals("new content", response.content());
            verify(commentRepository).save(comment);
        }

        @Test
        @DisplayName("an admin can update any comment")
        void adminCanUpdate() {
            Comment comment = buildComment(UUID.randomUUID(), author, 0);
            when(commentRepository.findByIdAndHiddenFalseAndBannedFalse(comment.getId())).thenReturn(Optional.of(comment));

            CommentResponse response = commentService.update(comment.getId(), admin, new CommentUpdateRequest("moderated"));

            assertEquals("moderated", response.content());
        }

        @Test
        @DisplayName("another user cannot update the comment")
        void otherUserCannotUpdate() {
            Comment comment = buildComment(UUID.randomUUID(), author, 0);
            when(commentRepository.findByIdAndHiddenFalseAndBannedFalse(comment.getId())).thenReturn(Optional.of(comment));

            assertThrows(CommentForbiddenException.class,
                () -> commentService.update(comment.getId(), other, new CommentUpdateRequest("hack")));

            verify(commentRepository, never()).save(any(Comment.class));
        }

        @Test
        @DisplayName("updating a missing or inactive comment throws not found")
        void updateMissingThrows() {
            UUID commentId = UUID.randomUUID();
            when(commentRepository.findByIdAndHiddenFalseAndBannedFalse(commentId)).thenReturn(Optional.empty());

            assertThrows(CommentNotFoundException.class,
                () -> commentService.update(commentId, author, new CommentUpdateRequest("x")));
        }

        @Test
        @DisplayName("the author can delete their own comment")
        void authorCanDelete() {
            Comment comment = buildComment(UUID.randomUUID(), author, 0);
            when(commentRepository.findByIdAndHiddenFalseAndBannedFalse(comment.getId())).thenReturn(Optional.of(comment));

            commentService.delete(comment.getId(), author);

            verify(commentRepository).delete(comment);
        }

        @Test
        @DisplayName("another user cannot delete the comment")
        void otherUserCannotDelete() {
            Comment comment = buildComment(UUID.randomUUID(), author, 0);
            when(commentRepository.findByIdAndHiddenFalseAndBannedFalse(comment.getId())).thenReturn(Optional.of(comment));

            assertThrows(CommentForbiddenException.class, () -> commentService.delete(comment.getId(), other));

            verify(commentRepository, never()).delete(any(Comment.class));
        }
    }



    @Nested
    @DisplayName("reactions")
    class ReactionTests {

        @Test
        @DisplayName("a first reaction is created, counted and notifies the comment author")
        void firstReactionIsAdded() {
            Comment comment = buildComment(UUID.randomUUID(), author, 0);
            when(commentRepository.findByIdAndHiddenFalseAndBannedFalse(comment.getId())).thenReturn(Optional.of(comment));
            when(commentReactionRepository.findByCommentIdAndUserId(comment.getId(), other.getId())).thenReturn(Optional.empty());

            CommentResponse response = commentService.toggleReaction(comment.getId(), other, ReactionType.LOVE);

            assertEquals(1, response.totalReactions());
            verify(commentReactionRepository).save(any(CommentReaction.class));
            verify(eventDispatcher).dispatchCommentReactionReceived(eq(author.getId()), eq(comment.getId()), any(UUID.class));
        }

        @Test
        @DisplayName("repeating the same emoji removes the reaction and decrements the count")
        void sameEmojiRemovesReaction() {
            Comment comment = buildComment(UUID.randomUUID(), author, 1);
            CommentReaction existing = CommentReaction.builder()
                .id(UUID.randomUUID()).comment(comment).user(other).emoji(ReactionType.LOVE).build();
            when(commentRepository.findByIdAndHiddenFalseAndBannedFalse(comment.getId())).thenReturn(Optional.of(comment));
            when(commentReactionRepository.findByCommentIdAndUserId(comment.getId(), other.getId())).thenReturn(Optional.of(existing));

            CommentResponse response = commentService.toggleReaction(comment.getId(), other, ReactionType.LOVE);

            assertEquals(0, response.totalReactions());
            verify(commentReactionRepository).delete(existing);
            verify(eventDispatcher, never()).dispatchCommentReactionReceived(any(UUID.class), any(UUID.class), any(UUID.class));
        }

        @Test
        @DisplayName("reacting with a different emoji switches the reaction without changing the count")
        void differentEmojiSwitchesReaction() {
            Comment comment = buildComment(UUID.randomUUID(), author, 1);
            CommentReaction existing = CommentReaction.builder()
                .id(UUID.randomUUID()).comment(comment).user(other).emoji(ReactionType.LOVE).build();
            when(commentRepository.findByIdAndHiddenFalseAndBannedFalse(comment.getId())).thenReturn(Optional.of(comment));
            when(commentReactionRepository.findByCommentIdAndUserId(comment.getId(), other.getId())).thenReturn(Optional.of(existing));

            CommentResponse response = commentService.toggleReaction(comment.getId(), other, ReactionType.WOW);

            assertEquals(1, response.totalReactions());
            assertEquals(ReactionType.WOW, existing.getEmoji());
            verify(commentReactionRepository).save(existing);
            verify(commentReactionRepository, never()).delete(any(CommentReaction.class));
        }

        @Test
        @DisplayName("removing an existing reaction decrements the count")
        void removeExistingReaction() {
            Comment comment = buildComment(UUID.randomUUID(), author, 2);
            CommentReaction existing = CommentReaction.builder()
                .id(UUID.randomUUID()).comment(comment).user(other).emoji(ReactionType.HAHA).build();
            when(commentReactionRepository.findByCommentIdAndUserId(comment.getId(), other.getId())).thenReturn(Optional.of(existing));

            commentService.removeReaction(comment.getId(), other);

            verify(commentReactionRepository).delete(existing);
            assertEquals(1, comment.getTotalReactions());
            verify(commentRepository).save(comment);
        }

        @Test
        @DisplayName("removing a missing reaction throws not found")
        void removeMissingReactionThrows() {
            UUID commentId = UUID.randomUUID();

            assertThrows(CommentNotFoundException.class, () -> commentService.removeReaction(commentId, other));

            verify(commentReactionRepository, never()).delete(any(CommentReaction.class));
        }
    }


    @Nested
    @DisplayName("read and moderation")
    class ReadAndModerationTests {

        @Test
        @DisplayName("getById returns an active comment")
        void getByIdReturnsActiveComment() {
            Comment comment = buildComment(UUID.randomUUID(), author, 0);
            when(commentRepository.findByIdAndHiddenFalseAndBannedFalse(comment.getId())).thenReturn(Optional.of(comment));

            CommentResponse response = commentService.getById(comment.getId(), other);

            assertEquals(comment.getId(), response.id());
        }

        @Test
        @DisplayName("getById on a hidden or missing comment throws not found")
        void getByIdHiddenThrows() {
            UUID commentId = UUID.randomUUID();
            when(commentRepository.findByIdAndHiddenFalseAndBannedFalse(commentId)).thenReturn(Optional.empty());

            assertThrows(CommentNotFoundException.class, () -> commentService.getById(commentId, other));
        }

        @Test
        @DisplayName("listByPost returns the paginated root comments")
        void listByPostReturnsPage() {
            Comment comment = buildComment(UUID.randomUUID(), author, 0);
            when(commentRepository.findRootCommentsByPostId(eq(postId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(comment)));

            CommentListResponse response = commentService.listByPost(postId, 0, 20);

            assertEquals(1, response.content().size());
            assertEquals(comment.getId(), response.content().get(0).id());
        }

        @Test
        @DisplayName("listReplies requires an active parent comment")
        void listRepliesMissingParentThrows() {
            UUID parentId = UUID.randomUUID();
            when(commentRepository.findByIdAndHiddenFalseAndBannedFalse(parentId)).thenReturn(Optional.empty());

            assertThrows(CommentNotFoundException.class, () -> commentService.listReplies(parentId, 0, 20));
        }

        @Test
        @DisplayName("toggleHidden flips the hidden flag")
        void toggleHiddenFlipsFlag() {
            Comment comment = buildComment(UUID.randomUUID(), author, 0);
            when(commentRepository.findByIdAndHiddenFalseAndBannedFalse(comment.getId())).thenReturn(Optional.of(comment));

            commentService.toggleHidden(comment.getId(), admin);

            assertTrue(comment.getHidden());
            verify(commentRepository).save(comment);
        }

        @Test
        @DisplayName("toggleBanned flips the banned flag exposed in the response")
        void toggleBannedFlipsFlag() {
            Comment comment = buildComment(UUID.randomUUID(), author, 0);
            when(commentRepository.findByIdAndHiddenFalseAndBannedFalse(comment.getId())).thenReturn(Optional.of(comment));

            CommentResponse response = commentService.toggleBanned(comment.getId(), admin);

            assertTrue(response.banned());
        }

        @Test
        @DisplayName("a regular user who is not the author cannot moderate")
        void regularUserCannotModerate() {
            Comment comment = buildComment(UUID.randomUUID(), author, 0);
            when(commentRepository.findByIdAndHiddenFalseAndBannedFalse(comment.getId())).thenReturn(Optional.of(comment));

            assertThrows(CommentForbiddenException.class, () -> commentService.toggleHidden(comment.getId(), other));
            assertThrows(CommentForbiddenException.class, () -> commentService.toggleBanned(comment.getId(), other));
        }

        @Test
        @DisplayName("countByPostId delegates to the repository")
        void countByPostIdDelegates() {
            when(commentRepository.countByPostId(postId)).thenReturn(42L);

            assertEquals(42L, commentService.countByPostId(postId));
        }
    }
}
