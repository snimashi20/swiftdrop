package com.swiftdrop.courier.service;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.common.exception.ConflictException;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.courier.domain.CourierApplication;
import com.swiftdrop.courier.domain.CourierApplicationStatus;
import com.swiftdrop.courier.dto.CourierApplicationMapper;
import com.swiftdrop.courier.dto.CourierApplicationResponseDto;
import com.swiftdrop.courier.repository.CourierApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Admin review of courier (rider) applications. Approving activates the linked
 * user account; rejecting disables it, exactly as the merchant flow does.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourierApplicationService {

    private final UserRepository userRepository;
    private final CourierApplicationRepository courierApplicationRepository;
    private final CourierApplicationMapper courierApplicationMapper;

    @Transactional(readOnly = true)
    public Page<CourierApplicationResponseDto> listForAdmin(CourierApplicationStatus status, Pageable pageable) {
        Page<CourierApplication> applications = status == null
                ? courierApplicationRepository.findAll(pageable)
                : courierApplicationRepository.findByStatus(status, pageable);
        return applications.map(courierApplicationMapper::toDto);
    }

    @Transactional(readOnly = true)
    public CourierApplicationResponseDto getByUserId(Long userId) {
        return courierApplicationRepository.findByUserId(userId)
                .map(courierApplicationMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Courier application not found"));
    }

    @Transactional
    public CourierApplicationResponseDto approve(Long applicationId, Authentication authentication) {
        CourierApplication application = getPendingApplication(applicationId);
        application.setStatus(CourierApplicationStatus.APPROVED);
        application.setRejectionReason(null);
        setReviewFields(application, authentication);

        if (application.getUserId() != null) {
            userRepository.findById(application.getUserId()).ifPresent(user -> {
                user.setStatus("ACTIVE");
                user.setEnabled(true);
                userRepository.save(user);
            });
        }

        log.info("Approved courier application with id: {}", applicationId);
        return courierApplicationMapper.toDto(courierApplicationRepository.save(application));
    }

    @Transactional
    public CourierApplicationResponseDto reject(Long applicationId, String reason, Authentication authentication) {
        CourierApplication application = getPendingApplication(applicationId);
        application.setStatus(CourierApplicationStatus.REJECTED);
        application.setRejectionReason(reason.trim());
        setReviewFields(application, authentication);

        if (application.getUserId() != null) {
            userRepository.findById(application.getUserId()).ifPresent(user -> {
                user.setStatus("REJECTED");
                user.setEnabled(false);
                userRepository.save(user);
            });
        }

        log.info("Rejected courier application with id: {}", applicationId);
        return courierApplicationMapper.toDto(courierApplicationRepository.save(application));
    }

    private CourierApplication getPendingApplication(Long applicationId) {
        CourierApplication application = courierApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Courier application not found"));
        if (application.getStatus() != CourierApplicationStatus.PENDING) {
            throw new ConflictException("Only pending courier applications can be reviewed");
        }
        return application;
    }

    private void setReviewFields(CourierApplication application, Authentication authentication) {
        User reviewer = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new NotFoundException("Admin user not found"));
        application.setReviewedAt(Instant.now());
        application.setReviewedBy(reviewer.getId());
    }
}
