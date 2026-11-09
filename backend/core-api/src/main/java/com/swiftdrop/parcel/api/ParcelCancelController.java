package com.swiftdrop.parcel.api;

import com.swiftdrop.parcel.dto.ParcelDetailResponse;
import com.swiftdrop.parcel.service.ParcelCancelService;
import com.swiftdrop.parcel.service.ParcelService;
import com.swiftdrop.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/parcels")
@RequiredArgsConstructor
public class ParcelCancelController {

    private final ParcelCancelService parcelCancelService;
    private final ParcelService parcelService;

    /** Cancel my parcel. Only allowed while it is still CREATED. */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ParcelDetailResponse cancel(@PathVariable Long id,
                                       @AuthenticationPrincipal AuthenticatedUser currentUser) {
        parcelCancelService.cancelForCustomer(currentUser.id(), id);
        return parcelService.getForCustomer(currentUser.id(), id);
    }
}