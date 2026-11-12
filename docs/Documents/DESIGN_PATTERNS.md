# SwiftDrop — Design Patterns and SOLID Principles

Author: Sithmi (M1) · Date: 2026-10-31 · Version: 1.0

This document maps the GoF and enterprise design patterns used in SwiftDrop to their concrete implementations in the codebase, and explains how each SOLID principle is applied.

---

## 1. Design Patterns

### 1.1 Strategy Pattern

**Intent:** Define a family of interchangeable algorithms, encapsulate each one, and let the client select which algorithm to use at runtime — without changing the client code.

**Where it is used:** Pricing fee calculation.

| Role | Class | Location |
| :--- | :--- | :--- |
| Strategy interface | `PricingStrategy` | `pricing/service/PricingStrategy.java` |
| Concrete strategy A | `StandardPricingStrategy` | `pricing/service/StandardPricingStrategy.java` |
| Concrete strategy B | `ExpressPricingStrategy` | `pricing/service/ExpressPricingStrategy.java` |
| Context | `PricingService` | `pricing/service/PricingService.java` |

**How it works:**

- `PricingStrategy` defines a single method: `calculateFee(rule, size, weightKg, isInterZone)`.
- `StandardPricingStrategy` (`@Component("STANDARD")`) computes the fee from the base fee, per-kg rate, size factor, and inter-zone surcharge.
- `ExpressPricingStrategy` (`@Component("EXPRESS")`) delegates to `StandardPricingStrategy` and applies a 1.5x multiplier.
- `PricingService` holds a `Map<String, PricingStrategy>` (Spring auto-collects all beans that implement the interface, keyed by their `@Component` name). The active strategy is selected by the `PRICING_STRATEGY` environment variable, defaulting to `STANDARD`.

**Why it matters:** Adding a new pricing tier (e.g., `ECONOMY`, `SAME_DAY`) requires only a new `@Component` class that implements `PricingStrategy`. No changes to `PricingService`, no `if/else` chains, no redeployment of other modules.

---

### 1.2 Observer Pattern

**Intent:** Define a one-to-many dependency so that when one object changes state, all its dependents are notified automatically — without the subject knowing who they are.

**Where it is used:** Parcel status change event propagation.

| Role | Class | Location |
| :--- | :--- | :--- |
| Subject | `ParcelStatusService` | `tracking/service/ParcelStatusService.java` |
| Event | `ParcelStatusChangedEvent` | `common/event/ParcelStatusChangedEvent.java` |
| Observer 1 | `ParcelStatusEventPublisher` | `common/event/ParcelStatusEventPublisher.java` |
| Observer 2 | `TrackingBroadcaster` | `tracking/event/TrackingBroadcaster.java` |
| Observer 3 | `ParcelStatusListener` (notification-service) | `notification/event/ParcelStatusListener.java` |

**How it works:**

1. `ParcelStatusService` calls `ApplicationEventPublisher.publishEvent(event)` after saving the status change. It does not know which listeners exist.
2. Spring's event system dispatches the event to every method annotated with `@TransactionalEventListener(phase = AFTER_COMMIT)`:
   - `ParcelStatusEventPublisher` sends the event to RabbitMQ (making it available to out-of-process consumers).
   - `TrackingBroadcaster` pushes a STOMP message to browsers watching the tracking number.
3. In the notification-service (a separate Spring Boot application), `ParcelStatusListener` receives the event from RabbitMQ via `@RabbitListener` — extending the Observer pattern across process boundaries through the message broker.

**Why it matters:** `ParcelStatusService` has a single responsibility: validate the transition, save the row, and announce the event. It is not coupled to email delivery, webhook dispatch, or WebSocket broadcasting. Adding a new reaction to status changes (e.g., an analytics event, a push notification) means writing a new listener — zero changes to the subject.

---

### 1.3 Repository Pattern

**Intent:** Mediate between the domain layer and the data-mapping layer using a collection-like interface for accessing domain objects, hiding persistence details from the business logic.

**Where it is used:** Every aggregate in SwiftDrop.

