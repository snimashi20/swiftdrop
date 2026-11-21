# SwiftDrop

A courier and parcel-delivery platform built by **Apex10** (10 members).
Customers book deliveries and watch them move live, merchants push orders in over an
API and get signed webhooks back, couriers update parcels from a phone, and
dispatchers assign work and read reports.

Parcel lifecycle: `CREATED → PICKED_UP → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED | FAILED`

---

## Repository layout

| Folder | What lives here |
| --- | --- |
| `backend/core-api` | The modular-monolith Spring Boot API (one package per member) |
| `backend/notification-service` | Separate Spring Boot app: email to Mailpit + mock SMS |
| `backend/mock-merchant-store` | Tiny app that receives and displays our webhooks |
| `backend/ai-assistant` | Bonus: Spring AI chatbot / MCP server |
| `frontend` | React + TypeScript + Vite app, served by Nginx |
| `deploy` | Docker Compose, Dockerfiles, Kubernetes / Helm |
| `docs/diagrams` | draw.io / PlantUML sources and exported PNGs |
| `docs/report` | Design documentation and the submitted PDF |
| `docs/test-reports` | JaCoCo coverage summary and test reports |

## Tech stack

Java 25 (LTS) · Maven · Spring Boot (Web, Validation, Data JPA, Security, OAuth2 Client,
WebSocket, AMQP, Mail, Actuator) · PostgreSQL 16 + Flyway · RabbitMQ · STOMP WebSockets ·
JWT + BCrypt + Google OAuth 2.0 · React + TypeScript + Vite + Tailwind + shadcn/ui ·
Docker + Docker Compose + Nginx · JUnit 5, Mockito, Testcontainers, WireMock, JaCoCo

---

## Getting started

### Prerequisites

- Java 25 (LTS) and Maven
- Node.js 22 (LTS)
- Docker Desktop

### Run it

```bash
git clone https://github.com/<owner>/swiftdrop.git
cd swiftdrop

# 1. Create your local env file and fill in the values
cp .env.example .env      # Windows PowerShell: Copy-Item .env.example .env

# 2. Start everything
docker compose up
```

| Service | Port | URL | Description |
| --- | --- | --- | --- |
| **Frontend** | 3000 / 5173 | `http://localhost:3000` (Docker) / `http://localhost:5173` (Dev) | React + TypeScript SPA |
| **Core API (Swagger UI)** | 8080 | `http://localhost:8080/swagger-ui.html` | OpenAPI documentation & interactive client |
| **Core API (REST)** | 8080 | `http://localhost:8080/api/v1` | Core modular monolith REST endpoints |
| **RabbitMQ Management** | 15672 | `http://localhost:15672` | AMQP dashboard (credentials: `guest`/`guest`) |
| **Mailpit Inbox** | 8025 | `http://localhost:8025` | Local developer mailbox for testing email receipts & notifications |
| **Notification Service** | 8081 | `http://localhost:8081` | Standalone email/SMS notification consumer |

### Demo Accounts & Testing

Default roles: `CUSTOMER`, `MERCHANT`, `COURIER`, `DISPATCHER`, `ADMIN`.
- **Google OAuth Login:** Use "Continue with Google" on `http://localhost:3000/login` (Docker) or `http://localhost:5173/login` (dev server), or open `http://localhost:8080/oauth2/authorization/google`. This needs real Google keys in `.env`.
- **Admin Endpoints:** Accessible under `/api/v1/admin/**` (requires `ROLE_ADMIN`).
- **Swagger Documentation:** Explore all REST routes at `http://localhost:8080/swagger-ui.html`.

### Demo data (seeded automatically)

There is nothing to run by hand any more. The demo users and parcels live in
`backend/core-api/src/main/resources/db/migration/R__demo_seed_data.sql`, a Flyway
*repeatable* migration, so core-api applies it on startup after the versioned migrations.

That file is the single source of truth for demo data — if you need another demo account or
parcel, add it there and keep the statement safe to rerun (Flyway reapplies the file
whenever its checksum changes). Do not start a second seed file; the copy that runs
automatically would quietly fall behind the one you edited.

Every demo account uses the password `Demo@1234`:

| Role | Email |
| --- | --- |
| `ADMIN` | `admin@demo.swiftdrop.lk` |
| `DISPATCHER` | `dispatcher@demo.swiftdrop.lk` |
| `COURIER` | `courier@demo.swiftdrop.lk` (Colombo), `courier3@demo.swiftdrop.lk` (Gampaha), `courier4@demo.swiftdrop.lk` (Kandy), `pending-courier@demo.swiftdrop.lk` (awaiting admin approval) |
| `MERCHANT` | `merchant@demo.swiftdrop.lk` (APPROVED), `pending-merchant@demo.swiftdrop.lk` (PENDING) |
| `CUSTOMER` | `customer@demo.swiftdrop.lk` and `customer2@demo.swiftdrop.lk` |

The seed also creates 8 customer parcels, `SDDEMO000001` to `SDDEMO000008`, one in each status (`CREATED` to `CANCELLED`), plus two approved-merchant sample orders, `SDM9DEMO0001` and `SDM9DEMO0002`. It is safe to rerun after the base demo data already exists.

### Shared team database (Supabase)

By default `docker compose up` uses the local `postgres` container and your data stays on
your own laptop. To work against the shared cloud database instead — so parcels, merchants
and couriers are visible to the whole team — ask Sithmi for the credentials and put them in
your `.env` under the "Option A" block in `.env.example`. Set **both** `DB_URL` (transaction
pooler, port 6543) and `DB_MIGRATION_URL` (session pooler, port 5432), or neither.

