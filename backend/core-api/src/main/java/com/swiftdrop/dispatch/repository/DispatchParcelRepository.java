package com.swiftdrop.dispatch.repository;

import com.swiftdrop.parcel.domain.Parcel;
import com.swiftdrop.parcel.domain.ParcelStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Dispatcher's own view of the parcels table. It lives in the dispatch module,
 * so M3's parcel package is not touched.
 */
@Repository
public interface DispatchParcelRepository extends JpaRepository<Parcel, Long>, JpaSpecificationExecutor<Parcel> {

    @Query("select p.assignedCourierId as courierId, count(p) as activeCount "
            + "from Parcel p "
            + "where p.assignedCourierId is not null and p.status not in :terminal "
            + "group by p.assignedCourierId")
    List<CourierLoad> countActiveByCourier(@Param("terminal") Collection<ParcelStatus> terminal);
}