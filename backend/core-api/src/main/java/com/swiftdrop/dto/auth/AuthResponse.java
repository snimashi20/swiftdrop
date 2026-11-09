package com.swiftdrop.dto.auth;

import java.util.Set;

public record AuthResponse(
        String token,
        String tokenType,
        Long id,
        String email,
        String name,
        Set<String> roles
) {
}
