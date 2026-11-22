package com.biojaveriana.api.motd;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.biojaveriana.api.common.Pagination;
import com.biojaveriana.api.motd.dto.FunFactCreateRequest;
import com.biojaveriana.api.motd.dto.FunFactListResponse;
import com.biojaveriana.api.motd.dto.FunFactResponse;
import com.biojaveriana.api.motd.dto.FunFactSummaryResponse;
import com.biojaveriana.api.motd.dto.FunFactUpdateRequest;
import com.biojaveriana.api.motd.dto.UserSummaryResponse;
import com.biojaveriana.api.motd.exceptions.FunFactImageLimitException;
import com.biojaveriana.api.motd.exceptions.FunFactNotFoundException;
import com.biojaveriana.api.poi.Poi;
import com.biojaveriana.api.poi.PoiRepository;
import com.biojaveriana.api.poi.exceptions.PoiNotFoundException;
import com.biojaveriana.api.s3.ObjectService;
import com.biojaveriana.api.s3.S3UrlGenerator;
import com.biojaveriana.api.s3.UploadPresign;
import com.biojaveriana.api.user.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FunFactService {
    private static final int MAX_IMAGES = 10;
    private static final String FUN_FACTS_PREFIX = "funfacts/";

    private final FunFactRepository funFactRepository;
    private final FunFactImageRepository funFactImageRepository;
    private final MotdSelectionRepository motdSelectionRepository;
    private final PoiRepository poiRepository;
    private final S3UrlGenerator s3UrlGenerator;
    private final ObjectService objectService;

    @Transactional
    public UploadPresign createPresign(User user, String contentType) {
        String key = FUN_FACTS_PREFIX + user.getId() + "/" + UUID.randomUUID() + "." + getFileExtension(contentType);
        return objectService.createPresign(key, contentType);
    }

    @Transactional
    public FunFactResponse create(User user, FunFactCreateRequest request) {
        if (request.getImageKeys() != null && request.getImageKeys().length > MAX_IMAGES) {
            throw new FunFactImageLimitException(MAX_IMAGES);
        }

        final Instant now = Instant.now();
        var funFact = FunFact.builder()
            .author(user)
            .title(request.getTitle())
            .description(request.getDescription())
            .poi(request.getPoiId() != null
                ? resolvePoi(request.getPoiId())
                : null)
            .hidden(false)
            .createdAt(now)
            .updatedAt(now)
            .build();

        funFact = funFactRepository.save(funFact);

        if (request.getImageKeys() != null) {
            List<FunFactImage> images = new ArrayList<>();
            for (int i = 0; i < request.getImageKeys().length; i++) {
                var image = FunFactImage.builder()
                    .funFact(funFact)
                    .s3Key(request.getImageKeys()[i])
                    .orderIndex(i)
                    .build();
                images.add(image);
            }
            funFactImageRepository.saveAll(images);
        }

        log.info("Fun fact created id={} by user id={}", funFact.getId(), user.getId());
        return FunFactResponse.from(funFact, s3UrlGenerator);
    }

    @Transactional
    public FunFactResponse update(UUID funFactId, User user, FunFactUpdateRequest request) {
        var funFact = funFactRepository.findById(funFactId)
            .orElseThrow(() -> new FunFactNotFoundException(funFactId));

        if (request.getTitle() != null) {
            funFact.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            funFact.setDescription(request.getDescription());
        }
        if (request.getPoiId() != null) {
            funFact.setPoi(resolvePoi(request.getPoiId()));
        }

        funFact = funFactRepository.save(funFact);
        log.info("Fun fact updated id={} by user id={}", funFactId, user.getId());
        return FunFactResponse.from(funFact, s3UrlGenerator);
    }

    @Transactional
    public void delete(UUID funFactId, User user) {
        var funFact = funFactRepository.findById(funFactId)
            .orElseThrow(() -> new FunFactNotFoundException(funFactId));

        motdSelectionRepository.deleteByFunFactId(funFactId);
        funFactImageRepository.deleteByFunFactId(funFactId);
        funFactRepository.delete(funFact);
        log.info("Fun fact deleted id={} by user id={}", funFactId, user.getId());
    }

    @Transactional(readOnly = true)
    public FunFactResponse getById(UUID funFactId) {
        var funFact = funFactRepository.findById(funFactId)
            .orElseThrow(() -> new FunFactNotFoundException(funFactId));
        return FunFactResponse.from(funFact, s3UrlGenerator);
    }

    @Transactional(readOnly = true)
    public FunFactListResponse list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<FunFact> funFactPage = funFactRepository.findAllByOrderByCreatedAtDesc(pageable);
        return toListResponse(funFactPage);
    }

    @Transactional
    public FunFactResponse toggleHidden(UUID funFactId, User user) {
        var funFact = funFactRepository.findById(funFactId)
            .orElseThrow(() -> new FunFactNotFoundException(funFactId));
        funFact.setHidden(!funFact.getHidden());
        funFact = funFactRepository.save(funFact);
        log.info("Fun fact {} hidden status toggled to {} by admin user id={}", funFactId, funFact.getHidden(), user.getId());
        return FunFactResponse.from(funFact, s3UrlGenerator);
    }

    private FunFactListResponse toListResponse(Page<FunFact> funFactPage) {
        List<FunFactSummaryResponse> content = funFactPage.getContent().stream()
            .map(funFact -> {
                String firstImageUrl = null;
                if (funFact.getImages() != null && !funFact.getImages().isEmpty()) {
                    var firstImage = funFact.getImages().stream()
                        .min(Comparator.comparingInt(image -> image.getOrderIndex()))
                        .orElse(null);
                    if (firstImage != null) {
                        firstImageUrl = s3UrlGenerator.generateUrl(firstImage.getS3Key());
                    }
                }
                return new FunFactSummaryResponse(
                    funFact.getId(),
                    new UserSummaryResponse(
                        funFact.getAuthor().getId(),
                        funFact.getAuthor().getUsername(),
                        funFact.getAuthor().getFirstName(),
                        funFact.getAuthor().getLastName(),
                        funFact.getAuthor().getProfilePictureKey() != null
                            ? s3UrlGenerator.generateUrl(funFact.getAuthor().getProfilePictureKey())
                            : null),
                    funFact.getTitle(),
                    firstImageUrl,
                    funFact.getCreatedAt()
                );
            })
            .toList();

        return new FunFactListResponse(
            content,
            Pagination.from(funFactPage)
        );
    }

    private Poi resolvePoi(UUID poiId) {
        return poiRepository.findById(poiId)
            .orElseThrow(() -> new PoiNotFoundException(poiId));
    }

    private String getFileExtension(String contentType) {
        return switch (contentType.toLowerCase()) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/tiff" -> "tiff";
            default -> "jpg";
        };
    }
}

