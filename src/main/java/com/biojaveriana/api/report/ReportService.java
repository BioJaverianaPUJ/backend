package com.biojaveriana.api.report;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.biojaveriana.api.comment.Comment;
import com.biojaveriana.api.comment.CommentRepository;
import com.biojaveriana.api.common.Pagination;
import com.biojaveriana.api.post.Post;
import com.biojaveriana.api.post.PostRepository;
import com.biojaveriana.api.report.dto.ReportCreateRequest;
import com.biojaveriana.api.report.dto.ReportListResponse;
import com.biojaveriana.api.report.dto.ReportResponse;
import com.biojaveriana.api.report.dto.ReportSummaryResponse;
import com.biojaveriana.api.report.exceptions.ReportAlreadyExistsException;
import com.biojaveriana.api.report.exceptions.ReportForbiddenException;
import com.biojaveriana.api.report.exceptions.ReportNotFoundException;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public ReportResponse createReport(User reporter, ReportCreateRequest request) {
        ReportType reportType = parseReportType(request.getReportType());
        User reportedUser;

        if (reportType == ReportType.POST) {
            UUID postId = UUID.fromString(request.getReportedPostId());
            Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ReportNotFoundException(postId));
            if (Boolean.TRUE.equals(post.getHidden()) || Boolean.TRUE.equals(post.getBanned())) {
                throw new ReportNotFoundException(postId);
            }
            reportedUser = post.getAuthor();
            boolean alreadyReported = reportRepository
                .existsByReporterIdAndReportedPostIdAndStatus(reporter.getId(), postId, ReportStatus.PENDING);
            if (alreadyReported) {
                throw new ReportAlreadyExistsException();
            }
            var report = Report.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
                .reportedPost(post)
                .reportedComment(null)
                .reportType(ReportType.POST)
                .reason(request.getReason())
                .status(ReportStatus.PENDING)
                .build();
            report = reportRepository.save(report);
            log.info("Report created for post id={}", postId);
            return toResponse(report);

        } else if (reportType == ReportType.COMMENT) {
            if (request.getReportedCommentId() == null || request.getReportedCommentId().isEmpty()) {
                throw new IllegalArgumentException("Comment ID is required for COMMENT type");
            }
            UUID commentId = UUID.fromString(request.getReportedCommentId());
            Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ReportNotFoundException(commentId));
            if (Boolean.TRUE.equals(comment.getHidden()) || Boolean.TRUE.equals(comment.getBanned())) {
                throw new ReportNotFoundException(commentId);
            }
            reportedUser = comment.getAuthor();
            boolean alreadyReported = reportRepository
                .existsByReporterIdAndReportedCommentIdAndStatus(reporter.getId(), commentId, ReportStatus.PENDING);
            if (alreadyReported) {
                throw new ReportAlreadyExistsException();
            }
            var report = Report.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
                .reportedPost(null)
                .reportedComment(comment)
                .reportType(ReportType.COMMENT)
                .reason(request.getReason())
                .status(ReportStatus.PENDING)
                .build();
            report = reportRepository.save(report);
            log.info("Report created for comment id={}", commentId);
            return toResponse(report);
        }
        throw new IllegalArgumentException("Invalid report type: " + request.getReportType());
    }

    @Transactional
    public ReportResponse getReport(UUID reportId) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new ReportNotFoundException(reportId));
        return toResponse(report);
    }

    @Transactional(readOnly = true)
    public ReportListResponse getReportsByReporter(User reporter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Report> reportPage = reportRepository.findAllByOrderByCreatedAtDesc(pageable);
        java.util.List<Report> filtered = reportPage.getContent().stream()
            .filter(r -> r.getReporter().getId().equals(reporter.getId()))
            .toList();
        Page<Report> resultPage = new PageImpl<>(filtered, pageable, filtered.size());
        return toListResponse(resultPage);
    }

    @Transactional(readOnly = true)
    public ReportListResponse getReportsByReportedUser(UUID reportedUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Report> reportPage = reportRepository.findByReportedUserIdOrderByCreatedAtDesc(reportedUserId, pageable);
        return toListResponse(reportPage);
    }

    @Transactional(readOnly = true)
    public ReportListResponse getAllReportsForAdmin(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Report> reportPage = reportRepository.findAllByOrderByCreatedAtDesc(pageable);
        return toListResponse(reportPage);
    }

    @Transactional(readOnly = true)
    public ReportListResponse getPendingReportsForAdmin(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Report> reportPage = reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING, pageable);
        return toListResponse(reportPage);
    }

    @Transactional
    public ReportResponse updateReportStatus(UUID reportId, User admin, String newStatusString) {
        ReportStatus newStatus;
        try {
            newStatus = ReportStatus.valueOf(newStatusString.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid report status: " + newStatusString);
        }
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new ReportNotFoundException(reportId));
        if (admin != null && admin.getRole() != Role.ADMIN && admin.getRole() != Role.SUPERADMIN) {
            throw new ReportForbiddenException();
        }
        report.setStatus(newStatus);
        report = reportRepository.save(report);
        log.info("Report {} status updated to {} by admin", reportId, newStatus);
        return toResponse(report);
    }

    private ReportType parseReportType(String type) {
        try {
            return ReportType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid report type: " + type);
        }
    }

    private ReportResponse toResponse(Report report) {
        String reportedPostId = report.getReportedPost() != null ? report.getReportedPost().getId().toString() : null;
        String reportedCommentId = report.getReportedComment() != null ? report.getReportedComment().getId().toString() : null;
        return new ReportResponse(
            report.getId(),
            report.getReporter().getUsername(),
            report.getReporter().getFirstName() + " " + report.getReporter().getLastName(),
            report.getReportedUser().getUsername(),
            report.getReportedUser().getFirstName() + " " + report.getReportedUser().getLastName(),
            report.getReportType(),
            reportedPostId,
            reportedCommentId,
            report.getReason(),
            report.getStatus(),
            report.getCreatedAt(),
            report.getUpdatedAt()
        );
    }

    private ReportListResponse toListResponse(Page<Report> reportPage) {
        java.util.List<ReportSummaryResponse> content = reportPage.getContent().stream()
            .map(report -> new ReportSummaryResponse(
                report.getId(),
                report.getReportType(),
                report.getReportedUser().getUsername(),
                report.getReportedUser().getFirstName() + " " + report.getReportedUser().getLastName(),
                report.getReason(),
                report.getStatus(),
                report.getCreatedAt()
            ))
            .toList();
        return new ReportListResponse(content, Pagination.from(reportPage));
    }
}
