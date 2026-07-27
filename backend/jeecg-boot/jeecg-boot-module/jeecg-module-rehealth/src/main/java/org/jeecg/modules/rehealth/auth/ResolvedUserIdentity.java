package org.jeecg.modules.rehealth.auth;

import java.util.Set;

public record ResolvedUserIdentity(String userId, Set<String> tenantIds) {
    public ResolvedUserIdentity {
        tenantIds = Set.copyOf(tenantIds);
    }
}
