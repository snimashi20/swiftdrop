package com.swiftdrop.courier.service;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.common.exception.BadRequestException;
import com.swiftdrop.common.exception.ConflictException;
import com.swiftdrop.courier.dto.CourierRegistrationResponse;
import com.swiftdrop.courier.dto.RegisterCourierRequest;
import com.swiftdrop.dispatch.domain.CourierProfile;
import com.swiftdrop.dispatch.repository.CourierProfileRepository;
import com.swiftdrop.dispatch.service.ZoneLookup;
import com.swiftdrop.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourierOnboardingService {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final CourierProfileRepository courierProfileRepository;
    private final ZoneLookup zoneLookup;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<ZoneLookup.ZoneOption> listAvailableZones() {
        return zoneLookup.listAll();
    }

    @Transactional
    public CourierRegistrationResponse register(
            RegisterCourierRequest request,
            MultipartFile nicFrontPhoto,
            MultipartFile nicBackPhoto) {

        if (userRepository.existsByEmailIgnoreCase(request.getEmail().trim())) {
            throw new ConflictException("Email is already registered");
        }

        int age = Period.between(request.getDateOfBirth(), LocalDate.now()).getYears();
        if (age < 18) {
            throw new BadRequestException("Courier applicants must be at least 18 years old");
        }

        if (!zoneLookup.exists(request.getZoneId())) {
            throw new BadRequestException("Selected operating delivery zone does not exist");
        }

        if (nicFrontPhoto == null || nicFrontPhoto.isEmpty()) {
            throw new BadRequestException("NIC front side photo is required");
        }
        if (nicBackPhoto == null || nicBackPhoto.isEmpty()) {
            throw new BadRequestException("NIC back side photo is required");
        }

        String nicFrontUrl = fileStorageService.storeDocument(nicFrontPhoto, "nic");
        String nicBackUrl = fileStorageService.storeDocument(nicBackPhoto, "nic");

        // AuthService creates COURIER users with status = PENDING_APPROVAL and enabled = false
        User savedUser = authService.createLocalUser(
                request.getEmail(),
                request.getPassword(),
                request.getFullName().trim(),
                request.getPhoneNumber().trim(),
                "COURIER"
        );

        // Keep courier profile inactive until Admin approves the application
        CourierProfile profile = CourierProfile.builder()
                .userId(savedUser.getId())
                .vehicleType(request.getVehicleType())
                .zoneId(request.getZoneId())
                .active(false)
                .nicNumber(request.getNicNumber().trim().toUpperCase())
                .dateOfBirth(request.getDateOfBirth())
                .residentialAddress(request.getResidentialAddress().trim())
                .drivingLicenseNumber(request.getDrivingLicenseNumber().trim().toUpperCase())
                .drivingLicenseExpiry(request.getDrivingLicenseExpiry())
                .vehiclePlateNumber(request.getVehiclePlateNumber().trim().toUpperCase())
                .emergencyContactName(request.getEmergencyContactName().trim())
                .emergencyContactPhone(request.getEmergencyContactPhone().trim())
                .nicFrontPhotoUrl(nicFrontUrl)
                .nicBackPhotoUrl(nicBackUrl)
                .build();

        CourierProfile savedProfile = courierProfileRepository.save(profile);

        return CourierRegistrationResponse.builder()
                .userId(savedUser.getId())
                .courierProfileId(savedProfile.getId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .vehicleType(savedProfile.getVehicleType().name())
                .zoneId(savedProfile.getZoneId())
                .status(savedUser.getStatus())
                .nicFrontPhotoUrl(savedProfile.getNicFrontPhotoUrl())
                .nicBackPhotoUrl(savedProfile.getNicBackPhotoUrl())
                .build();
    }
}