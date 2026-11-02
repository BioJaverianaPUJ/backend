package com.biojaveriana.api.observation;


import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.biojaveriana.api.s3.S3UrlGenerator;
import com.biojaveriana.api.security.CurrentUser;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.observation.dto.ObservationResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/observations")
@RequiredArgsConstructor
public class ObservationController {
    private final ObservationService observationService;
    private final S3UrlGenerator s3UrlGenerator;

    @PostMapping
    public ObservationResponse create(
        @CurrentUser User user,
        @RequestParam double latitude,
        @RequestParam double longitude,
        @RequestPart("file") MultipartFile file
    ) {
        var observation = observationService.createObservation(user, file, latitude, longitude);
        return ObservationResponse.from(observation, s3UrlGenerator);
    }
}
