package com.swiftdrop.merchant.repository;

import com.swiftdrop.merchant.domain.MerchantOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchantOrderRepository extends JpaRepository<MerchantOrder, Long> {

    Optional<MerchantOrder> findByMerchantIdAndIdempotencyKey(Long merchantId, String idempotencyKey);

    Optional<MerchantOrder> findByMerchantIdAndParcelId(Long merchantId, Long parcelId);

    Page<MerchantOrder> findByMerchantIdOrderByCreatedAtDesc(Long merchantId, Pageable pageable);
}
