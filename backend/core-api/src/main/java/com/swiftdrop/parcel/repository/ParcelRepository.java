package com.swiftdrop.parcel.repository;

import com.swiftdrop.parcel.domain.Parcel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParcelRepository extends JpaRepository<Parcel, Long> {

    Optional<Parcel> findByTrackingNumber(String trackingNumber);

    List<Parcel> findByAssignedCourierId(Long assignedCourierId);

    /**
     * Returns all active/in-progress parcels OR parcels updated today for this courier.
     */
    @Query("SELECT p FROM Parcel p WHERE p.assignedCourierId = :courierId " +
           "AND (p.status IN ('PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY') " +
           "     OR p.updatedAt >= :startOfDay) " +
           "ORDER BY p.updatedAt DESC")
    List<Parcel> findTodayDeliveriesForCourier(@Param("courierId") Long courierId,
                                              @Param("startOfDay") Instant startOfDay);
}