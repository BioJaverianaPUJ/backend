package com.biojaveriana.api.observation;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.biojaveriana.api.s3.ObjectService;
import com.biojaveriana.api.s3.UploadPresign;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObservationImageUploader {
    private final ObjectService objectService;

    private static final String OBSERVATION_PREFIX = "observations/";

    public UploadPresign createPresign(String contentType) {
        String key = OBSERVATION_PREFIX + UUID.randomUUID() + "." + getFileExtension(contentType);
        return objectService.createPresign(key, contentType);
    }

    public String confirmUpload(String key) {
        if (!objectService.confirmUpload(key)) {
            throw new com.biojaveriana.api.s3.exceptions.UploadNotFoundException();
        }
        return objectService.generateUrl(key);
    }

    public void delete(String key) {
        objectService.delete(key);
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
