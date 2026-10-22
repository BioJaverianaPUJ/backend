package com.biojaveriana.api.security;

import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.biojaveriana.api.auth.exceptions.UnauthorizedException;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.UserService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {
    private final UserService userService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        var hasAnnotation = parameter.hasParameterAnnotation(CurrentUser.class);
        var isUserType = parameter.getParameterType().equals(User.class);

        return hasAnnotation && isUserType;
    }

    @Override
    public @Nullable Object resolveArgument(
        MethodParameter parameter,
        @Nullable ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest,
        @Nullable WebDataBinderFactory binderFactory
    ) throws Exception {
        var authentication = SecurityContextHolder
            .getContext()
            .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException();
        }

        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new UnauthorizedException();
        }

        var userId = UUID.fromString(jwt.getSubject());
        var user = userService.findById(userId);
        if (user == null) {
            throw new UnauthorizedException();
        }

        return user;
    }
}
