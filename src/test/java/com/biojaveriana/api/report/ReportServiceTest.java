package com.biojaveriana.api.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.data.domain.Pageable;

import com.biojaveriana.api.comment.Comment;
import com.biojaveriana.api.comment.CommentRepository;
import com.biojaveriana.api.post.Post;
import com.biojaveriana.api.post.PostRepository;
import com.biojaveriana.api.report.dto.ReportCreateRequest;
import com.biojaveriana.api.report.dto.ReportListResponse;
import com.biojaveriana.api.report.dto.ReportResponse;
import com.biojaveriana.api.report.exceptions.ReportAlreadyExistsException;
import com.biojaveriana.api.report.exceptions.ReportForbiddenException;
import com.biojaveriana.api.report.exceptions.ReportNotFoundException;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    private ReportService reportService;
    private User reporter;
    private User reported;
    private User admin;
    private Post post;
    private Comment comment;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(reportRepository, postRepository, commentRepository);
        reporter = User.builder().id(UUID.randomUUID()).username("reporter").firstName("Rep").lastName("Oter").role(Role.USER).build();
        reported = User.builder().id(UUID.randomUUID()).username("offender").firstName("Off").lastName("Ended").role(Role.USER).build();
        admin = User.builder().id(UUID.randomUUID()).username("admin").firstName("Ad").lastName("Min").role(Role.ADMIN).build();
        post = Post.builder().id(UUID.randomUUID()).author(reported).title("t").description("d").hidden(false).banned(false).build();
        comment = Comment.builder().id(UUID.randomUUID()).author(reported).content("c").hidden(false).banned(false).build();
    }

    private ReportCreateRequest postReport() {
        return ReportCreateRequest.builder()
            .reportType("POST")
            .reportedPostId(post.getId().toString())
            .reason("spam")
            .build();
    }

    private ReportCreateRequest commentReport() {
        return ReportCreateRequest.builder()
            .reportType("COMMENT")
            .reportedCommentId(comment.getId().toString())
            .reason("spam")
            .build();
    }

    private Report buildReport(UUID id) {
        return Report.builder()
            .id(id)
            .reporter(reporter)
            .reportedUser(reported)
            .reportType(ReportType.POST)
            .reason("spam")
            .status(ReportStatus.PENDING)
            .build();
    }


    @Nested
    @DisplayName("createReport")
    class CreateTests {

        @Test
        @DisplayName("a post report is created in PENDING state")
        void postReportCreated() {
            when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
            when(reportRepository.existsByReporterIdAndReportedPostIdAndStatus(
                reporter.getId(), post.getId(), ReportStatus.PENDING)).thenReturn(false);
            when(reportRepository.save(any(Report.class))).thenAnswer(inv -> {
                Report r = inv.getArgument(0);
                if (r.getId() == null) {
                    r.setId(UUID.randomUUID());
                }
                return r;
            });

            ReportResponse response = reportService.createReport(reporter, postReport());

            assertEquals(ReportType.POST, response.reportType());
            assertEquals(post.getId().toString(), response.reportedPostId());
            assertEquals(ReportStatus.PENDING, response.status());
            assertEquals(reporter.getUsername(), response.reporterUsername());
            assertEquals(reported.getUsername(), response.reportedUserUsername());
        }

        @Test
        @DisplayName("a duplicate pending post report is rejected")
        void duplicatePostReportRejected() {
            when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
            when(reportRepository.existsByReporterIdAndReportedPostIdAndStatus(
                reporter.getId(), post.getId(), ReportStatus.PENDING)).thenReturn(true);

            assertThrows(ReportAlreadyExistsException.class, () -> reportService.createReport(reporter, postReport()));

            verify(reportRepository, never()).save(any(Report.class));
        }

        @Test
        @DisplayName("reporting a missing post throws not found")
        void missingPostThrows() {
            when(postRepository.findById(post.getId())).thenReturn(Optional.empty());

            assertThrows(ReportNotFoundException.class, () -> reportService.createReport(reporter, postReport()));
        }

        @Test
        @DisplayName("reporting a hidden post throws not found")
        void hiddenPostThrows() {
            Post hidden = Post.builder().id(UUID.randomUUID()).author(reported).title("t").description("d").hidden(true).banned(false).build();
            when(postRepository.findById(hidden.getId())).thenReturn(Optional.of(hidden));

            assertThrows(ReportNotFoundException.class, () -> reportService.createReport(reporter,
                ReportCreateRequest.builder().reportType("POST").reportedPostId(hidden.getId().toString()).reason("spam").build()));
        }

        @Test
        @DisplayName("a comment report is created with the comment author as reported user")
        void commentReportCreated() {
            when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
            when(reportRepository.existsByReporterIdAndReportedCommentIdAndStatus(
                reporter.getId(), comment.getId(), ReportStatus.PENDING)).thenReturn(false);
            when(reportRepository.save(any(Report.class))).thenAnswer(inv -> {
                Report r = inv.getArgument(0);
                if (r.getId() == null) {
                    r.setId(UUID.randomUUID());
                }
                return r;
            });

            ReportResponse response = reportService.createReport(reporter, commentReport());

            assertEquals(ReportType.COMMENT, response.reportType());
            assertEquals(comment.getId().toString(), response.reportedCommentId());
        }

        @Test
        @DisplayName("a comment report without a comment id is invalid")
        void commentReportWithoutIdThrows() {
            assertThrows(IllegalArgumentException.class, () -> reportService.createReport(reporter,
                ReportCreateRequest.builder().reportType("COMMENT").reason("spam").build()));
        }

        @Test
        @DisplayName("an unknown report type is invalid")
        void invalidReportTypeThrows() {
            assertThrows(IllegalArgumentException.class, () -> reportService.createReport(reporter,
                ReportCreateRequest.builder().reportType("BAN").reportedPostId(post.getId().toString()).reason("spam").build()));
        }
    }


    @Nested
    @DisplayName("read reports")
    class ReadTests {

        @Test
        @DisplayName("getReport returns the stored report")
        void getReportReturnsReport() {
            Report report = buildReport(UUID.randomUUID());
            when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));

            ReportResponse response = reportService.getReport(report.getId());

            assertEquals(report.getId(), response.id());
        }

        @Test
        @DisplayName("getReport on a missing id throws not found")
        void getReportMissingThrows() {
            UUID id = UUID.randomUUID();
            when(reportRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(ReportNotFoundException.class, () -> reportService.getReport(id));
        }

        @Test
        @DisplayName("getReportsByReporter only includes the reporter's own reports")
        void getReportsByReporterFiltersByReporter() {
            Report mine = buildReport(UUID.randomUUID());
            Report theirs = buildReport(UUID.randomUUID());
            theirs.setReporter(User.builder().id(UUID.randomUUID()).username("someone").role(Role.USER).build());
            when(reportRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mine, theirs)));

            ReportListResponse response = reportService.getReportsByReporter(reporter, 0, 20);

            assertEquals(1, response.content().size());
            assertEquals(mine.getId(), response.content().get(0).id());
        }

        @Test
        @DisplayName("getPendingReportsForAdmin returns the pending page")
        void getPendingReportsForAdmin() {
            Report pending = buildReport(UUID.randomUUID());
            when(reportRepository.findByStatusOrderByCreatedAtDesc(eq(ReportStatus.PENDING), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pending)));

            ReportListResponse response = reportService.getPendingReportsForAdmin(0, 20);

            assertEquals(1, response.content().size());
            assertEquals(ReportStatus.PENDING, response.content().get(0).status());
        }
    }

    @Nested
    @DisplayName("updateReportStatus")
    class UpdateStatusTests {

        @Test
        @DisplayName("an admin can move a report to a new status")
        void adminCanUpdateStatus() {
            Report report = buildReport(UUID.randomUUID());
            when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
            when(reportRepository.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));

            ReportResponse response = reportService.updateReportStatus(report.getId(), admin, "resolved");

            assertEquals(ReportStatus.RESOLVED, response.status());
        }

        @Test
        @DisplayName("status parsing is case-insensitive")
        void adminCanUpdateStatusCaseInsensitive() {
            Report report = buildReport(UUID.randomUUID());
            when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
            when(reportRepository.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));

            ReportResponse response = reportService.updateReportStatus(report.getId(), admin, "UNDER_REVIEW");

            assertEquals(ReportStatus.UNDER_REVIEW, response.status());
        }

        @Test
        @DisplayName("a non-admin cannot update the status")
        void nonAdminCannotUpdateStatus() {
            Report report = buildReport(UUID.randomUUID());
            when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));

            assertThrows(ReportForbiddenException.class,
                () -> reportService.updateReportStatus(report.getId(), reporter, "resolved"));

            verify(reportRepository, never()).save(any(Report.class));
        }

        @Test
        @DisplayName("an invalid status string is rejected before any lookup")
        void invalidStatusThrows() {
            assertThrows(IllegalArgumentException.class,
                () -> reportService.updateReportStatus(UUID.randomUUID(), admin, "BOGUS"));

            verify(reportRepository, never()).findById(any(UUID.class));
        }

        @Test
        @DisplayName("updating a missing report throws not found")
        void updateMissingThrows() {
            UUID id = UUID.randomUUID();
            when(reportRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(ReportNotFoundException.class, () -> reportService.updateReportStatus(id, admin, "resolved"));
        }
    }
}
