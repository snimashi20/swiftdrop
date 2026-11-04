package com.swiftdrop.merchant.security;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.common.exception.ForbiddenException;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.merchant.domain.Merchant;
import com.swiftdrop.merchant.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentMerchantProvider {

    private final UserRepository userRepository;
    private final MerchantRepository merchantRepository;

    public Merchant getCurrentMerchant(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ForbiddenException("Authentication required");
        }

        if (authentication.getPrincipal() instanceof MerchantPrincipal principal) {
            return merchantRepository.findById(principal.merchantId())
                    .orElseThrow(() -> new NotFoundException("Merchant profile not found"));
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return merchantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException("Merchant profile not found for user"));
    }
}
