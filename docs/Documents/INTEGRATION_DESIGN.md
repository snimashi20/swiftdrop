# SwiftDrop — Integration Design

Author: Sithmi (M1) · Date: 2026-10-31 · Version: 1.0

---

## 1. Overview

SwiftDrop uses three integration styles to connect its services and external systems:

| Style | Where Used | Why |
| :--- | :--- | :--- |
| **Synchronous REST over HTTPS** | Frontend ↔ core-api, Frontend ↔ notification-service, Merchant systems ↔ core-api | Simple request/reply for CRUD operations where the caller needs an immediate response |
| **Asynchronous Publish/Subscribe via RabbitMQ** | core-api → notification-service, core-api → webhook consumer | Decouples the status-change writer from every downstream consumer; the API response is not delayed by email delivery or webhook retries |
| **Webhook Callback (outbound HTTP POST)** | Webhook consumer → merchant endpoints | Pushes status updates to external merchant systems in near-real-time without requiring them to poll |

The integration design diagram (`docs/diagrams/integration-design.png`) shows the full flow across all three styles.

---

## 2. Why Publish/Subscribe

When a courier updates a parcel's status, several things must happen: an email goes to the customer, a mock SMS is logged, a webhook fires to the merchant, and the tracking page receives a real-time push. If these were all synchronous calls inside the status-change transaction, any single failure (a slow SMTP server, an unreachable merchant endpoint) would block the courier's API response or, worse, roll back a valid status change.

Publish/Subscribe solves this by separating **"something happened"** from **"what to do about it"**:

- **The publisher (core-api)** commits the status change to PostgreSQL and publishes a single `ParcelStatusChangedEvent` to the broker. Its job is done.
- **Each subscriber** (notification-service, webhook consumer) receives its own copy of the event independently and processes it at its own pace.
- **Adding a new consumer** (e.g., an analytics pipeline) requires zero changes to the publisher — just bind a new queue to the existing exchange.

This is the **Observer pattern** at the infrastructure level: the subject (core-api) does not know or care how many observers exist.

### Alternatives considered

| Alternative | Why Rejected |
| :--- | :--- |
| **Synchronous REST calls from core-api to notification-service** | Couples the status-change latency to email/SMS delivery time. A 5-second SMTP timeout becomes a 5-second courier API response. |
| **Polling** (notification-service queries core-api for new events) | Adds latency (polling interval), wastes resources when nothing changed, and requires core-api to maintain an event log API. |
| **Shared database** (notification-service reads the parcel table directly) | Tight coupling at the data layer. Schema changes in core-api break notification-service with no compile-time warning. Violates service encapsulation. |

---

## 3. Why RabbitMQ

| Criterion | RabbitMQ | Kafka | Redis Streams |
| :--- | :--- | :--- | :--- |
| **Message pattern** | Topic exchange with per-consumer queues — exactly what SwiftDrop needs | Log-based, designed for high-throughput replay — overkill for our volume | Lightweight but lacks native dead-letter, routing keys, and durable exchange semantics |
| **Delivery guarantees** | Per-message acknowledgement with requeue and dead-letter routing | Consumer-group offsets — powerful but complex for two consumers | Manual ACK, no built-in DLQ |
| **Dead-letter support** | Built-in: rejected or expired messages route to a DLQ automatically | Requires a custom error topic and producer-side retry logic | Manual implementation |
| **Spring Boot integration** | First-class via `spring-boot-starter-amqp` — `@RabbitListener`, `RabbitTemplate`, auto-declared topology | Requires `spring-kafka` + Zookeeper/KRaft — heavier operational footprint | Requires manual `StreamListener` wiring |
| **Operational complexity** | Single process, management UI included, runs in Docker with one image | Minimum 3 brokers + ZooKeeper (or KRaft) for production | Tied to Redis — mixing concerns if Redis is only used as a broker |
| **Team familiarity** | Covered in the EAD module, well-documented Spring examples | Not covered in coursework | Not covered in coursework |

**Decision:** RabbitMQ is the right fit because SwiftDrop needs reliable message delivery with dead-letter handling for a small number of consumers, not high-throughput log replay. Its Spring Boot integration lets us declare the full topology (exchange, queues, bindings, DLQs) as Java `@Bean` definitions, and the `@RabbitListener` annotation makes consumers trivial to write.

---

## 4. Topology

```
                    ┌─────────────────────────────────┐
                    │  Topic Exchange                  │
                    │  swiftdrop.parcel.events         │
                    │  routing key: parcel.status.#    │
                    └──────────┬──────────┬────────────┘
                               │          │
                 ┌─────────────┘          └─────────────┐
                 ▼                                      ▼
   ┌─────────────────────────┐        ┌─────────────────────────┐
   │ notification.parcel-    │        │ webhook.parcel-          │
   │ status                  │        │ status                   │
   │ (durable, x-dead-letter │        │ (durable, x-dead-letter  │
   │  → .dlq)               │        │  → .dlq)                 │
   └────────────┬────────────┘        └────────────┬─────────────┘
                │                                  │
                ▼                                  ▼
   notification.parcel-status.dlq    webhook.parcel-status.dlq
```

