package com.swiftdrop.dispatch.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Delivery details of a COURIER user. Links to users and zones by id only,
 * so this module does not depend on other members' entity classes.
 */
@Entity
@Table(name = "courier_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourierProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 30)
    private VehicleType vehicleType;

    @Column(name = "zone_id", nullable = false)
    private Long zoneId;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "nic_number", length = 20)
    private String nicNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "residential_address", length = 300)
    private String residentialAddress;

    @Column(name = "driving_license_number", length = 50)
    private String drivingLicenseNumber;

    @Column(name = "driving_license_expiry")
    private LocalDate drivingLicenseExpiry;

    @Column(name = "vehicle_plate_number", length = 30)
    private String vehiclePlateNumber;

    @Column(name = "emergency_contact_name", length = 100)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;

    @Column(name = "nic_front_photo_url", length = 500)
    private String nicFrontPhotoUrl;

    @Column(name = "nic_back_photo_url", length = 500)
    private String nicBackPhotoUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}