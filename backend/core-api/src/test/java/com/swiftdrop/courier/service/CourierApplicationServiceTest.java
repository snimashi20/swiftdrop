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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourierApplicationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourierApplicationRepository courierApplicationRepository;

    @Mock
    private CourierApplicationMapper courierApplicationMapper;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CourierApplicationService courierApplicationService;

    @Test
    @DisplayName("Lists every application when no status filter is supplied")
    void listForAdmin_withoutStatus_usesFindAll() {
        Pageable pageable = PageRequest.of(0, 20);
        CourierApplication application = CourierApplication.builder().id(4L).build();
        when(courierApplicationRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(application), pageable, 1));
        when(courierApplicationMapper.toDto(application))
                .thenReturn(CourierApplicationResponseDto.builder().id(4L).build());

        var result = courierApplicationService.listForAdmin(null, pageable);

        assertEquals(1, result.getTotalElements());
        verify(courierApplicationRepository).findAll(pageable);
        verify(courierApplicationRepository, never()).findByStatus(any(), any());
    }

    @Test
    @DisplayName("Filters applications by status when one is supplied")
    void listForAdmin_withStatus_usesFindByStatus() {
        Pageable pageable = PageRequest.of(0, 20);
        CourierApplication application = CourierApplication.builder()
                .id(4L).status(CourierApplicationStatus.PENDING).build();
        when(courierApplicationRepository.findByStatus(CourierApplicationStatus.PENDING, pageable))
                .thenReturn(new PageImpl<>(List.of(application), pageable, 1));
        when(courierApplicationMapper.toDto(application))
                .thenReturn(CourierApplicationResponseDto.builder()
                        .id(4L).status(CourierApplicationStatus.PENDING).build());

        var result = courierApplicationService.listForAdmin(CourierApplicationStatus.PENDING, pageable);

        assertEquals(CourierApplicationStatus.PENDING, result.getContent().get(0).getStatus());
        verify(courierApplicationRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Approves a pending application, activates the user, and records the reviewer")
    void approve_pendingApplication_success() {
        CourierApplication application = CourierApplication.builder()
                .id(55L).userId(9L).status(CourierApplicationStatus.PENDING).build();
        User admin = new User();
        admin.setId(7L);
        User courierUser = new User();
        courierUser.setId(9L);
        courierUser.setRole("COURIER");
        courierUser.setStatus("PENDING_APPROVAL");
        courierUser.setEnabled(false);
        when(courierApplicationRepository.findById(55L)).thenReturn(Optional.of(application));
        when(authentication.getName()).thenReturn("admin@example.com");
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findById(9L)).thenReturn(Optional.of(courierUser));
        when(courierApplicationRepository.save(application)).thenReturn(application);
        when(courierApplicationMapper.toDto(application)).thenReturn(CourierApplicationResponseDto.builder()
                .id(55L).status(CourierApplicationStatus.APPROVED).reviewedBy(7L).build());

        CourierApplicationResponseDto result = courierApplicationService.approve(55L, authentication);

        assertEquals(CourierApplicationStatus.APPROVED, application.getStatus());
        assertEquals("ACTIVE", courierUser.getStatus());
        assertTrue(courierUser.isEnabled());
        verify(userRepository).save(courierUser);
        assertEquals(7L, application.getReviewedBy());
        assertNotNull(application.getReviewedAt());
        assertEquals(CourierApplicationStatus.APPROVED, result.getStatus());
    }

    @Test
    @DisplayName("Clears a previous rejection reason on approval")
    void approve_clearsRejectionReason() {
        CourierApplication application = CourierApplication.builder()
                .id(55L).status(CourierApplicationStatus.PENDING).rejectionReason("stale note").build();
        User admin = new User();
        admin.setId(7L);
        when(courierApplicationRepository.findById(55L)).thenReturn(Optional.of(application));
        when(authentication.getName()).thenReturn("admin@example.com");
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(courierApplicationRepository.save(application)).thenReturn(application);
        when(courierApplicationMapper.toDto(application)).thenReturn(CourierApplicationResponseDto.builder()
                .id(55L).status(CourierApplicationStatus.APPROVED).build());

        courierApplicationService.approve(55L, authentication);

        assertNull(application.getRejectionReason());
    }

    @Test
    @DisplayName("Rejects a pending application, disables the user, and trims the reason")
    void reject_pendingApplication_success() {
        CourierApplication application = CourierApplication.builder()
                .id(55L).userId(9L).status(CourierApplicationStatus.PENDING).build();
        User admin = new User();
        admin.setId(7L);
        User courierUser = new User();
        courierUser.setId(9L);
        courierUser.setStatus("PENDING_APPROVAL");
        courierUser.setEnabled(false);
        when(courierApplicationRepository.findById(55L)).thenReturn(Optional.of(application));
        when(authentication.getName()).thenReturn("admin@example.com");
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findById(9L)).thenReturn(Optional.of(courierUser));
        when(courierApplicationRepository.save(application)).thenReturn(application);
        when(courierApplicationMapper.toDto(application)).thenReturn(CourierApplicationResponseDto.builder()
                .id(55L).status(CourierApplicationStatus.REJECTED).rejectionReason("No valid licence").build());

        CourierApplicationResponseDto result = courierApplicationService.reject(55L, " No valid licence ", authentication);

        assertEquals(CourierApplicationStatus.REJECTED, application.getStatus());
        assertEquals("No valid licence", application.getRejectionReason());
        assertEquals("REJECTED", courierUser.getStatus());
        assertFalse(courierUser.isEnabled());
        assertEquals(CourierApplicationStatus.REJECTED, result.getStatus());
    }

    @Test
    @DisplayName("Refuses to review an application that is no longer pending")
    void approve_nonPendingApplication_throwsConflict() {
        CourierApplication application = CourierApplication.builder()
                .id(55L).status(CourierApplicationStatus.APPROVED).build();
        when(courierApplicationRepository.findById(55L)).thenReturn(Optional.of(application));

        assertThrows(ConflictException.class, () -> courierApplicationService.approve(55L, authentication));
        verify(courierApplicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reports a missing application as not found")
    void reject_missingApplication_throwsNotFound() {
        when(courierApplicationRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> courierApplicationService.reject(404L, "reason", authentication));
    }

    @Test
    @DisplayName("Reports a missing reviewer account as not found")
    void approve_unknownReviewer_throwsNotFound() {
        CourierApplication application = CourierApplication.builder()
                .id(55L).status(CourierApplicationStatus.PENDING).build();
        when(courierApplicationRepository.findById(55L)).thenReturn(Optional.of(application));
        when(authentication.getName()).thenReturn("ghost@example.com");
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> courierApplicationService.approve(55L, authentication));
        verify(courierApplicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Looks up an application by the linked user id")
    void getByUserId_returnsApplication() {
        CourierApplication application = CourierApplication.builder().id(55L).userId(9L).build();
        when(courierApplicationRepository.findByUserId(9L)).thenReturn(Optional.of(application));
        when(courierApplicationMapper.toDto(application))
                .thenReturn(CourierApplicationResponseDto.builder().id(55L).userId(9L).build());

        assertEquals(55L, courierApplicationService.getByUserId(9L).getId());
    }
}
