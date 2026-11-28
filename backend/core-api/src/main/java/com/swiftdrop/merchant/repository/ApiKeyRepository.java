package com.swiftdrop.merchant.repository;

import com.swiftdrop.merchant.domain.ApiKey;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    @EntityGraph(attributePaths = "merchant")
    Optional<ApiKey> findByKeyHashAndActiveTrue(String keyHash);

    List<ApiKey> findByMerchantIdOrderByCreatedAtDesc(Long merchantId);

    Optional<ApiKey> findByIdAndMerchantId(Long id, Long merchantId);

    @Modifying
    @Query("""
            update ApiKey k
            set k.lastUsedAt = :now
            where k.id = :id
              and (k.lastUsedAt is null or k.lastUsedAt < :threshold)
            """)
    int touchLastUsedAt(@Param("id") Long id, @Param("now") Instant now, @Param("threshold") Instant threshold);
}
