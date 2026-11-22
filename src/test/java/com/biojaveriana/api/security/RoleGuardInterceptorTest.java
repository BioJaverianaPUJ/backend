package com.biojaveriana.api.security;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import org.springframework.web.method.HandlerMethod;

import com.biojaveriana.api.common.exceptions.ForbiddenException;
import com.biojaveriana.api.common.exceptions.NotAuthorizedException;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.UserService;
import com.biojaveriana.api.user.exceptions.UserNotFoundException;

@ExtendWith(MockitoExtension.class)
class RoleGuardInterceptorTest {

    @Mock
    private UserService userService;

    private RoleGuardInterceptor interceptor;
    private UUID userId;

    static class MethodLevelFixture {
        @HasRole(Role.ADMIN)
        public void adminOnly() {
        }

        public void publicEndpoint() {
        }
    }

    @HasRole(Role.ADMIN)
    static class ClassLevelFixture {
        public void inheritedRequirement() {
        }
    }

    @BeforeEach
    void setUp() {
        interceptor = new RoleGuardInterceptor(userService);
        userId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User userWithRole(Role role) {
        return User.builder().id(userId).role(role).build();
    }

    private void authenticateJwt() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject(userId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(jwt, "N/A", List.of()));
    }

    private HandlerMethod methodHandler(String methodName) throws NoSuchMethodException {
        return new HandlerMethod(new MethodLevelFixture(),
            MethodLevelFixture.class.getDeclaredMethod(methodName));
    }

    private HandlerMethod classHandler() throws NoSuchMethodException {
        return new HandlerMethod(new ClassLevelFixture(),
            ClassLevelFixture.class.getDeclaredMethod("inheritedRequirement"));
    }

    @Test
    @DisplayName("preHandle: non-controller handlers pass through untouched")
    void nonHandlerMethodPassesThrough() throws Exception {
        assertTrue(interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), "static-resource"));
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("preHandle: endpoints without @HasRole pass through without a user lookup")
    void noRoleAnnotationPassesThrough() throws Exception {
        authenticateJwt();

        assertTrue(interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), methodHandler("publicEndpoint")));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("preHandle: a user holding the required role is allowed")
    void matchingRoleIsAllowed() throws Exception {
        authenticateJwt();
        when(userService.findById(userId)).thenReturn(userWithRole(Role.ADMIN));

        assertTrue(interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), methodHandler("adminOnly")));
    }

    @Test
    @DisplayName("preHandle: a user without the required role is forbidden")
    void missingRoleIsForbidden() throws Exception {
        authenticateJwt();
        when(userService.findById(userId)).thenReturn(userWithRole(Role.USER));

        assertThrows(ForbiddenException.class, () -> interceptor.preHandle(
            new MockHttpServletRequest(), new MockHttpServletResponse(), methodHandler("adminOnly")));
    }

    @Test
    @DisplayName("preHandle: superadmins bypass every role requirement")
    void superAdminBypassesRoleCheck() throws Exception {
        authenticateJwt();
        when(userService.findById(userId)).thenReturn(userWithRole(Role.SUPERADMIN));

        assertTrue(interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), methodHandler("adminOnly")));
    }

    @Test
    @DisplayName("preHandle: class-level @HasRole is honored")
    void classLevelAnnotationIsHonored() throws Exception {
        authenticateJwt();
        when(userService.findById(userId)).thenReturn(userWithRole(Role.ADMIN));

        assertTrue(interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), classHandler()));
    }

    @Test
    @DisplayName("preHandle: class-level @HasRole rejects non-matching roles")
    void classLevelAnnotationRejectsWrongRole() throws Exception {
        authenticateJwt();
        when(userService.findById(userId)).thenReturn(userWithRole(Role.USER));

        assertThrows(ForbiddenException.class, () -> interceptor.preHandle(
            new MockHttpServletRequest(), new MockHttpServletResponse(), classHandler()));
    }

    @Test
    @DisplayName("preHandle: missing authentication is rejected")
    void missingAuthenticationThrows() throws Exception {
        SecurityContextHolder.clearContext();

        assertThrows(NotAuthorizedException.class, () -> interceptor.preHandle(
            new MockHttpServletRequest(), new MockHttpServletResponse(), methodHandler("adminOnly")));
    }

    @Test
    @DisplayName("preHandle: a non-JWT principal is rejected")
    void nonJwtPrincipalThrows() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("someone", "N/A", List.of()));

        assertThrows(NotAuthorizedException.class, () -> interceptor.preHandle(
            new MockHttpServletRequest(), new MockHttpServletResponse(), methodHandler("adminOnly")));
    }

    @Test
    @DisplayName("preHandle: a JWT subject without a matching user is rejected as not authorized")
    void unknownUserThrowsNotAuthorized() throws Exception {
        authenticateJwt();
        when(userService.findById(userId)).thenThrow(new UserNotFoundException(userId));

        assertThrows(NotAuthorizedException.class, () -> interceptor.preHandle(
            new MockHttpServletRequest(), new MockHttpServletResponse(), methodHandler("adminOnly")));
    }
}

