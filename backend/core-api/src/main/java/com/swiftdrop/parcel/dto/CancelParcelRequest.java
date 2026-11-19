package com.swiftdrop.parcel.dto;

import com.swiftdrop.parcel.domain.CancelReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CancelParcelRequest {

    @NotNull
    private CancelReason reason;

    /** Required when the reason is OTHER, optional otherwise. */
    @Size(max = 200)
    private String comment;
}
