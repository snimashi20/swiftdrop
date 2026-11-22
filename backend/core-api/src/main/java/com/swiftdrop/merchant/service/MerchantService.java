package com.swiftdrop.merchant.service;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.common.exception.ConflictException;
import com.swiftdrop.merchant.domain.Merchant;
import com.swiftdrop.merchant.domain.MerchantStatus;
import com.swiftdrop.merchant.dto.MerchantMapper;
import com.swiftdrop.merchant.dto.MerchantResponseDto;
import com.swiftdrop.merchant.dto.RegisterMerchantRequest;
import com.swiftdrop.merchant.dto.UpdateMerchantProfileRequest;
import com.swiftdrop.merchant.repository.MerchantRepository;
import com.swiftdrop.merchant.security.CurrentMerchantProvider;
import com.swiftdrop.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantService {

    private final UserRepository userRepository;
    private final MerchantRepository merchantRepository;
    private final AuthService authService;
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

        User savedUser = authService.createLocalUser(
                normalizedEmail,
                request.getPassword(),
                request.getContactName().trim(),
                request.getContactPhone().trim(),
                "MERCHANT");

        Merchant merchant = Merchant.builder()
                .userId(savedUser.getId())
                .businessName(request.getBusinessName().trim())
                .businessType(request.getBusinessType())
                .registrationNumber(normalizedRegNo)
                .businessAddress(request.getBusinessAddress().trim())
                .businessDescription(request.getBusinessDescription().trim())
                .verificationDeclarationAccepted(request.isVerificationDeclarationAccepted())
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

    @Transactional
    public MerchantResponseDto updateMyProfile(UpdateMerchantProfileRequest request, Authentication authentication) {
        Merchant merchant = currentMerchantProvider.getCurrentMerchant(authentication);
        String businessName = request.getBusinessName().trim();
        String businessType = request.getBusinessType();
        String registrationNumber = request.getRegistrationNumber().trim();
        String businessAddress = request.getBusinessAddress().trim();
        String businessDescription = request.getBusinessDescription().trim();
        String contactName = request.getContactName().trim();
        String contactPhone = request.getContactPhone().trim();
        boolean declarationAccepted = request.isVerificationDeclarationAccepted();

        boolean verificationDetailsChanged = !Objects.equals(merchant.getBusinessName(), businessName)
                || !Objects.equals(merchant.getBusinessType(), businessType)
                || !Objects.equals(merchant.getRegistrationNumber(), registrationNumber)
                || !Objects.equals(merchant.getBusinessAddress(), businessAddress)
                || !Objects.equals(merchant.getBusinessDescription(), businessDescription)
                || !Objects.equals(merchant.getContactName(), contactName)
                || !Objects.equals(merchant.getContactPhone(), contactPhone)
                || !Objects.equals(merchant.getVerificationDeclarationAccepted(), declarationAccepted);

        if (!verificationDetailsChanged) {
            return merchantMapper.toDto(merchant);
        }

        if (merchantRepository.existsByRegistrationNumberAndIdNot(registrationNumber, merchant.getId())) {
            throw new ConflictException("Business registration number already registered");
        }

        merchant.setBusinessName(businessName);
        merchant.setBusinessType(businessType);
        merchant.setRegistrationNumber(registrationNumber);
        merchant.setBusinessAddress(businessAddress);
        merchant.setBusinessDescription(businessDescription);
        merchant.setContactName(contactName);
        merchant.setContactPhone(contactPhone);
        merchant.setVerificationDeclarationAccepted(declarationAccepted);

        // Any change to submitted verification details must be reviewed again.
        merchant.setStatus(MerchantStatus.PENDING);
        merchant.setRejectionReason(null);
        merchant.setReviewedAt(null);
        merchant.setReviewedBy(null);

        if (merchant.getUserId() != null) {
            User user = userRepository.findById(merchant.getUserId())
                    .orElseThrow(() -> new com.swiftdrop.common.exception.NotFoundException("Merchant user not found"));
            user.setFullName(merchant.getContactName());
            user.setPhoneNumber(merchant.getContactPhone());
            // Keep the merchant able to sign in and edit their profile while review is pending.
            // Machine API access is gated separately by the merchant's APPROVED status.
            user.setStatus("ACTIVE");
            user.setEnabled(true);
            userRepository.save(user);
        }

        return merchantMapper.toDto(merchantRepository.save(merchant));
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
