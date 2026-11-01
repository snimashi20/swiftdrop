package com.swiftdrop.merchant.security;

import java.security.Principal;

public record MerchantPrincipal(Long merchantId) implements Principal {
    @Override
    public String getName() {
        return String.valueOf(merchantId);
    }
}