| Domain | Repository Interface | Key Query Methods |
| :--- | :--- | :--- |
| Parcels | `ParcelRepository` | `findByTrackingNumber`, `findByCustomerId` |
| Users | `UserRepository` | `findByEmail`, `existsByEmail` |
| Courier profiles | `CourierProfileRepository` | `findByUserId`, custom workload queries |
| Status history | `StatusHistoryRepository` | `findByParcelIdOrderByChangedAtDesc` |
| Notification log | `NotificationLogRepository` | `findByEventId`, `existsByEventIdAndType` |
| Pricing rules | `PricingRuleRepository` | `findByZoneId` |
| Zones | `ZoneRepository` | `findByDistrict` |
| Merchants | `MerchantRepository` | `findByUserId` |
| API keys | `ApiKeyRepository` | `findByKeyHash` |
| Webhook endpoints | `WebhookEndpointRepository` | `findByMerchantId` |
| Webhook deliveries | `WebhookDeliveryRepository` | delivery log queries |
| Merchant orders | `MerchantOrderRepository` | `findByMerchantId` |
| Delivery proofs | `DeliveryProofRepository` | `findByParcelId` |

**How it works:**

- Each repository is a Spring Data JPA interface extending `JpaRepository<Entity, Long>`.
- Business services (e.g., `ParcelStatusService`, `PricingService`) depend on these interfaces, never on `EntityManager` or raw SQL.
- Spring Data generates the implementation at runtime from the method signatures and `@Query` annotations.

**Why it matters:** The domain layer is testable in isolation — unit tests can mock the repository interface without a database. The persistence technology (JPA/Hibernate, JDBC, or a future migration) can change without touching any service class.

---

### 1.4 Template Method Pattern

**Intent:** Define the skeleton of an algorithm, deferring specific steps to subclasses or implementations.

**Where it is used:** Notification delivery in `NotificationService`.

The private `deliver()` method in `NotificationService` defines a fixed sequence:

1. Check for duplicate `(eventId, type)` — skip if already processed.
2. Insert a `PENDING` notification log row (claims the unique constraint slot).
3. Execute the delivery action (passed as a `Runnable`).
4. Update the row to `SENT` or `FAILED`.

The actual delivery step varies: for email it calls `EmailNotifier.send()` (Thymeleaf template → Mailpit SMTP), for SMS it calls `SmsNotifier.send()` (mock log entry). The surrounding algorithm (dedup check, log row, status update) is identical for both channels.

---

### 1.5 Additional Patterns

| Pattern | Where | Implementation |
| :--- | :--- | :--- |
| **Builder** | Entity construction | Lombok `@Builder` on `Parcel`, `StatusHistory`, `NotificationLog`, `PricingRule`, etc. Avoids telescoping constructors. |
| **Data Transfer Object (DTO)** | API boundaries | Request/response records (`QuoteRequest`, `QuoteResponse`, `NotificationLogResponse`, `ErrorResponse`) decouple the public API from internal entities. |
| **Singleton** | Spring beans | All `@Service`, `@Component`, and `@Repository` beans are singletons by default — one instance per application context. |
| **Front Controller** | Exception handling | `GlobalExceptionHandler` (`@RestControllerAdvice`) intercepts all controller exceptions and maps them to a standardized `ErrorResponse` JSON — a single place for error formatting. |
| **State Machine** | Status transitions | `StatusTransitionRules` encodes the allowed transitions in an `EnumMap<ParcelStatus, Set<ParcelStatus>>`. The `validate()` method rejects illegal transitions (e.g., `DELIVERED → CREATED`) before any state change is persisted. |

---

## 2. SOLID Principles

### 2.1 Single Responsibility Principle (SRP)

> *A class should have one, and only one, reason to change.*

| Class | Single Responsibility |
| :--- | :--- |
| `ParcelStatusService` | Validate and persist status transitions; publish the domain event. Does not send emails, push WebSocket messages, or call webhooks. |
| `ParcelStatusEventPublisher` | Forward the domain event to RabbitMQ. Does not know what the event means or who reads it. |
| `EmailNotifier` | Render a Thymeleaf template and send an email. Does not decide when to send or record the outcome. |
| `SmsNotifier` | Log a mock SMS message. Same boundary as `EmailNotifier`. |
| `NotificationService` | Orchestrate the delivery sequence (dedup, log, dispatch, status update). Delegates the actual send to the notifiers. |
| `StatusTransitionRules` | Encode which status transitions are legal. Does not persist data or publish events. |
| `GlobalExceptionHandler` | Map exceptions to HTTP responses. Does not throw or create exceptions. |

