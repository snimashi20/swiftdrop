# SwiftDrop — Shared Contracts & Architectural Specifications

This document defines the frozen specifications and contracts across the entire SwiftDrop ecosystem. These specifications must be maintained across all modules to ensure interoperability between team members.

---

## 1. Frozen Shared Contracts

| Contract Item | Frozen Specification | Details / Examples |
| :--- | :--- | :--- |
| **User Roles** | `CUSTOMER`, `MERCHANT`, `COURIER`, `DISPATCHER`, `ADMIN` | Stored as uppercase strings in database and JWT claims |
| **Tracking Number** | Prefix `SD` + 10 alphanumeric characters (uppercase letters and digits) | Example: `SD7K2M9QX4LP` (total length: 12 chars) |
| **RabbitMQ Exchange** | `swiftdrop.parcel.events` | Topic exchange |
| **Event Routing Key** | `parcel.status.changed` | Published whenever a parcel changes status |
| **Standard Queues** | `notification.parcel-status`<br>`webhook.parcel-status` | Both bound to `swiftdrop.parcel.events` with dead-letter queue (DLQ) support |
| **WebSocket Endpoint** | `/ws` | STOMP over SockJS/WebSocket |
| **WebSocket Topic** | `/topic/tracking/{trackingNumber}` | Clients subscribe to receive live status & location pings |
| **Standard Error JSON** | `{ timestamp, status, code, message, path }` | Implemented in `com.swiftdrop.common.dto.ErrorResponse` |

---

## 2. Standard Error Response Contract

All REST APIs must adhere to the standardized JSON error structure handled by `GlobalExceptionHandler`:

```json
{
  "timestamp": "2026-10-26T14:30:00Z",
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "Full name cannot be blank",
  "path": "/api/v1/users/me"
}
```

### Standard Error Codes
- `400 BAD REQUEST`: `VALIDATION_FAILED`, `BAD_REQUEST`, `INVALID_FORMAT`
- `401 UNAUTHORIZED`: `UNAUTHORIZED`, `INVALID_CREDENTIALS`, `TOKEN_EXPIRED`
- `403 FORBIDDEN`: `FORBIDDEN`, `ACCESS_DENIED`
- `404 NOT FOUND`: `RESOURCE_NOT_FOUND`, `USER_NOT_FOUND`, `PARCEL_NOT_FOUND`
- `409 CONFLICT`: `EMAIL_ALREADY_EXISTS`, `STATUS_CONFLICT`
- `500 INTERNAL SERVER ERROR`: `INTERNAL_ERROR`

---

## 3. RabbitMQ Message Contract

Whenever a parcel's status changes, an event of type `ParcelStatusChangedEvent` is published to the `swiftdrop.parcel.events` exchange:

```json
{
  "eventId": "a7b8c9d0-1234-5678-9abc-def012345678",
  "parcelId": 1052,
  "trackingNumber": "SD7K2M9QX4LP",
  "oldStatus": "PICKED_UP",
  "newStatus": "IN_TRANSIT",
  "changedAt": "2026-10-26T14:45:10.500Z",
  "changedByRole": "COURIER",
  "customerId": 42,
  "merchantId": null,
  "note": "Departed central processing facility"
}
```

---

## 4. Environment Variables Mapping (`.env`)

Every service retrieves configuration via environment variables. The `.env` file must never be committed to Git.

```properties
# Database
POSTGRES_DB=swiftdrop
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_password
DB_URL=jdbc:postgresql://localhost:5432/swiftdrop
DB_USERNAME=postgres
DB_PASSWORD=your_password

# Messaging (RabbitMQ)
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_MANAGEMENT_PORT=15672

# Security & JWT (M8 & M2)
JWT_SECRET=super_secret_jwt_key_at_least_256_bits_length_here_12345
JWT_EXPIRATION_MS=86400000
JWT_REFRESH_EXPIRATION_MS=604800000

# Google OAuth2 (M2)
GOOGLE_CLIENT_ID=your_google_client_id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your_google_client_secret
GOOGLE_REDIRECT_URI=http://localhost:8080/login/oauth2/code/google

# Mailpit (M1)
MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_FROM=no-reply@swiftdrop.lk
MAILPIT_UI_PORT=8025

# Service Ports
CORE_API_PORT=8080
NOTIFICATION_SERVICE_PORT=8081
MOCK_MERCHANT_STORE_PORT=8082
AI_ASSISTANT_PORT=8083
FRONTEND_PORT=5173
```

