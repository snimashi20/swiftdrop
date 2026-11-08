package com.swiftdrop.dispatch.dto;

/** A user that can become a courier: id and name only, no contact details. */
public record CourierCandidateResponse(
        Long userId,
        String fullName
) {}