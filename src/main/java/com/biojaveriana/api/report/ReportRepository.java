package com.biojaveriana.api.report;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, UUID> {
    List<Report> findByReporterIdAndStatus(UUID reporterId, ReportStatus status);

    boolean existsByReporterIdAndReportedPostIdAndStatus(UUID reporterId, UUID reportedPostId, ReportStatus status);

    boolean existsByReporterIdAndReportedCommentIdAndStatus(UUID reporterId, UUID reportedCommentId, ReportStatus status);

    @Query("SELECT r FROM Report r WHERE r.reportedUser.id = :reportedUserId ORDER BY r.createdAt DESC")
    Page<Report> findByReportedUserIdOrderByCreatedAtDesc(@Param("reportedUserId") UUID reportedUserId, Pageable pageable);

    @Query("SELECT r FROM Report r WHERE r.status = :status ORDER BY r.createdAt DESC")
    Page<Report> findByStatusOrderByCreatedAtDesc(@Param("status") ReportStatus status, Pageable pageable);

    @Query("SELECT r FROM Report r WHERE r.status = :status ORDER BY r.createdAt DESC")
    Page<Report> findAllByStatusOrderByCreatedAtDesc(@Param("status") ReportStatus status, Pageable pageable);

    @Query("SELECT r FROM Report r ORDER BY r.createdAt DESC")
    Page<Report> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT r FROM Report r WHERE r.reporter.id = :reporterId AND r.reportedPost.id = :reportedPostId AND r.status = :status")
    Optional<Report> findByReporterIdAndReportedPostIdAndStatus(@Param("reporterId") UUID reporterId, @Param("reportedPostId") UUID reportedPostId, @Param("status") ReportStatus status);

    @Query("SELECT r FROM Report r WHERE r.reporter.id = :reporterId AND r.reportedComment.id = :reportedCommentId AND r.status = :status")
    Optional<Report> findByReporterIdAndReportedCommentIdAndStatus(@Param("reporterId") UUID reporterId, @Param("reportedCommentId") UUID reportedCommentId, @Param("status") ReportStatus status);
}
