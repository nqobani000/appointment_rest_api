package com.appointment.api.cache;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CacheNames {
    public static final String SERVICES = "services";
    public static final String SERVICE_BY_ID = "serviceById";
}