package com.biojaveriana.api.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
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
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.NativeWebRequest;

import com.biojaveriana.api.common.exceptions.NotAuthorizedException;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.UserService;

@ExtendWith(MockitoExtension.class)
class CurrentUserArgumentResolverTest {

    @Mock
    private UserService userService;

    private CurrentUserArgumentResolver resolver;
    private User user;
    private UUID userId;

    static class Fixture {
        void annotatedUserParameter(@CurrentUser User user) {
        }

        void plainUserParameter(User user) {
        }

        void annotatedNonUserParameter(@CurrentUser String name) {
        }
    }

    private MethodParameter parameterOf(String methodName, int index) {
        for (var method : Fixture.class.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return new MethodParameter(method, index);
            }
        }
        throw new IllegalStateException("fixture method not found: " + methodName);
    }

    @BeforeEach
    void setUp() {
        resolver = new CurrentUserArgumentResolver(userService);
        userId = UUID.randomUUID();
        user = User.builder().id(userId).username("jane").role(Role.USER).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, "N/A", List.of()));
    }

    @Test
    @DisplayName("supportsParameter: true only for @CurrentUser-annotated User parameters")
    void supportsParameterMatchesAnnotationAndType() {
        assertTrue(resolver.supportsParameter(parameterOf("annotatedUserParameter", 0)));
        assertFalse(resolver.supportsParameter(parameterOf("plainUserParameter", 0)));
        assertFalse(resolver.supportsParameter(parameterOf("annotatedNonUserParameter", 0)));
    }

    @Test
    @DisplayName("resolveArgument: loads the user referenced by the JWT subject")
    void resolveArgumentReturnsUserFromJwtSubject() throws Exception {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject(userId.toString()).build();
        authenticate(jwt);
        when(userService.findById(userId)).thenReturn(user);

        Object resolved = resolver.resolveArgument(
            parameterOf("annotatedUserParameter", 0), null, mock(NativeWebRequest.class), null);

        assertSame(user, resolved);
    }

    @Test
    @DisplayName("resolveArgument: no authentication in the context is rejected")
    void resolveArgumentWithoutAuthenticationThrows() throws Exception {
        SecurityContextHolder.clearContext();

        assertThrows(NotAuthorizedException.class, () -> resolver.resolveArgument(
            parameterOf("annotatedUserParameter", 0), null, mock(NativeWebRequest.class), null));
    }

    @Test
    @DisplayName("resolveArgument: an unauthenticated principal is rejected")
    void resolveArgumentWithUnauthenticatedTokenThrows() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.unauthenticated("anonymousUser", "N/A"));

        assertThrows(NotAuthorizedException.class, () -> resolver.resolveArgument(
            parameterOf("annotatedUserParameter", 0), null, mock(NativeWebRequest.class), null));
    }

    @Test
    @DisplayName("resolveArgument: a non-JWT principal is rejected")
    void resolveArgumentWithNonJwtPrincipalThrows() throws Exception {
        authenticate("not-a-jwt");

        assertThrows(NotAuthorizedException.class, () -> resolver.resolveArgument(
            parameterOf("annotatedUserParameter", 0), null, mock(NativeWebRequest.class), null));
    }

    @Test
    @DisplayName("resolveArgument: a JWT subject without a matching user is rejected")
    void resolveArgumentWithUnknownUserThrows() throws Exception {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject(userId.toString()).build();
        authenticate(jwt);
        when(userService.findById(userId)).thenReturn(null);

        assertThrows(NotAuthorizedException.class, () -> resolver.resolveArgument(
            parameterOf("annotatedUserParameter", 0), null, mock(NativeWebRequest.class), null));
    }
}
