package com.swiftdrop.courier;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.common.exception.BadRequestException;
import com.swiftdrop.common.exception.ConflictException;
import com.swiftdrop.courier.domain.CourierApplication;
import com.swiftdrop.courier.domain.CourierApplicationStatus;
import com.swiftdrop.courier.dto.CourierRegistrationResponse;
import com.swiftdrop.courier.dto.RegisterCourierRequest;
import com.swiftdrop.courier.repository.CourierApplicationRepository;
import com.swiftdrop.courier.service.CourierOnboardingService;
import com.swiftdrop.courier.service.FileStorageService;
import com.swiftdrop.dispatch.domain.CourierProfile;
import com.swiftdrop.dispatch.domain.VehicleType;
import com.swiftdrop.dispatch.repository.CourierProfileRepository;
import com.swiftdrop.dispatch.service.ZoneLookup;
import com.swiftdrop.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourierOnboardingServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AuthService authService;
    @Mock private CourierProfileRepository courierProfileRepository;
    @Mock private CourierApplicationRepository courierApplicationRepository;
    @Mock private ZoneLookup zoneLookup;
    @Mock private FileStorageService fileStorageService;

    @InjectMocks private CourierOnboardingService service;

    private RegisterCourierRequest validRequest;
    private MockMultipartFile nicFront;
    private MockMultipartFile nicBack;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterCourierRequest();
        validRequest.setFullName("Kamal Perera");
        validRequest.setEmail("kamal.rider@swiftdrop.lk");
        validRequest.setPassword("Rider@1234");
        validRequest.setPhoneNumber("0771234567");
        validRequest.setNicNumber("200112345678");
        validRequest.setDateOfBirth(LocalDate.of(2001, 5, 10));
        validRequest.setResidentialAddress("No 12, Galle Road, Colombo 03");
        validRequest.setVehicleType(VehicleType.MOTORBIKE);
        validRequest.setVehiclePlateNumber("WP BEX-1234");
        validRequest.setDrivingLicenseNumber("B-9988776");
        validRequest.setDrivingLicenseExpiry(LocalDate.now().plusYears(3));
        validRequest.setZoneId(5L);
        validRequest.setEmergencyContactName("Nimal Perera");
        validRequest.setEmergencyContactPhone("0719876543");

        nicFront = new MockMultipartFile("nicFront", "front.jpg", "image/jpeg", "front-bytes".getBytes());
        nicBack = new MockMultipartFile("nicBack", "back.png", "image/png", "back-bytes".getBytes());
    }

    @Test
    void register_validApplicantWithBothNicSides_createsPendingUserAndInactiveProfile() {
        when(userRepository.existsByEmailIgnoreCase("kamal.rider@swiftdrop.lk")).thenReturn(false);
        when(zoneLookup.exists(5L)).thenReturn(true);
        when(fileStorageService.storeDocument(nicFront, "nic")).thenReturn("/api/v1/courier/documents/nic/front.jpg");
        when(fileStorageService.storeDocument(nicBack, "nic")).thenReturn("/api/v1/courier/documents/nic/back.png");

        User savedUser = User.builder()
                .id(42L)
                .email("kamal.rider@swiftdrop.lk")
                .fullName("Kamal Perera")
                .role("COURIER")
                .status("PENDING_APPROVAL")
                .enabled(false)
                .build();
        when(authService.createLocalUser(anyString(), anyString(), anyString(), anyString(), eq("COURIER")))
                .thenReturn(savedUser);

        when(courierProfileRepository.save(any(CourierProfile.class))).thenAnswer(inv -> {
            CourierProfile p = inv.getArgument(0);
            p.setId(99L);
            return p;
        });

        CourierRegistrationResponse res = service.register(validRequest, nicFront, nicBack);

        assertEquals(42L, res.userId());
        assertEquals(99L, res.courierProfileId());
        assertEquals("PENDING_APPROVAL", res.status());
        assertEquals("/api/v1/courier/documents/nic/front.jpg", res.nicFrontPhotoUrl());
        assertEquals("/api/v1/courier/documents/nic/back.png", res.nicBackPhotoUrl());
        verify(courierProfileRepository).save(argThat(p -> !p.isActive() && "200112345678".equals(p.getNicNumber())));
    }

    @Test
    void register_validApplicant_queuesPendingCourierApplicationForAdminReview() {
        when(userRepository.existsByEmailIgnoreCase("kamal.rider@swiftdrop.lk")).thenReturn(false);
        when(zoneLookup.exists(5L)).thenReturn(true);
        when(zoneLookup.districtOf(5L)).thenReturn(Optional.of("Colombo"));
        when(fileStorageService.storeDocument(nicFront, "nic")).thenReturn("/front.jpg");
        when(fileStorageService.storeDocument(nicBack, "nic")).thenReturn("/back.png");

        User savedUser = User.builder()
                .id(42L)
                .email("kamal.rider@swiftdrop.lk")
                .fullName("Kamal Perera")
                .phoneNumber("0771234567")
                .role("COURIER")
                .status("PENDING_APPROVAL")
                .enabled(false)
                .build();
        when(authService.createLocalUser(anyString(), anyString(), anyString(), anyString(), eq("COURIER")))
                .thenReturn(savedUser);
        when(courierProfileRepository.save(any(CourierProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        service.register(validRequest, nicFront, nicBack);

        ArgumentCaptor<CourierApplication> captor = ArgumentCaptor.forClass(CourierApplication.class);
        verify(courierApplicationRepository).save(captor.capture());
        CourierApplication application = captor.getValue();
        assertEquals(42L, application.getUserId());
        assertEquals(CourierApplicationStatus.PENDING, application.getStatus());
        assertEquals("Kamal Perera", application.getFullName());
        assertEquals("kamal.rider@swiftdrop.lk", application.getContactEmail());
        assertEquals("0771234567", application.getContactPhone());
        assertEquals("B-9988776", application.getLicenseNumber());
        assertEquals(VehicleType.MOTORBIKE, application.getVehicleType());
        assertEquals("Colombo", application.getPreferredZone());
    }

    @Test
    void register_missingNicBack_throwsBadRequest() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(zoneLookup.exists(5L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.register(validRequest, nicFront, null));
        verify(courierProfileRepository, never()).save(any());
    }

    @Test
    void register_underageApplicant_throwsBadRequest() {
        validRequest.setDateOfBirth(LocalDate.now().minusYears(16));
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> service.register(validRequest, nicFront, nicBack));
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.register(validRequest, nicFront, nicBack));
    }
}