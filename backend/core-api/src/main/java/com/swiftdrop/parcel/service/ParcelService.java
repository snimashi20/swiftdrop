package com.swiftdrop.parcel.service;

import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.parcel.domain.Address;
import com.swiftdrop.parcel.domain.Parcel;
import com.swiftdrop.parcel.domain.ParcelStatus;
import com.swiftdrop.parcel.dto.CreateParcelRequest;
import com.swiftdrop.parcel.dto.ParcelAddressDto;
import com.swiftdrop.parcel.dto.ParcelDetailResponse;
import com.swiftdrop.parcel.dto.ParcelResponse;
import com.swiftdrop.parcel.repository.ParcelRepository;
import com.swiftdrop.pricing.dto.QuoteRequest;
import com.swiftdrop.pricing.dto.QuoteResponse;
import com.swiftdrop.pricing.service.PricingService;
import com.swiftdrop.tracking.service.ParcelStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ParcelService {

    private final ParcelRepository parcelRepository;
    private final PricingService pricingService;
    private final TrackingNumberGenerator trackingNumberGenerator;
    private final ParcelStatusService parcelStatusService;

    /** A merchant creates a parcel through the merchant API. */
    @Transactional
    public ParcelResponse createForMerchant(Long merchantId, CreateParcelRequest request) {
        return create(request, null, merchantId, "MERCHANT", null);
    }

    /** A signed-in customer books a parcel. */
    @Transactional
    public ParcelResponse createForCustomer(Long customerId, CreateParcelRequest request) {
        return create(request, customerId, null, "CUSTOMER", customerId);
    }

    /** The customer's own parcels, optionally filtered by status. */
    @Transactional(readOnly = true)
    public Page<ParcelDetailResponse> listForCustomer(Long customerId, ParcelStatus status, Pageable pageable) {
        Page<Parcel> page = (status == null)
                ? parcelRepository.findByCustomerId(customerId, pageable)
                : parcelRepository.findByCustomerIdAndStatus(customerId, status, pageable);
        return page.map(this::toDetail);
    }

    /** One parcel, only if it belongs to this customer. Another customer's parcel looks like "not found". */
    @Transactional(readOnly = true)
    public ParcelDetailResponse getForCustomer(Long customerId, Long parcelId) {
        Parcel parcel = parcelRepository.findById(parcelId)
                .filter(p -> customerId.equals(p.getCustomerId()))
                .orElseThrow(() -> new NotFoundException("Parcel not found: " + parcelId));
        return toDetail(parcel);
    }

    private ParcelResponse create(CreateParcelRequest request, Long customerId, Long merchantId,
                                  String changedByRole, Long changedByUserId) {

        // 1. The fee is always calculated here on the server, never taken from the client.
        QuoteRequest quoteRequest = new QuoteRequest();
        quoteRequest.setPickupDistrict(request.getPickup().getDistrict());
        quoteRequest.setDropoffDistrict(request.getDropoff().getDistrict());
        quoteRequest.setSize(request.getSize());
        quoteRequest.setWeightKg(request.getWeightKg());
        QuoteResponse quote = pricingService.calculateQuote(quoteRequest);

        // 2. Build the parcel.
        Address pickup = new Address();
        pickup.setLine1(request.getPickup().getLine1());
        pickup.setCity(request.getPickup().getCity());
        pickup.setDistrict(request.getPickup().getDistrict());

        Address dropoff = new Address();
        dropoff.setLine1(request.getDropoff().getLine1());
        dropoff.setCity(request.getDropoff().getCity());
        dropoff.setDistrict(request.getDropoff().getDistrict());

        Parcel parcel = new Parcel();
        parcel.setTrackingNumber(trackingNumberGenerator.generate());
        parcel.setCustomerId(customerId);
        parcel.setMerchantId(merchantId);
        parcel.setStatus(ParcelStatus.CREATED);
        parcel.setPickup(pickup);
        parcel.setDropoff(dropoff);
        parcel.setReceiverName(request.getReceiverName());
        parcel.setReceiverPhone(request.getReceiverPhone());
        parcel.setSize(request.getSize());
        parcel.setWeightKg(request.getWeightKg());
        parcel.setDeliveryInstructions(request.getDeliveryInstructions());
        parcel.setFee(quote.getFee());
        parcel.setPickupZoneId(quote.getPickupZoneId());

        // 3. Save it, then write the first status_history row and publish the event.
        Parcel saved = parcelRepository.save(parcel);
        parcelStatusService.recordCreated(saved, changedByRole, changedByUserId);

        return ParcelResponse.builder()
                .id(saved.getId())
                .trackingNumber(saved.getTrackingNumber())
                .status(saved.getStatus())
                .fee(saved.getFee())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    private ParcelDetailResponse toDetail(Parcel parcel) {
        ParcelAddressDto pickup = new ParcelAddressDto();
        pickup.setLine1(parcel.getPickup().getLine1());
        pickup.setCity(parcel.getPickup().getCity());
        pickup.setDistrict(parcel.getPickup().getDistrict());

        ParcelAddressDto dropoff = new ParcelAddressDto();
        dropoff.setLine1(parcel.getDropoff().getLine1());
        dropoff.setCity(parcel.getDropoff().getCity());
        dropoff.setDistrict(parcel.getDropoff().getDistrict());

        return ParcelDetailResponse.builder()
                .id(parcel.getId())
                .trackingNumber(parcel.getTrackingNumber())
                .status(parcel.getStatus())
                .fee(parcel.getFee())
                .pickup(pickup)
                .dropoff(dropoff)
                .receiverName(parcel.getReceiverName())
                .receiverPhone(parcel.getReceiverPhone())
                .size(parcel.getSize())
                .weightKg(parcel.getWeightKg())
                .deliveryInstructions(parcel.getDeliveryInstructions())
                .createdAt(parcel.getCreatedAt())
                .updatedAt(parcel.getUpdatedAt())
                .build();
    }
}