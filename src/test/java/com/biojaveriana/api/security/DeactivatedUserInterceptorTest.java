package com.biojaveriana.api.security;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import com.biojaveriana.api.common.exceptions.NotAuthorizedException;
import com.biojaveriana.api.user.BanService;
import com.biojaveriana.api.user.exceptions.UserDeactivatedException;
import com.biojaveriana.api.user.exceptions.UserNotFoundException;

@ExtendWith(MockitoExtension.class)
class DeactivatedUserInterceptorTest {

    @Mock
    private BanService banService;

    private DeactivatedUserInterceptor interceptor;
    private UUID userId;

    @BeforeEach
    void setUp() {
        interceptor = new DeactivatedUserInterceptor(banService);
        userId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateJwt() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject(userId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(jwt, "N/A", List.of()));
    }

    @Test
    @DisplayName("preHandle: unauthenticated requests pass through without a ban check")
    void unauthenticatedPassesThrough() throws Exception {
        SecurityContextHolder.clearContext();

        assertTrue(interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), "handler"));

        verifyNoInteractions(banService);
    }

    @Test
    @DisplayName("preHandle: non-JWT principals pass through without a ban check")
    void nonJwtPrincipalPassesThrough() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("someone", "N/A", List.of()));

        assertTrue(interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), "handler"));

        verifyNoInteractions(banService);
    }

    @Test
    @DisplayName("preHandle: an authenticated user without an active ban passes through")
    void noActiveBanPassesThrough() throws Exception {
        authenticateJwt();

        assertTrue(interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), "handler"));

        verify(banService).checkActiveBan(userId);
    }

    @Test
    @DisplayName("preHandle: an active ban surfaces as UserDeactivatedException")
    void activeBanThrowsDeactivated() throws Exception {
        authenticateJwt();
        doThrow(new UserDeactivatedException()).when(banService).checkActiveBan(userId);

        assertThrows(UserDeactivatedException.class,
            () -> interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), "handler"));
    }

    @Test
    @DisplayName("preHandle: a deleted user surfaces as NotAuthorizedException")
    void deletedUserThrowsNotAuthorized() throws Exception {
        authenticateJwt();
        doThrow(new UserNotFoundException(userId)).when(banService).checkActiveBan(userId);

        assertThrows(NotAuthorizedException.class,
            () -> interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), "handler"));
    }
}
