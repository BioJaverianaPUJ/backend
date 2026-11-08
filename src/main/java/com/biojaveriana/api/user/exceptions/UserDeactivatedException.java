package com.biojaveriana.api.user.exceptions;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;
import com.biojaveriana.api.user.BanType;

public class UserDeactivatedException extends ServiceException {
    public UserDeactivatedException() {
        super(HttpStatus.FORBIDDEN, "USER_DEACTIVATED", "You have been banned from accessing this resource");
    }

    public UserDeactivatedException(BanType banType, Instant expiresAt) {
        super(
            HttpStatus.FORBIDDEN,
            "USER_DEACTIVATED",
            banType == BanType.PERMANENT
                ? "You have been permanently banned from accessing this resource"
                : "You have been temporarily banned from accessing this resource",
            Map.of(
                "banType", banType.name().toLowerCase(),
                "expiresAt", expiresAt
            )
        );
    }
}
