package com.swiftdrop.parcel.api;

import com.swiftdrop.parcel.dto.CancelParcelRequest;
import com.swiftdrop.parcel.dto.ParcelDetailResponse;
import com.swiftdrop.parcel.service.ParcelCancelService;
import com.swiftdrop.parcel.service.ParcelService;
import com.swiftdrop.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/parcels")
@RequiredArgsConstructor
public class ParcelCancelController {

    private final ParcelCancelService parcelCancelService;
    private final ParcelService parcelService;

    /** Cancel my parcel with a reason. Only allowed while it is still CREATED. */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ParcelDetailResponse cancel(@PathVariable Long id,
                                       @Valid @RequestBody CancelParcelRequest request,
                                       @AuthenticationPrincipal AuthenticatedUser currentUser) {
        parcelCancelService.cancelForCustomer(currentUser.id(), id, request);
        return parcelService.getForCustomer(currentUser.id(), id);
    }
}