package com.swiftdrop.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponseDto {
    private Long id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String role;
    private String provider;
    private boolean enabled;
}