package com.appointment.api.cache;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CacheNames {
    public static final String SERVICES = "services";
    public static final String SERVICE_BY_ID = "serviceById";
    public static final String BRANCH_SERVICES = "branchServices";
    public static final String BRANCHES = "branches";
}