package com.biojaveriana.api.motd.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FunFactUpdateRequest {
    private String title;

    private String description;

    private UUID poiId;
}
