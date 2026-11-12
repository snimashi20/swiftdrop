package com.swiftdrop.auth.domain;

import java.util.Set;

public enum Role {
    ROLE_CUSTOMER,
    ROLE_MERCHANT,
    ROLE_COURIER,
    ROLE_DISPATCHER,
    ROLE_ADMIN;

    public static final String CUSTOMER = "ROLE_CUSTOMER";
    public static final String MERCHANT = "ROLE_MERCHANT";
    public static final String COURIER = "ROLE_COURIER";
    public static final String DISPATCHER = "ROLE_DISPATCHER";
    public static final String ADMIN = "ROLE_ADMIN";

    private static final Set<Role> SELF_REGISTERABLE = Set.of(
            ROLE_CUSTOMER,
            ROLE_MERCHANT,
            ROLE_COURIER
    );

    private static final Set<Role> APPROVAL_REQUIRED = Set.of(
            ROLE_MERCHANT,
            ROLE_COURIER
    );

    /**
     * Resolves a role string (e.g. "CUSTOMER", "ROLE_CUSTOMER", "merchant") to a Role enum.
     * Returns null if unparseable.
     */
    public static Role fromString(String roleStr) {
        if (roleStr == null || roleStr.isBlank()) {
            return null;
        }
        String clean = roleStr.trim().toUpperCase();
        if (!clean.startsWith("ROLE_")) {
            clean = "ROLE_" + clean;
        }
        try {
            return Role.valueOf(clean);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Checks if the given role is allowed for self-registration (CUSTOMER, MERCHANT, COURIER).
     */
    public static boolean isSelfRegisterable(String roleStr) {
        Role role = fromString(roleStr);
        return role != null && SELF_REGISTERABLE.contains(role);
    }

    /**
     * Checks if the given role requires administrator approval (MERCHANT, COURIER).
     */
    public static boolean requiresApproval(String roleStr) {
        Role role = fromString(roleStr);
        return role != null && APPROVAL_REQUIRED.contains(role);
    }

    /**
     * Checks if the given role requires administrator approval.
     */
    public static boolean requiresApproval(Role role) {
        return role != null && APPROVAL_REQUIRED.contains(role);
    }

    /**
     * Normalizes a requested role string to its enum name if self-registerable,
     * otherwise returns the provided default role.
     */
    public static String normalizeOrDefault(String roleStr, String defaultRole) {
        Role role = fromString(roleStr);
        if (role != null && SELF_REGISTERABLE.contains(role)) {
            return role.name();
        }
        return defaultRole;
    }
}

