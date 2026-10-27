package com.swiftdrop.webhook.repository;

import com.swiftdrop.webhook.domain.DeliveryStatus;
import com.swiftdrop.webhook.domain.WebhookDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, Long> {

    List<WebhookDelivery> findByEndpointIdOrderByCreatedAtDesc(Long endpointId);

    Page<WebhookDelivery> findByEndpointIdOrderByCreatedAtDesc(Long endpointId, Pageable pageable);

    List<WebhookDelivery> findByStatusAndNextRetryAtLessThanEqual(DeliveryStatus status, Instant time);
}
