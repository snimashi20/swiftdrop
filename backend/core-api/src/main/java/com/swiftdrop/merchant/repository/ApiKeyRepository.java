package com.swiftdrop.merchant.repository;

import com.swiftdrop.merchant.domain.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByKeyHashAndActiveTrue(String keyHash);

    List<ApiKey> findByMerchantIdOrderByCreatedAtDesc(Long merchantId);

    Optional<ApiKey> findByIdAndMerchantId(Long id, Long merchantId);
}
