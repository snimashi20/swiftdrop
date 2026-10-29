package com.swiftdrop.webhook.repository;

import com.swiftdrop.webhook.domain.WebhookEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, Long> {

    Optional<WebhookEndpoint> findByMerchantId(Long merchantId);

    Optional<WebhookEndpoint> findByMerchantIdAndActiveTrue(Long merchantId);

    boolean existsByMerchantId(Long merchantId);
}
