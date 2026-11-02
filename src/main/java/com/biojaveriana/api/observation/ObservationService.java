package com.biojaveriana.api.observation;

import java.io.IOException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.biojaveriana.api.s3.ObjectService;
import com.biojaveriana.api.s3.exceptions.UnsupportedContentTypeException;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.observation.exceptions.InvalidObservationException;
import com.biojaveriana.api.observation.exceptions.ObservationUploadException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ObservationService {
    private final ObservationRepository observationRepository;
    private final ObjectService objectService;

    public Observation createObservation(User user, MultipartFile file, double latitude, double longitude) {
        if (file.isEmpty()) {
            throw new InvalidObservationException("Uploaded file is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new InvalidObservationException("Content type is null");
        }

        String key = generateKey(user, contentType);
        try {
            objectService.upload(key, file.getInputStream(), file.getSize(), contentType);
        } catch (IOException e) {
            throw new ObservationUploadException("Failed to upload observation image", e);
        }

        Observation observation = Observation.builder()
            .user(user)
            .referencePictureKey(key)
            .latitude(latitude)
            .longitude(longitude)
            .jobStatus(IdentificationJobStatus.QUEUED)
            .retries(0)
            .observationType(null)
            .speciesCommonName(null)
            .certainty(0.0)
            .specimen(null)
            .build();

        return observationRepository.save(observation);
    }

    private String generateKey(User user, String contentType) {
        String ext = contentTypeToExtension(contentType);
        return String.format("observations/%s/%s%s", user.getId(), UUID.randomUUID().toString(), ext);
    }

    private String contentTypeToExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            default -> throw new UnsupportedContentTypeException(contentType);
        };
    }
}
