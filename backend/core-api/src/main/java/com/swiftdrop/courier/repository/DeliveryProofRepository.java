package com.swiftdrop.courier.repository;

import com.swiftdrop.courier.domain.DeliveryProof;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryProofRepository extends JpaRepository<DeliveryProof, UUID> {
    Optional<DeliveryProof> findByParcelId(Long parcelId);
    boolean existsByParcelId(Long parcelId);
}