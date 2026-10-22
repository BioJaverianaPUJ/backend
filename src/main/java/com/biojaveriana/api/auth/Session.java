package com.biojaveriana.api.auth;

import com.biojaveriana.api.user.User;

public record Session(
    User user,
    Token token
) {}
