package com.biojaveriana.api.specimen.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SpecimenMatchingProperties.class)
public class SpecimenMatchingConfig {
    // Marker configuration class — all properties are defined in SpecimenMatchingProperties.
}