Three rules once you are on the shared database:

1. **Only run migrations you intend everyone to get.** We all share one Flyway history, so a
   migration you apply from a feature branch is applied for everybody. Teammates on older
   branches will not crash — `spring.flyway.ignore-migration-patterns: "*:missing"` downgrades
   the unknown migration to a warning — but a half-finished `ALTER TABLE` will still break
   their `ddl-auto: validate` check. If a migration is not ready to merge, switch back to the
   local container while you work on it.
2. **No destructive SQL.** `TRUNCATE`, `DROP`, or a stray `DELETE` without a `WHERE` wipes
   everyone's demo state, not just yours.
3. **Use Docker.** Running core-api outside Docker activates the `dev` profile, which uses an
   in-memory H2 database with `ddl-auto: update` and Flyway switched off. Pointing that at the
   shared database would let Hibernate alter the shared schema, so don't — `docker compose up`
   activates the `docker` profile, which is the configuration the shared database expects.

Before the demo, take a backup: `pg_dump` the shared database the day before, so a bad
afternoon cannot cost us the presentation.

---

## How we work

- **Branches:** `main` (working releases only) ← `develop` (we merge here) ← `feature/m5-tracking`
- **Every change** goes in through a pull request to `develop`, approved by your review
  partner, with a green CI build.
- **Review partners:** M1 ↔ M10 · M8 ↔ M2 · M3 ↔ M4 · M5 ↔ M7 · M6 ↔ M9
- **Commit from your own GitHub account**, in small commits, every day. Commits are the
  proof of your contribution.
- **Never commit secrets.** Real values go in `.env` (git-ignored); add the variable
  *name* to `.env.example`.

### Code rules (short version)

- Backend packages: `com.swiftdrop.<module>` with `api`, `service`, `repository`,
  `domain`, `dto`, `event`.
- Thin controllers, logic in services, never return an entity from an API — return a DTO.
- All URLs start with `/api/v1/`; validate every request body with `@Valid`.
- Read settings from environment variables; nothing hard-coded.
- Flyway files: `V<yyyyMMddHHmm>__<module>_<what>.sql`
- Frontend work goes in `src/features/<module>/`, uses the shared Axios client, and must
  work on a phone screen.

### Shared contracts (frozen — do not change without telling the group)

| Contract | Value |
| --- | --- |
| Roles | `CUSTOMER`, `MERCHANT`, `COURIER`, `DISPATCHER`, `ADMIN` |
| Tracking number | `SD` + 10 random capitals/digits, e.g. `SD7K2M9QX4LP` |
| Exchange | `swiftdrop.parcel.events` (topic), routing key `parcel.status.changed` |
| Queues | `notification.parcel-status`, `webhook.parcel-status` (+ dead-letter queues) |
| WebSocket | endpoint `/ws`, topic `/topic/tracking/{trackingNumber}` |
| Error JSON | `{ timestamp, status, code, message, path }` |

---

## Team

| Member | Name | Owns |
| --- | --- | --- |
| M1 (leader) | Sithmi | Leadership, RabbitMQ events, notifications |
| M2 | Sadeepa | Google login, profiles, admin users, receipts |
| M3 | Isuri | Parcel booking and history, Docker, CI, coverage |
| M4 | Naviru | Zones and pricing, AI assistant bonus |
| M5 | Nethmini | Status lifecycle and live tracking |
| M6 | Lamya | Dispatcher, workload, reports |
| M7 | Pamithra | Courier app |
| M8 | Dinira | Foundation, JWT login, security |
| M9 | Binuwara | Merchants and the merchant API |
| M10 | Sandavi | Webhooks and retry, Kubernetes bonus |

## Merchant integration guide (M9)

### Register and get approved

1. Open `/merchant/register` or send `POST /api/v1/merchants/register` with the business name, registration number, contact name, email, password, and a Sri Lankan mobile number (`07XXXXXXXX` or `+947XXXXXXXX`).
2. Sign in with the registered email and password. The merchant starts in `PENDING` status.
3. An administrator reviews the application under `/admin/merchants` and approves or rejects it. Pending and rejected merchants cannot create API keys or orders.

### Create an API key

1. Sign in as the approved merchant and open `/merchant/api-keys`.
2. Generate a labeled key. Copy it when it appears; the full key is returned once.
3. Store the key in your shop backend's secret manager or environment. Do not put it in frontend code, Git, screenshots, or logs. Revoke it from the same page if it is exposed or no longer needed.

### Create an order

Send a `POST` request to `/api/v1/merchant-api/orders` with:

- `X-API-Key`: your merchant key
- `Idempotency-Key`: a unique request identifier. Reuse it when retrying the same order.
- JSON fields `pickup`, `dropoff`, `receiverName`, `receiverPhone`, `size`, and `weightKg`; `externalOrderId` is optional.

The pickup and dropoff objects use `line1`, `city`, and `district`. A successful new order returns HTTP 201 and a tracking number. Reusing the same idempotency key returns the original order. Bulk creation uses `POST /api/v1/merchant-api/orders/bulk` and accepts up to 50 orders.

The signed-in merchant dashboard reads orders from `/api/v1/merchants/me/orders` and status totals from `/api/v1/merchants/me/orders/summary`. Store integrations should use the API-key protected `/api/v1/merchant-api/orders` endpoints.

Import `docs/postman/SwiftDrop-Merchant-API.postman_collection.json` to try registration, approval, single order, idempotent retry, bulk, list, status, and invalid-key flows. Keep real credentials in a local Postman environment.
