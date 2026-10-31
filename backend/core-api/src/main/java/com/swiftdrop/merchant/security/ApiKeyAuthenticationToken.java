package com.swiftdrop.merchant.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final MerchantPrincipal principal;

    public ApiKeyAuthenticationToken(MerchantPrincipal principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public MerchantPrincipal getPrincipal() {
        return principal;
    }
}
