package com.biojaveriana.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import com.biojaveriana.api.security.CurrentUserArgumentResolver;
import com.biojaveriana.api.security.DeactivatedUserInterceptor;
import com.biojaveriana.api.security.RoleGuardInterceptor;
import com.biojaveriana.api.user.BanService;
import com.biojaveriana.api.user.UserService;

/**
 * Provides the real security resolver/interceptors to {@code @WebMvcTest} slices.
 * The MVC slice does not instantiate plain {@code @Component} beans, but
 * {@code WebConfig} (a {@code WebMvcConfigurer}) depends on them, so they are
 * declared here. Their own dependencies ({@code UserService}, {@code BanService})
 * are supplied as {@code @MockitoBean} by each controller test.
 */
@TestConfiguration
public class SecuritySliceConfiguration {

    @Bean
    public CurrentUserArgumentResolver currentUserArgumentResolver(UserService userService) {
        return new CurrentUserArgumentResolver(userService);
    }

    @Bean
    public RoleGuardInterceptor roleGuardInterceptor(UserService userService) {
        return new RoleGuardInterceptor(userService);
    }

    @Bean
    public DeactivatedUserInterceptor deactivatedUserInterceptor(BanService banService) {
        return new DeactivatedUserInterceptor(banService);
    }
}