### 2.2 Open/Closed Principle (OCP)

> *Software entities should be open for extension but closed for modification.*

- **Pricing strategies:** Adding a new pricing tier means creating a new class that implements `PricingStrategy`. `PricingService` is closed for modification — it discovers strategies from the Spring context via `Map<String, PricingStrategy>`.
- **Event listeners:** Adding a new reaction to a status change means writing a new `@TransactionalEventListener` or `@RabbitListener`. `ParcelStatusService` is never modified.
- **Notification channels:** A future push-notification channel would be a new notifier class, called alongside `EmailNotifier` and `SmsNotifier` in `NotificationService.handle()`.

### 2.3 Liskov Substitution Principle (LSP)

> *Subtypes must be substitutable for their base types without altering correctness.*

- `StandardPricingStrategy` and `ExpressPricingStrategy` both implement `PricingStrategy`. `PricingService` calls `calculateFee()` on whichever instance the map returns — the behaviour changes (standard vs. express fee), but the contract (accepts the same inputs, returns a `BigDecimal` fee) is honoured by both.
- All repository interfaces extend `JpaRepository`. Service classes depend on the interface, and any Spring Data implementation can be substituted (e.g., an in-memory repository in tests).

### 2.4 Interface Segregation Principle (ISP)

> *Clients should not be forced to depend on interfaces they do not use.*

- `PricingStrategy` has exactly one method (`calculateFee`). A strategy implementation is not forced to implement unrelated operations like CRUD on pricing rules.
- Repository interfaces expose only the query methods that their consumers need. For example, `DispatchParcelRepository` and `ReportParcelRepository` are separate interfaces to the same `Parcel` table — dispatch queries (unassigned parcels, zone filtering) live in one; report queries (summary statistics) live in the other.
- `NotificationController` endpoints are split by audience: `/me/*` endpoints serve the customer, `/admin` serves dispatchers — different clients, different response shapes, no forced coupling.

### 2.5 Dependency Inversion Principle (DIP)

> *High-level modules should not depend on low-level modules. Both should depend on abstractions.*

- **`PricingService` → `PricingStrategy` (interface):** The service depends on the `PricingStrategy` abstraction, not on `StandardPricingStrategy` or `ExpressPricingStrategy` directly. Spring injects the concrete beans.
- **`ParcelStatusService` → `ApplicationEventPublisher` (interface):** The service publishes events through Spring's event abstraction. It does not depend on `RabbitTemplate`, `SimpMessagingTemplate`, or any concrete transport.
- **All services → repository interfaces:** `ParcelStatusService` depends on `ParcelRepository` (an interface), never on Hibernate's `SessionFactory` or `EntityManager`. The JPA implementation is injected by Spring Data.
- **Constructor injection (`@RequiredArgsConstructor`):** All dependencies are declared as `final` fields and injected through the constructor — making them explicit, immutable, and easy to replace in tests.

---

## 3. Summary Table

| Principle / Pattern | Key Evidence in Codebase |
| :--- | :--- |
| **Strategy** | `PricingStrategy` → `StandardPricingStrategy`, `ExpressPricingStrategy`; selected at runtime via `Map<String, PricingStrategy>` |
| **Observer** | `ParcelStatusService` → `ApplicationEventPublisher` → `ParcelStatusEventPublisher`, `TrackingBroadcaster`, `ParcelStatusListener` |
| **Repository** | 14 Spring Data JPA interfaces abstracting all database access |
| **Template Method** | `NotificationService.deliver()` — fixed dedup/log/status skeleton, variable delivery action |
| **SRP** | Each class has one reason to change (e.g., `EmailNotifier` only sends email) |
| **OCP** | New pricing tiers and event listeners are extensions, not modifications |
| **LSP** | `PricingStrategy` implementations are interchangeable; repository interfaces are substitutable |
| **ISP** | Narrow interfaces: one method for strategy, separate repositories per use-case |
| **DIP** | Services depend on interfaces (`PricingStrategy`, `ApplicationEventPublisher`, repositories), never on concrete implementations |
