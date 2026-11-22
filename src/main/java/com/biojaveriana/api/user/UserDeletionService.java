package com.biojaveriana.api.user;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.biojaveriana.api.comment.CommentReactionRepository;
import com.biojaveriana.api.comment.CommentRepository;
import com.biojaveriana.api.gamification.repository.UserAchievementRepository;
import com.biojaveriana.api.gamification.repository.UserGamificationRepository;
import com.biojaveriana.api.gamification.repository.XpLogRepository;
import com.biojaveriana.api.observation.ObservationJobRepository;
import com.biojaveriana.api.observation.ObservationRepository;
import com.biojaveriana.api.poi.PoiRepository;
import com.biojaveriana.api.poi.PoiTelemetryRepository;
import com.biojaveriana.api.post.PostImageRepository;
import com.biojaveriana.api.post.PostRepository;
import com.biojaveriana.api.post.PostVoteRepository;
import com.biojaveriana.api.report.ReportRepository;
import com.biojaveriana.api.s3.ObjectService;
import com.biojaveriana.api.user.exceptions.SuperAdminDeletionException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Fully deletes a user account and every piece of data related to that user,
 * in a foreign-key-safe order. Also removes the user's files from object storage.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserDeletionService {

    private static final String SUPERADMIN_USERNAME = "superadmin";

    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final CommentReactionRepository commentReactionRepository;
    private final ReportRepository reportRepository;
    private final BanRepository banRepository;
    private final PoiRepository poiRepository;
    private final PoiTelemetryRepository poiTelemetryRepository;
    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostVoteRepository postVoteRepository;
    private final ObservationRepository observationRepository;
    private final ObservationJobRepository observationJobRepository;
    private final XpLogRepository xpLogRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserGamificationRepository userGamificationRepository;
    private final ObjectService objectService;

    public void deleteAccount(User user) {
        if (SUPERADMIN_USERNAME.equals(user.getUsername())) {
            throw new SuperAdminDeletionException();
        }

        UUID userId = user.getId();

        List<UUID> commentIds = commentRepository.findIdsByAuthorId(userId);
        List<UUID> poiIds = poiRepository.findIdsByCreatorId(userId);
        List<UUID> postIds = postRepository.findIdsByAuthorId(userId);
        List<UUID> observationIds = observationRepository.findIdsByUserId(userId);

        deleteObjectStorageFiles(user, userId, postIds);

        // Delete children before parents so that foreign key constraints are satisfied.
        commentReactionRepository.deleteByUserId(userId);
        if (!commentIds.isEmpty()) {
            commentReactionRepository.deleteByCommentIdIn(commentIds);
            commentRepository.deleteByParentIdIn(commentIds);
        }
        commentRepository.deleteByAuthorId(userId);
        reportRepository.deleteByReporterIdOrReportedUserId(userId);
        banRepository.deleteByUserId(userId);
        banRepository.clearCreatedBy(userId);
        poiTelemetryRepository.deleteByUserId(userId);
        if (!poiIds.isEmpty()) {
            poiTelemetryRepository.deleteByPoiIdIn(poiIds);
        }
        poiRepository.deleteByCreatorId(userId);
        postVoteRepository.deleteByVoterId(userId);
        if (!postIds.isEmpty()) {
            postVoteRepository.deleteByPostIdIn(postIds);
            postImageRepository.deleteByPostIdIn(postIds);
        }
        postRepository.deleteByAuthorId(userId);
        if (!observationIds.isEmpty()) {
            observationJobRepository.deleteByObservationIdIn(observationIds);
        }
        observationRepository.deleteByUserId(userId);
        xpLogRepository.deleteByUserId(userId);
        userAchievementRepository.deleteByUserId(userId);
        userGamificationRepository.deleteByUserId(userId);

        userRepository.delete(user);

        log.info("Deleted user account {} and all associated data", userId);
    }

    private void deleteObjectStorageFiles(User user, UUID userId, List<UUID> postIds) {
        List<String> s3Keys = new ArrayList<>();

        if (user.getProfilePictureKey() != null) {
            s3Keys.add(user.getProfilePictureKey());
        }
        if (user.getBannerKey() != null) {
            s3Keys.add(user.getBannerKey());
        }
        s3Keys.addAll(observationRepository.findReferencePictureKeysByUserId(userId));
        if (!postIds.isEmpty()) {
            s3Keys.addAll(postImageRepository.findS3KeysByPostIdIn(postIds));
        }

        s3Keys.forEach(objectService::delete);
    }
}
