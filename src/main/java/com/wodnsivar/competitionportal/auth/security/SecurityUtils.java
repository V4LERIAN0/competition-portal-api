package com.wodnsivar.competitionportal.auth.security;

import com.wodnsivar.competitionportal.common.exception.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserPrincipal getCurrentUserOrThrow() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new ForbiddenException("Authentication is required.");
        }

        return userPrincipal;
    }

    public static Long getCurrentUserIdOrThrow() {
        return getCurrentUserOrThrow().getId();
    }
}