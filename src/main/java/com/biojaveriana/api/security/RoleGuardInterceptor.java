package com.biojaveriana.api.security;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.biojaveriana.api.common.exceptions.ForbiddenException;
import com.biojaveriana.api.auth.exceptions.UnauthorizedException;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.UserService;
import com.biojaveriana.api.user.exceptions.UserNotFoundException;
import com.biojaveriana.api.user.Role;

@Component
@RequiredArgsConstructor
public class RoleGuardInterceptor implements HandlerInterceptor {
    private final UserService userService;

    @Override
    public boolean preHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler
    ) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // We obtain the has role annotation (if there is one)
        HasRole hasRole = handlerMethod.getMethodAnnotation(HasRole.class);
        if (hasRole == null) {
            hasRole = handlerMethod.getBeanType().getAnnotation(HasRole.class);
        }

        if (hasRole == null) {
            return true;
        }

        // We get the current authenticated user
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException();
        }

        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new UnauthorizedException();
        }

        UUID userId = UUID.fromString(jwt.getSubject());
        User user;

        try {
            user = userService.findById(userId);
        } catch (UserNotFoundException e) {
            throw new UnauthorizedException();
        }

        // Verify role
        Role[] requiredRoles = hasRole.value();
        for (Role role : requiredRoles) {
            if (user.getRole() == role) {
                return true;
            }
        }

        throw new ForbiddenException();
    }
}
