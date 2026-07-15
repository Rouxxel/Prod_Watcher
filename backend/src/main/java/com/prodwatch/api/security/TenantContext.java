package com.prodwatch.api.security;

import java.util.UUID;

import com.prodwatch.api.error.BusinessRuleException;

public final class TenantContext {

    private TenantContext() {}

    public static UUID requireEcosystemId(CurrentUser user) {
        UUID ecosystemId = user.getEcosystemId();
        if (ecosystemId == null) {
            throw new BusinessRuleException("User has no ecosystem assigned");
        }
        return ecosystemId;
    }
}
