package com.biojaveriana.api.security;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.biojaveriana.api.common.exceptions.NotAuthorizedException;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.UserService;
import com.biojaveriana.api.user.exceptions.UserDeactivatedException;
import com.biojaveriana.api.user.exceptions.UserNotFoundException;

@Component
@RequiredArgsConstructor
public class DeactivatedUserInterceptor implements HandlerInterceptor {
    private final UserService userService;

    @Override
    public boolean preHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler
    ) throws Exception {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return true;
        }

        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return true;
        }

        UUID userId = UUID.fromString(jwt.getSubject());
        User user;

        try {
            user = userService.findById(userId);
        } catch (UserNotFoundException e) {
            throw new NotAuthorizedException();
        }

        if (user.isDeactivated()) {
            throw new UserDeactivatedException();
        }

        return true;
    }
}
