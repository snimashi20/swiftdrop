package com.swiftdrop.dispatch.dto;

import jakarta.validation.constraints.NotNull;

public record AssignParcelRequest(
        @NotNull Long courierProfileId
) {}