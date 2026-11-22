package com.biojaveriana.api.config;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * OpenAPI 3 configuration for the Bio Javeriana API (served by springdoc).
 *
 * <p>The OpenAPI description is available at {@code /v3/api-docs} (JSON) and
 * {@code /v3/api-docs.yaml}, and the interactive Swagger UI at
 * {@code /swagger-ui.html}. Several grouped views (see the {@link GroupedOpenApi}
 * beans below) are exposed at {@code /v3/api-docs/{group}} and can be selected
 * in the Swagger UI to focus on a single domain.
 *
 * <p>Authentication is a stateless JWT (HS256) carried as an HTTP Bearer token.
 * The scheme is applied globally; the few public endpoints override it with an
 * empty {@code security} declaration on their {@code @Operation}.
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI bioJaverianaOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Bio Javeriana API")
                .version("v1")
                .description("""
                    REST API for the Bio Javeriana community platform: posts, comments, \
                    species observations, geographic POIs, message-of-the-day fun facts, \
                    content reporting, moderation and a gamification layer (XP, missions, \
                    achievements).

                    ## Authentication
                    - Obtain a JWT by calling `POST /api/v1/auth/login` or `POST /api/v1/auth/register`
                      (a new token can also be obtained with `POST /api/v1/auth/refresh-token`).
                    - Send it as `Authorization: Bearer <token>` on every protected endpoint.
                    - Use the **Authorize** button in this UI to set the token once for the whole session.
                    - Requests from deactivated or banned users are rejected even with a valid token.

                    ## Roles
                    - `USER` — default account, can use community features.
                    - `ADMIN` — adds moderation and content-management endpoints (marked *Admin* in the docs).
                    - `SUPERADMIN` — full access; bypasses every role check.

                    ## Errors
                    All errors share the `ErrorResponse` schema: `{ status, code, message, path, timestamp, details }`.
                    Common codes: `400` validation/illegal argument, `401` missing or invalid token,
                    `403` insufficient permissions, `404` unknown resource, `500` unexpected error.
                    """))
            .components(new Components()
                .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                    .name(BEARER_AUTH)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT access token issued by /api/v1/auth/login or /api/v1/auth/register")))
            .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    /**
     * Authentication, user accounts and bans.
     */
    @Bean
    public GroupedOpenApi coreApi() {
        return GroupedOpenApi.builder()
            .group("core")
            .displayName("Core — auth, users, bans")
            .pathsToMatch("/api/v1/auth/**", "/api/v1/users/**", "/api/v1/bans/**")
            .build();
    }

    /**
     * Community content: posts, comments, reports, MOTD fun facts and POIs.
     */
    @Bean
    public GroupedOpenApi communityApi() {
        return GroupedOpenApi.builder()
            .group("community")
            .displayName("Community — posts, comments, reports, MOTD, POIs")
            .pathsToMatch(
                "/api/v1/posts/**",
                "/api/v1/comments/**",
                "/api/v1/reports/**",
                "/api/v1/motd/**",
                "/api/v1/pois/**"
            )
            .build();
    }

    /**
     * Field biology: observations and the species/specimen reference data.
     */
    @Bean
    public GroupedOpenApi fieldApi() {
        return GroupedOpenApi.builder()
            .group("field")
            .displayName("Field — observations, species, specimens")
            .pathsToMatch(
                "/api/v1/observations/**",
                "/api/v1/species/**",
                "/api/v1/specimens/**"
            )
            .build();
    }

    /**
     * Gamification layer: XP summaries and history, missions and achievements.
     */
    @Bean
    public GroupedOpenApi gamificationApi() {
        return GroupedOpenApi.builder()
            .group("gamification")
            .displayName("Gamification — XP, missions, achievements")
            .pathsToMatch(
                "/api/v1/gamification/**",
                "/api/v1/missions/**",
                "/api/v1/achievements/**"
            )
            .build();
    }

    /**
     * API endpoints that do not require a token, mirroring the {@code permitAll}
     * rules in {@link SecurityConfig} (everything under {@code /api/v1/auth/**}
     * except {@code /api/v1/auth/refresh-token}).
     *
     * <p>springdoc drops an empty {@code @Operation(security = {})} override, so
     * without this the global {@code bearerAuth} requirement would incorrectly
     * appear on public endpoints in the Swagger UI. Setting an explicit empty
     * security list renders {@code "security": []} on the operation, which the
     * OpenAPI 3.1 spec defines as "no security".
     */
    private static final Set<String> PUBLIC_ENDPOINTS = Set.of(
        "/api/v1/auth/login",
        "/api/v1/auth/register"
    );

    @Bean
    public OperationCustomizer publicEndpointSecurityCustomizer() {
        return (operation, handlerMethod) -> {
            String path = mappingPath(handlerMethod);
            if (path != null && PUBLIC_ENDPOINTS.contains(path)) {
                operation.setSecurity(new ArrayList<>());
            }
            return operation;
        };
    }

    /**
     * Combines the class-level and method-level {@link RequestMapping} path
     * values (or the meta-annotated {@code @GetMapping}/{@code @PostMapping}/...)
     * into the full request path, e.g. {@code /api/v1/auth/login}.
     */
    private static String mappingPath(HandlerMethod handlerMethod) {
        Method method = handlerMethod.getMethod();
        RequestMapping methodMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        if (methodMapping == null) {
            return null;
        }
        RequestMapping classMapping =
                AnnotatedElementUtils.findMergedAnnotation(method.getDeclaringClass(), RequestMapping.class);
        String basePath = classMapping == null ? "" : firstPathElement(classMapping.path(), classMapping.value());
        String subPath = firstPathElement(methodMapping.path(), methodMapping.value());
        String path = (basePath + "/" + subPath).replaceAll("/+", "/");
        return path.startsWith("/") ? path : "/" + path;
    }

    private static String firstPathElement(String[]... candidates) {
        for (String[] array : candidates) {
            for (String candidate : array) {
                if (candidate != null && !candidate.isEmpty()) {
                    return candidate;
                }
            }
        }
        return "";
    }
}
