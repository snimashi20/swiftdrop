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
1. `V202610250001__init.sql`: Initial setup
2. `V202610261100__sadeepa_users_and_profiles.sql`: Creates `users` and `user_addresses` tables
3. `V202610261500__isuri_parcels.sql`: Creates `parcels` table with tracking number and indices
