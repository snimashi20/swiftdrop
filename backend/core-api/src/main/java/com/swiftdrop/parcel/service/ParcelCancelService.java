package com.swiftdrop.parcel.service;

import com.swiftdrop.common.exception.BadRequestException;
import com.swiftdrop.common.exception.ConflictException;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.parcel.domain.CancelReason;
import com.swiftdrop.parcel.domain.Parcel;
import com.swiftdrop.parcel.domain.ParcelStatus;
import com.swiftdrop.parcel.dto.CancelParcelRequest;
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
    public void cancelForCustomer(Long customerId, Long parcelId, CancelParcelRequest request) {
        Parcel parcel = parcelRepository.findById(parcelId)
                .filter(p -> customerId.equals(p.getCustomerId()))
                .orElseThrow(() -> new NotFoundException("Parcel not found: " + parcelId));

        if (parcel.getStatus() != ParcelStatus.CREATED) {
            throw new ConflictException("Only parcels in CREATED status can be cancelled");
        }

        String comment = request.getComment() == null ? "" : request.getComment().trim();
        if (request.getReason() == CancelReason.OTHER && comment.isEmpty()) {
            throw new BadRequestException("Please tell us why you are cancelling");
        }

        parcel.setCancelReason(request.getReason());
        parcel.setCancelComment(comment.isEmpty() ? null : comment);
        parcelRepository.save(parcel);

        String note = "Cancelled by customer: " + request.getReason().getLabel()
                + (comment.isEmpty() ? "" : " - " + comment);
        parcelStatusService.changeStatus(parcelId, ParcelStatus.CANCELLED, "CUSTOMER", customerId, note);
    }
}
