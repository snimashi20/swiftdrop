package com.swiftdrop.security;

import java.security.Principal;

public record AuthenticatedUser(Long id, String email, String role) implements Principal {

    @Override
    public String getName() {
        return email;
    }
}
