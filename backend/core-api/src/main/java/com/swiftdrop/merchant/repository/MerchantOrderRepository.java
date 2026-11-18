package com.swiftdrop.merchant.repository;

import com.swiftdrop.merchant.domain.MerchantOrder;
import com.swiftdrop.parcel.domain.ParcelStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import java.time.Instant;

import java.util.Optional;

@Repository
public interface MerchantOrderRepository extends JpaRepository<MerchantOrder, Long> {

    Optional<MerchantOrder> findByMerchantIdAndIdempotencyKey(Long merchantId, String idempotencyKey);

    Optional<MerchantOrder> findByMerchantIdAndParcelId(Long merchantId, Long parcelId);

    @org.springframework.data.jpa.repository.Query("""
            select o from MerchantOrder o
            join Parcel p on p.id = o.parcelId
            where o.merchant.id = :merchantId and p.trackingNumber = :trackingNumber
            """)
    Optional<MerchantOrder> findByMerchantIdAndParcelTrackingNumber(Long merchantId, String trackingNumber);

    Page<MerchantOrder> findByMerchantIdOrderByCreatedAtDesc(Long merchantId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
            select o from MerchantOrder o
            join Parcel p on p.id = o.parcelId
            where o.merchant.id = :merchantId
              and (:status is null or p.status = :status)
            order by o.createdAt desc
            """)
    Page<MerchantOrder> findDashboardOrders(Long merchantId, ParcelStatus status, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
            select p.status, count(o) from MerchantOrder o
            join Parcel p on p.id = o.parcelId
            where o.merchant.id = :merchantId
            group by p.status
            """)
    java.util.List<Object[]> countDashboardOrdersByStatus(Long merchantId);

    @org.springframework.data.jpa.repository.Query(value = """
            select cast(created_at at time zone 'UTC' as date) as order_date, count(*) as order_count
            from merchant_orders
            where merchant_id = :merchantId and created_at >= :since
            group by cast(created_at at time zone 'UTC' as date)
            order by order_date
            """, nativeQuery = true)
    java.util.List<Object[]> countOrdersByUtcDateSince(@Param("merchantId") Long merchantId,
                                                       @Param("since") Instant since);
}
