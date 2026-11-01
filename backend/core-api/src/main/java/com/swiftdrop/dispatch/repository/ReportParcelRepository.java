package com.swiftdrop.dispatch.repository;

import com.swiftdrop.parcel.domain.Parcel;
import com.swiftdrop.parcel.domain.ParcelStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Read-only queries for the dispatcher reports. A finished parcel can no longer change,
 * so its updated_at is the moment it reached its final status.
 */
@Repository
public interface ReportParcelRepository extends JpaRepository<Parcel, Long> {

    @Query("select p.status as status, p.updatedAt as finishedAt, p.pickup.district as district "
            + "from Parcel p "
            + "where p.status in :statuses and p.updatedAt >= :since")
    List<FinishedParcel> findFinishedSince(@Param("statuses") Collection<ParcelStatus> statuses,
                                           @Param("since") Instant since);
}