# SwiftDrop

A courier and parcel-delivery platform built by **Apex10** (10 members).
Customers book deliveries and watch them move live, merchants push orders in over an
API and get signed webhooks back, couriers update parcels from a phone, and
dispatchers assign work and read reports.

> **Project brief:** `docs/report/SwiftDrop-Project-Apex10.pdf` — the full brief with task
> cards, shared contracts and deadlines. _(TODO: add the shared doc link here.)_

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

Java 21 (LTS) · Maven · Spring Boot (Web, Validation, Data JPA, Security, OAuth2 Client,
WebSocket, AMQP, Mail, Actuator) · PostgreSQL 16 + Flyway · RabbitMQ · STOMP WebSockets ·
JWT + BCrypt + Google OAuth 2.0 · React + TypeScript + Vite + Tailwind + shadcn/ui ·
Docker + Docker Compose + Nginx · JUnit 5, Mockito, Testcontainers, WireMock, JaCoCo

---

## Getting started

> ⚠️ **Placeholder — these steps are filled in by M3 once `docker-compose.yml` lands.**

### Prerequisites

- Java 21 (LTS) and Maven
- Node.js 22 (LTS)
- Docker Desktop

### Run it

```bash
git clone https://github.com/<owner>/swiftdrop.git
cd swiftdrop

# 1. Create your local env file and fill in the values
cp .env.example .env

# 2. Start everything
docker compose up
```

| Service | URL |
| --- | --- |
| Frontend | TBD |
| Core API (Swagger UI) | TBD `/swagger-ui.html` |
| RabbitMQ management | TBD |
| Mailpit inbox | TBD |

### Demo accounts

TBD — one seeded account per role (`CUSTOMER`, `MERCHANT`, `COURIER`, `DISPATCHER`, `ADMIN`).

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
