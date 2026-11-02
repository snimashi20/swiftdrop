package com.swiftdrop.tracking.service;

import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.parcel.domain.Address;
import com.swiftdrop.parcel.domain.Parcel;
import com.swiftdrop.parcel.repository.ParcelRepository;
import com.swiftdrop.tracking.dto.TrackingResponse;
import com.swiftdrop.tracking.repository.StatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TrackingService {

    private static final Pattern TRACKING_NUMBER = Pattern.compile("^SD[A-Z0-9]{10}$");
    // Same message for a malformed and an unknown number, so nothing can be probed.
    private static final String NOT_FOUND = "Tracking number not found";

    private final ParcelRepository parcelRepository;
    private final StatusHistoryRepository statusHistoryRepository;

    @Transactional(readOnly = true)
    public TrackingResponse getByTrackingNumber(String trackingNumber) {
        String normalized = trackingNumber == null ? "" : trackingNumber.trim().toUpperCase(Locale.ROOT);
        if (!TRACKING_NUMBER.matcher(normalized).matches()) {
            throw new NotFoundException(NOT_FOUND);
        }
        Parcel parcel = parcelRepository.findByTrackingNumber(normalized)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));

        List<TrackingResponse.Entry> timeline = statusHistoryRepository
                .findByParcelIdOrderByChangedAtAsc(parcel.getId()).stream()
                .map(h -> new TrackingResponse.Entry(h.getNewStatus(), h.getChangedAt()))
                .toList();
        if (timeline.isEmpty()) { // e.g. seed data created without history
            timeline = List.of(new TrackingResponse.Entry(parcel.getStatus(), parcel.getCreatedAt()));
        }

        return new TrackingResponse(
                parcel.getTrackingNumber(),
                parcel.getStatus(),
                shortName(parcel.getReceiverName()),
                cityOf(parcel.getPickup()),
                cityOf(parcel.getDropoff()),
                timeline);
    }

    /** "Nimal Perera" -> "N. Perera"; "Nimal" -> "N."; blank -> "". */
    static String shortName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+");
        String initial = new String(Character.toChars(Character.toUpperCase(parts[0].codePointAt(0)))) + ".";
        return parts.length == 1 ? initial : initial + " " + parts[parts.length - 1];
    }

    private static String cityOf(Address address) {
        return address == null || address.getCity() == null ? "" : address.getCity();
    }
}