- **Exchange type: Topic** — allows future routing keys (e.g., `parcel.location.updated`) without changing the exchange.
- **One queue per consumer** — notification-service and the webhook consumer each get their own copy of every event.
- **Dead-letter queues** — if a consumer rejects a message (e.g., unparseable JSON), it lands in the matching `.dlq` instead of being lost. An operator can inspect and replay it from the RabbitMQ management console.

---

## 5. Event Contract

All consumers depend on the shared `ParcelStatusChangedEvent` record:

```java
public record ParcelStatusChangedEvent(
    UUID    eventId,          // idempotency key
    Long    parcelId,
    String  trackingNumber,
    String  oldStatus,        // null when the parcel is first created
    String  newStatus,
    Instant occurredAt,
    String  changedByRole,    // CUSTOMER, COURIER, DISPATCHER, SYSTEM
    Long    customerId,
    Long    courierProfileId, // nullable
    String  note              // nullable
) {}
```

- **`eventId`** is a UUID generated at the point of the status change. Consumers use it for idempotency: the notification-service has a unique constraint on `(event_id, type)` so processing the same event twice is a no-op.
- **Serialization:** Jackson JSON via `Jackson2JsonMessageConverter` on the producer side and `JacksonJsonMessageConverter` on the consumer side. Both are configured as Spring beans.

---

## 6. Publish-After-Commit Guarantee

The publisher uses `@TransactionalEventListener(phase = AFTER_COMMIT)`:

1. `ParcelStatusService` updates the parcel status and inserts a `StatusHistory` row inside a `@Transactional` method.
2. Within the same method, it calls `ApplicationEventPublisher.publishEvent(event)` — a Spring application event, not yet a RabbitMQ message.
3. Spring holds the event until the transaction commits successfully.
4. Only after the commit does `ParcelStatusEventPublisher.publish()` fire, which sends the event to RabbitMQ via `RabbitTemplate`.

**Why this matters:** If the database transaction rolls back (e.g., optimistic lock conflict), no event is published. Consumers never see a status change that didn't actually happen. If the broker is down after the commit, the publisher catches the `AmqpException` and logs it — the status change is not rolled back because it is already committed and the customer's data is correct.

---

## 7. Consumer Resilience

### Notification consumer (`notification-service`)
- Validates required fields; rejects malformed messages immediately (`AmqpRejectAndDontRequeueException` → DLQ).
- Checks `existsByEventIdAndType` before processing — duplicate events are skipped.
- Inserts a `PENDING` notification log row first (claiming the unique constraint slot), then attempts delivery. If delivery fails, the row is marked `FAILED` with the exception class name.
- A failed email or SMS does **not** reject the message — the event is acknowledged and the failure is recorded in `notification_log`.

### Webhook consumer (`core-api`)
- Builds a JSON payload and signs it with HMAC-SHA256 using the merchant's webhook secret.
- Retries with exponential backoff (5s → 25s → 125s) on HTTP 5xx responses.
- After all retries are exhausted, the message is rejected and routed to `webhook.parcel-status.dlq`.
- Each delivery attempt is recorded in `webhook_delivery_log` for auditability.

---

## 8. Retry and Dead-Letter Policy

| Scenario | Behaviour |
| :--- | :--- |
| Consumer throws `AmqpRejectAndDontRequeueException` | Message goes directly to the DLQ — no retry |
| Consumer throws any other exception | Message is requeued by RabbitMQ for another attempt (up to 3 times, then rejected → DLQ) |
| Webhook HTTP 5xx | Consumer retries internally with backoff before acknowledging; if all attempts fail, message is rejected → DLQ |
| Poison message (unparseable JSON) | Deserialization fails, consumer rejects → DLQ |
| Broker down when publisher sends | `AmqpException` caught and logged; status change is already committed; event is lost (acceptable trade-off — the tracking page still shows the correct status via REST) |

Dead-letter messages sit in the DLQ until an operator inspects them via the RabbitMQ management UI and either replays or discards them.

---

## 9. Real-Time Tracking (WebSocket/STOMP)

In addition to the asynchronous broker flow, `core-api` pushes status updates to the React tracking page via STOMP over WebSocket:

- `TrackingBroadcaster` listens for the same Spring application event (`@TransactionalEventListener(phase = AFTER_COMMIT)`) and sends a message to `/topic/tracking/{trackingNumber}` using `SimpMessagingTemplate`.
- The React frontend subscribes to this topic and updates the tracking timeline in real time.
- This is a separate, fire-and-forget channel — if the WebSocket connection drops, the frontend falls back to REST polling.

---

## 10. Summary

SwiftDrop's integration architecture separates concerns cleanly: REST for synchronous CRUD, RabbitMQ pub/sub for asynchronous event distribution, and outbound webhooks for merchant integration. RabbitMQ was chosen over Kafka and Redis because it provides exactly the delivery guarantees, dead-letter routing, and Spring Boot integration that a parcel-tracking system with a small number of consumers needs — without the operational overhead of a distributed log.
