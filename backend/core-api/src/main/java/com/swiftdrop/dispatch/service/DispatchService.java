package com.swiftdrop.dispatch.service;

import com.swiftdrop.common.exception.BadRequestException;
import com.swiftdrop.common.exception.ConflictException;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.dispatch.domain.CourierProfile;
import com.swiftdrop.dispatch.dto.CourierWorkloadResponse;
import com.swiftdrop.dispatch.dto.ParcelAssignmentResponse;
import com.swiftdrop.dispatch.dto.UnassignedParcelResponse;
import com.swiftdrop.dispatch.repository.CourierLoad;
import com.swiftdrop.dispatch.repository.CourierProfileRepository;
import com.swiftdrop.dispatch.repository.DispatchParcelRepository;
import com.swiftdrop.parcel.domain.Address;
import com.swiftdrop.parcel.domain.Parcel;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchService {

    private static final ZoneId SRI_LANKA = ZoneId.of("Asia/Colombo");

    private final DispatchParcelRepository parcelRepository;
    private final CourierProfileRepository courierRepository;

    /**
     * Parcels with no courier that are not finished, oldest first.
     * The district filter matches the pickup district (one zone = one district).
     */
    @Transactional(readOnly = true)
    public Page<UnassignedParcelResponse> listUnassigned(String district, LocalDate from, LocalDate to,
                                                         Pageable pageable) {
        var terminal = TerminalStatuses.values();
        Specification<Parcel> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("assignedCourierId")));
            predicates.add(cb.not(root.get("status").in(terminal)));
            if (district != null && !district.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("pickup").get("district")),
                        district.trim().toLowerCase()));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<Instant>get("createdAt"),
                        from.atStartOfDay(SRI_LANKA).toInstant()));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.<Instant>get("createdAt"),
                        to.plusDays(1).atStartOfDay(SRI_LANKA).toInstant()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return parcelRepository.findAll(spec, pageable).map(this::toUnassigned);
    }

    /**
     * Assigns (or reassigns) a parcel to a courier. Never changes the parcel status:
     * only ParcelStatusService may do that.
     * Order of checks follows the activity diagram: parcel, then courier.
     */
    @Transactional
    public ParcelAssignmentResponse assign(Long parcelId, Long courierProfileId, Long dispatcherUserId) {
        Parcel parcel = parcelRepository.findById(parcelId)
                .orElseThrow(() -> new NotFoundException("Parcel not found"));
        if (TerminalStatuses.isTerminal(parcel.getStatus())) {
            throw new ConflictException("Parcel can no longer be assigned");
        }
        CourierProfile courier = courierRepository.findById(courierProfileId)
                .orElseThrow(() -> new NotFoundException("Courier profile not found"));
        if (!courier.isActive()) {
            throw new BadRequestException("Courier is inactive");
        }

        boolean reassignment = parcel.getAssignedCourierId() != null;
        parcel.setAssignedCourierId(courier.getId());
        parcel.setAssignedByUserId(dispatcherUserId);
        parcel.setAssignedAt(Instant.now());
        Parcel saved = parcelRepository.save(parcel);

        log.info("Parcel id={} {} to courier profile id={}",
                saved.getId(), reassignment ? "reassigned" : "assigned", courier.getId());
        return toAssignment(saved);
    }

    /** Couriers with their number of active deliveries, least busy first. */
    @Transactional(readOnly = true)
    public List<CourierWorkloadResponse> workload(Long zoneId, Boolean active) {
        Map<Long, Long> loads = parcelRepository.countActiveByCourier(TerminalStatuses.values()).stream()
                .collect(Collectors.toMap(CourierLoad::getCourierId, CourierLoad::getActiveCount));

        return findCouriers(zoneId, active).stream()
                .map(c -> new CourierWorkloadResponse(c.getId(), c.getUserId(), c.getVehicleType(),
                        c.getZoneId(), c.isActive(), loads.getOrDefault(c.getId(), 0L)))
                .sorted(Comparator.comparingLong(CourierWorkloadResponse::activeDeliveries))
                .toList();
    }

    private List<CourierProfile> findCouriers(Long zoneId, Boolean active) {
        Pageable all = Pageable.unpaged();
        if (zoneId != null && active != null) {
            return courierRepository.findByZoneIdAndActive(zoneId, active, all).getContent();
        } else if (zoneId != null) {
            return courierRepository.findByZoneId(zoneId, all).getContent();
        } else if (active != null) {
            return courierRepository.findByActive(active, all).getContent();
        }
        return courierRepository.findAll(all).getContent();
    }

    private UnassignedParcelResponse toUnassigned(Parcel p) {
        Address pickup = p.getPickup();
        Address dropoff = p.getDropoff();
        return new UnassignedParcelResponse(
                p.getId(),
                p.getTrackingNumber(),
                p.getStatus(),
                pickup == null ? null : pickup.getCity(),
                pickup == null ? null : pickup.getDistrict(),
                dropoff == null ? null : dropoff.getCity(),
                dropoff == null ? null : dropoff.getDistrict(),
                p.getSize(),
                p.getWeightKg(),
                p.getCreatedAt());
    }

    private ParcelAssignmentResponse toAssignment(Parcel p) {
        return new ParcelAssignmentResponse(p.getId(), p.getTrackingNumber(), p.getStatus(),
                p.getAssignedCourierId(), p.getAssignedByUserId(), p.getAssignedAt());
    }
}