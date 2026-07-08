package com.prodwatch.api.security;

import com.prodwatch.api.entity.AppRole;
import com.prodwatch.api.error.ForbiddenException;

public final class RoleChecker {

    private RoleChecker() {}

    public static void requireRole(CurrentUser user, AppRole... allowed) {
        if (user == null) {
            throw new ForbiddenException("Authentication required");
        }
        for (AppRole role : allowed) {
            if (user.getRole() == role) {
                return;
            }
        }
        throw new ForbiddenException("Insufficient permissions");
    }

    public static void requireAdmin(CurrentUser user) {
        requireRole(user, AppRole.admin);
    }
}
