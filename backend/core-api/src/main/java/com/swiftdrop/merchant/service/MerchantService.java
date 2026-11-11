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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

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
        user.setRole("ROLE_MERCHANT");
        user.setProvider("LOCAL");
        user.setEnabled(false);
        user.setStatus("PENDING_APPROVAL");

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

    @Transactional(readOnly = true)
    public Page<MerchantResponseDto> listForAdmin(MerchantStatus status, Pageable pageable) {
        Page<Merchant> merchants = status == null
                ? merchantRepository.findAll(pageable)
                : merchantRepository.findByStatus(status, pageable);
        return merchants.map(merchantMapper::toDto);
    }

    @Transactional
    public MerchantResponseDto approve(Long merchantId, Authentication authentication) {
        Merchant merchant = getPendingMerchant(merchantId);
        merchant.setStatus(MerchantStatus.APPROVED);
        merchant.setRejectionReason(null);
        setReviewFields(merchant, authentication);

        if (merchant.getUserId() != null) {
            userRepository.findById(merchant.getUserId()).ifPresent(user -> {
                user.setStatus("ACTIVE");
                user.setEnabled(true);
                userRepository.save(user);
            });
        }

        return merchantMapper.toDto(merchantRepository.save(merchant));
    }

    @Transactional
    public MerchantResponseDto reject(Long merchantId, String reason, Authentication authentication) {
        Merchant merchant = getPendingMerchant(merchantId);
        merchant.setStatus(MerchantStatus.REJECTED);
        merchant.setRejectionReason(reason.trim());
        setReviewFields(merchant, authentication);

        if (merchant.getUserId() != null) {
            userRepository.findById(merchant.getUserId()).ifPresent(user -> {
                user.setStatus("REJECTED");
                user.setEnabled(false);
                userRepository.save(user);
            });
        }

        return merchantMapper.toDto(merchantRepository.save(merchant));
    }

    private Merchant getPendingMerchant(Long merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new com.swiftdrop.common.exception.NotFoundException("Merchant not found"));
        if (merchant.getStatus() != MerchantStatus.PENDING) {
            throw new ConflictException("Only pending merchants can be reviewed");
        }
        return merchant;
    }

    private void setReviewFields(Merchant merchant, Authentication authentication) {
        User reviewer = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new com.swiftdrop.common.exception.NotFoundException("Admin user not found"));
        merchant.setReviewedAt(Instant.now());
        merchant.setReviewedBy(reviewer.getId());
    }
}
