package com.biojaveriana.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = SECURITY_SCHEME_NAME;

        return new OpenAPI()
            .info(new Info()
                .title("BioJaveriana API")
                .description("RESTful API for the BioJaveriana biodiversity observation platform. " +
                    "This API provides endpoints for user management, biodiversity observations, " +
                    "species and specimen tracking, posts and comments, points of interest, " +
                    "and content moderation.")
                .version("1.0.0")
                .contact(new Contact()
                    .name("BioJaveriana Team")
                    .email("contact@biojaveriana.com"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT token obtained from the /api/v1/auth/login endpoint. " +
                            "Format: Bearer {token}")));
    }
}
