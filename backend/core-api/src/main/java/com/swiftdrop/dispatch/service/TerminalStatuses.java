package com.swiftdrop.dispatch.service;

import com.swiftdrop.parcel.domain.ParcelStatus;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Statuses after which a parcel needs no more work. Looked up by name so this module
 * keeps compiling whether or not the CANCELLED status has been merged yet.
 */
public final class TerminalStatuses {

    private static final Set<String> NAMES = Set.of("DELIVERED", "FAILED", "CANCELLED");

    private TerminalStatuses() {
    }

    public static List<ParcelStatus> values() {
        return Arrays.stream(ParcelStatus.values())
                .filter(s -> NAMES.contains(s.name()))
                .toList();
    }

    public static boolean isTerminal(ParcelStatus status) {
        return status != null && NAMES.contains(status.name());
    }
}