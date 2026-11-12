package com.swiftdrop.webhook.service;

import com.swiftdrop.webhook.domain.DeliveryStatus;
import com.swiftdrop.webhook.domain.WebhookDelivery;
import com.swiftdrop.webhook.repository.WebhookDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookRetryScheduler {

    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookDispatcherService dispatcherService;

    /**
     * Polls the database every 5 seconds for pending retries that have reached their scheduled backoff time.
     */
    @Scheduled(fixedDelayString = "${swiftdrop.webhook.retry-poll-interval-ms:5000}")
    public void processPendingRetries() {
        Instant now = Instant.now();
        List<WebhookDelivery> dueRetries = deliveryRepository.findByStatusAndNextRetryAtLessThanEqual(DeliveryStatus.RETRYING, now);

        if (!dueRetries.isEmpty()) {
            log.info("Found {} webhook delivery attempt(s) ready for retry", dueRetries.size());
            for (WebhookDelivery delivery : dueRetries) {
                try {
                    dispatcherService.retryDelivery(delivery);
                } catch (Exception e) {
                    log.error("Error retrying delivery ID {}: {}", delivery.getId(), e.getMessage(), e);
                }
            }
        }
    }
}