---

## 5. Database Migration Rules (Flyway)

Flyway migration scripts must be added to `backend/core-api/src/main/resources/db/migration/` using the strict naming convention:

$$\text{V}\langle yyyyMMddHHmm \rangle\text{\_\_}\langle \text{module}\rangle\text{\_}\langle \text{description}\rangle\text{.sql}$$

### Current Applied Migrations:
1. `V202610250001__init.sql`: Initial schema setup
2. `V202610261000__nethmini_status_history.sql`: Parcel status history table
3. `V202610261100__sadeepa_users_and_profiles.sql`: Creates `users` and `user_addresses` tables
4. `V202610261500__isuri_parcels.sql`: Creates `parcels` table with tracking number and indices
5. `V202610261600__binuwara_merchants.sql`: Merchants, API keys, and merchant orders
6. `V202610261700__naviru_pricing.sql`: Delivery pricing rules and zones
7. `V202610261800__lamya_courier_profile.sql`: Courier profiles and dispatcher workload tracking
8. `V202610262300__sithmi_notification_log.sql`: Notification logs for SMS and email
9. `V202610271000__sandavi_webhook_tables.sql`: Webhook endpoints and delivery logs
10. `V202610281200__courier_delivery_proof.sql`: Courier proof of delivery uploads
11. `V202610311500__isuri_parcels_pickup_zone.sql`: Pickup zone association for parcels
12. `V202610311800__sithmi_fix_notification_log.sql`: Notification log schema fix
13. `V202610311900__binuwara_merchant_parcel_fk.sql`: Merchant order to parcel foreign key
14. `V202610312100__lamya_courier_profile_fks.sql`: Courier profile and parcel assignment foreign keys
15. `V202610312200__nethmini_status_history_fk.sql`: Status history to parcel foreign key
16. `V202611021200__binuwara_merchant_user_fks.sql`: Merchants to users foreign keys
17. `V202611041000__sithmi_notification_read_at.sql`: `read_at` on `notification_log` for the unread count

---

## 6. Notifications API (notification-service, port 8081)

Served by **notification-service**, not core-api. The browser reaches it on the same origin:
nginx and the Vite dev server send `/api/v1/notifications` to port 8081, everything else to 8080.
Do not add a `/api/v1/notifications` route to core-api.

Authentication is the **same Bearer token core-api issues** (shared `JWT_SECRET`). The role and the
enabled flag are re-read from the `users` table on every request, so disabling an account takes
effect at once. Errors use the standard error JSON of section 2.

| Method & path | Who | Returns |
| :--- | :--- | :--- |
| `GET /api/v1/notifications/me` | any signed-in user | The caller's own notifications, newest first |
| `GET /api/v1/notifications/me/unread-count` | any signed-in user | `{ "unreadCount": 3 }` for the bell |
| `POST /api/v1/notifications/me/read-all` | any signed-in user | `{ "updated": 4 }` |
| `POST /api/v1/notifications/{id}/read` | any signed-in user | `{ "updated": 2 }`; 404 if the row is another user's |
| `GET /api/v1/notifications/admin` | `ADMIN`, `DISPATCHER` | The delivery log, one row per channel |

Query parameters: `page` (from 0) and `size` (1–100) everywhere; `unreadOnly` on `/me`; `type`
(`EMAIL`, `SMS`), `status` (`PENDING`, `SENT`, `FAILED`), `trackingNumber` and `customerId` on
`/admin`. All filters are optional and combine.

Every paged response has the same shape:

```json
{ "content": [], "page": 0, "size": 20, "totalElements": 0, "totalPages": 0, "last": true }
```

**`/me` returns one item per status change**, even though a change is both e-mailed and texted, and
it carries no channel or recipient: `id`, `parcelId`, `trackingNumber`, `oldStatus`, `newStatus`,
`message` ("Parcel SD7K2M9QX4LP is now in transit."), `createdAt`, `read`, `readAt`. Marking one
item read marks every channel of that status change.

**`/admin` returns one row per delivery attempt**, adding `eventId`, `customerId`, `type`,
`recipient`, `status`, `failureReason` and `sentAt`. `recipient` is always the masked value the
consumer stored (`s***@example.com`, `*******567`); the service never stores or returns a full
e-mail address or phone number.
