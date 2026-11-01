package com.supplychain.security;

import com.supplychain.user.UserRole;

/** The security principal for a request. Controllers receive it through @AuthenticationPrincipal. */
public record AuthenticatedUser(long id, UserRole role) {}
