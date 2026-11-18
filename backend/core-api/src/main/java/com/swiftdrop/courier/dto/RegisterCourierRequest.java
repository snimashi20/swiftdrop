package com.swiftdrop.courier.dto;

import com.swiftdrop.dispatch.domain.VehicleType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterCourierRequest {

    @NotBlank(message = "Full legal name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Email address is required")
    @Email(message = "Enter a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Mobile phone number is required")
    @Pattern(regexp = "^(?:0|\\+94)7\\d{8}$", message = "Use 07XXXXXXXX or +947XXXXXXXX")
    private String phoneNumber;

    @NotBlank(message = "NIC number is required")
    @Pattern(regexp = "^(?:\\d{9}[VvXx]|\\d{12})$", message = "Enter a valid Sri Lankan NIC (e.g., 991234567V or 200112345678)")
    private String nicNumber;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Residential address is required")
    @Size(min = 5, max = 300, message = "Residential address must be between 5 and 300 characters")
    private String residentialAddress;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    @NotBlank(message = "Vehicle registration / license plate number is required")
    @Size(min = 3, max = 30, message = "Vehicle plate number must be between 3 and 30 characters")
    private String vehiclePlateNumber;

    @NotBlank(message = "Driving license number is required")
    @Size(min = 4, max = 50, message = "Driving license number must be between 4 and 50 characters")
    private String drivingLicenseNumber;

    @NotNull(message = "Driving license expiry date is required")
    @Future(message = "Driving license must not be expired")
    private LocalDate drivingLicenseExpiry;

    @NotNull(message = "Operating delivery zone is required")
    private Long zoneId;

    @NotBlank(message = "Emergency contact name is required")
    @Size(min = 2, max = 100, message = "Emergency contact name must be between 2 and 100 characters")
    private String emergencyContactName;

    @NotBlank(message = "Emergency contact phone is required")
    @Pattern(regexp = "^(?:0|\\+94)7\\d{8}$", message = "Use 07XXXXXXXX or +947XXXXXXXX")
    private String emergencyContactPhone;
}