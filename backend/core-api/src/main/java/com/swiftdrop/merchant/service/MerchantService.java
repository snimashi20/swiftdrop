package com.swiftdrop.merchant.service;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.common.exception.ConflictException;
import com.swiftdrop.merchant.domain.Merchant;
import com.swiftdrop.merchant.domain.MerchantStatus;
import com.swiftdrop.merchant.dto.MerchantMapper;
import com.swiftdrop.merchant.dto.MerchantResponseDto;
import com.swiftdrop.merchant.dto.RegisterMerchantRequest;
import com.swiftdrop.merchant.repository.MerchantRepository;
import com.swiftdrop.merchant.security.CurrentMerchantProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantService {

    private final UserRepository userRepository;
    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;
    private final MerchantMapper merchantMapper;
    private final CurrentMerchantProvider currentMerchantProvider;

    @Transactional
    public MerchantResponseDto register(RegisterMerchantRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String normalizedRegNo = request.getRegistrationNumber().trim();

        if (userRepository.findByEmail(normalizedEmail).isPresent()
                || merchantRepository.existsByContactEmail(normalizedEmail)) {
            throw new ConflictException("Email already in use");
        }

        if (merchantRepository.existsByRegistrationNumber(normalizedRegNo)) {
            throw new ConflictException("Business registration number already registered");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getContactName().trim());
        user.setPhoneNumber(request.getContactPhone().trim());
        user.setRole("MERCHANT");
        user.setProvider("LOCAL");
        user.setEnabled(true);

        User savedUser = userRepository.save(user);

        Merchant merchant = Merchant.builder()
                .userId(savedUser.getId())
                .businessName(request.getBusinessName().trim())
                .registrationNumber(normalizedRegNo)
                .contactName(request.getContactName().trim())
                .contactEmail(normalizedEmail)
                .contactPhone(request.getContactPhone().trim())
                .status(MerchantStatus.PENDING)
                .build();

        Merchant savedMerchant = merchantRepository.save(merchant);
        log.info("Registered new merchant account with id: {}", savedMerchant.getId());

        return merchantMapper.toDto(savedMerchant);
    }

    @Transactional(readOnly = true)
    public MerchantResponseDto getMyProfile(Authentication authentication) {
        Merchant merchant = currentMerchantProvider.getCurrentMerchant(authentication);
        return merchantMapper.toDto(merchant);
    }
}
