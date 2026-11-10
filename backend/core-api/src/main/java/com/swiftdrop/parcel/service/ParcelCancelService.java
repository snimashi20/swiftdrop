package com.swiftdrop.parcel.service;

import com.swiftdrop.common.exception.ConflictException;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.parcel.domain.Parcel;
import com.swiftdrop.parcel.domain.ParcelStatus;
import com.swiftdrop.parcel.repository.ParcelRepository;
import com.swiftdrop.tracking.service.ParcelStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ParcelCancelService {

    private final ParcelRepository parcelRepository;
    private final ParcelStatusService parcelStatusService;

    @Transactional
    public void cancelForCustomer(Long customerId, Long parcelId) {
        Parcel parcel = parcelRepository.findById(parcelId)
                .filter(p -> customerId.equals(p.getCustomerId()))
                .orElseThrow(() -> new NotFoundException("Parcel not found: " + parcelId));

        if (parcel.getStatus() != ParcelStatus.CREATED) {
            throw new ConflictException("Only parcels in CREATED status can be cancelled");
        }

        parcelStatusService.changeStatus(parcelId, ParcelStatus.CANCELLED, "CUSTOMER",
                customerId, "Cancelled by customer");
    }
}