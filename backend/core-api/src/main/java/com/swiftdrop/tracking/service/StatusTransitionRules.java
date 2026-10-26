package com.swiftdrop.tracking.service;

import com.swiftdrop.tracking.domain.ParcelStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import static com.swiftdrop.tracking.domain.ParcelStatus.CREATED;
import static com.swiftdrop.tracking.domain.ParcelStatus.DELIVERED;
import static com.swiftdrop.tracking.domain.ParcelStatus.FAILED;
import static com.swiftdrop.tracking.domain.ParcelStatus.IN_TRANSIT;
import static com.swiftdrop.tracking.domain.ParcelStatus.OUT_FOR_DELIVERY;
import static com.swiftdrop.tracking.domain.ParcelStatus.PICKED_UP;

@Component
public class StatusTransitionRules {

    private static final Map<ParcelStatus, Set<ParcelStatus>> ALLOWED =
            new EnumMap<>(ParcelStatus.class);

    static {
        ALLOWED.put(CREATED, Set.of(PICKED_UP));
        ALLOWED.put(PICKED_UP, Set.of(IN_TRANSIT, FAILED));
        ALLOWED.put(IN_TRANSIT, Set.of(OUT_FOR_DELIVERY, FAILED));
        ALLOWED.put(OUT_FOR_DELIVERY, Set.of(DELIVERED, FAILED));
        ALLOWED.put(FAILED, Set.of(OUT_FOR_DELIVERY));
        ALLOWED.put(DELIVERED, Set.of());
    }

    public boolean canTransition(ParcelStatus from, ParcelStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public void validate(ParcelStatus from, ParcelStatus to) {
        if (!canTransition(from, to)) {
            throw new InvalidStatusTransitionException(from, to);
        }
    }
}