# SwiftDrop — Project Overview & Architecture Guide

> **Organization:** Apex10  
> **Project:** SwiftDrop — Courier & Parcel Delivery Platform  
> **Target Environment:** Java 21 (LTS) · Spring Boot 4.0.9 · PostgreSQL 16 · RabbitMQ · React + Vite + TypeScript  

---

## 1. System Overview

**SwiftDrop** is a full-featured courier and parcel delivery platform designed to serve multiple user roles:
- **Customers**: Book deliveries, manage personal address books, track packages live via interactive maps and real-time updates.
- **Merchants**: Integrate their online stores via an authenticated Merchant REST API and receive real-time status updates via HMAC-SHA256 signed webhooks.
- **Couriers**: Use a mobile-optimized interface to accept assignments, update parcel delivery progress, record pickup/drop-off notes, and upload proof of delivery.
- **Dispatchers**: Manage system-wide parcel distribution, assign workload to couriers, monitor delays, and generate operational reports.
- **Administrators**: Manage system configuration, oversee user accounts, inspect audit logs, and maintain system health.

---

## 2. Core Parcel Lifecycle

Every parcel in the system transitions through a strictly governed lifecycle:

```
[ CREATED ]
    │
    ▼
[ PICKED_UP ]
    │
    ▼
[ IN_TRANSIT ]
    │
    ▼
[ OUT_FOR_DELIVERY ]
    │
    ├──────────────────────────┐
    ▼                          ▼
[ DELIVERED ]              [ FAILED ]
```

### Parcel Status Descriptions
1. **CREATED**: The customer or merchant has submitted a delivery order. The system calculates shipping fees and assigns a unique tracking number (`SD` + 10 alphanumeric characters).
2. **PICKED_UP**: The courier arrives at the pickup address, inspects the package, and scans/marks it as collected.
3. **IN_TRANSIT**: The parcel is in transit between distribution hubs or sorting facilities.
4. **OUT_FOR_DELIVERY**: The parcel has been loaded onto a courier vehicle for final-mile delivery to the receiver.
5. **DELIVERED**: Successfully handed over to the receiver. Proof of delivery or receiver signature is recorded.
6. **FAILED**: Delivery failed (e.g., incorrect address, receiver unreachable, damaged package). Note is logged for dispatch review.

---

## 3. Technology Stack & System Components

### 3.1 Repository & Services Structure
```
swiftdrop/
├── backend/
│   ├── core-api/                  # Modular monolith Spring Boot 4.0.9 application
│   ├── notification-service/      # Standalone service consuming RabbitMQ events (Mailpit + SMS)
│   ├── mock-merchant-store/       # Lightweight webhook listener application for verification
│   └── ai-assistant/              # Spring AI / MCP Chatbot assistant
├── frontend/                      # React 18/19 + TypeScript + Vite + Tailwind CSS + shadcn/ui
├── deploy/                        # Dockerfiles, Kubernetes manifests, Helm charts
└── docs/                          # Architectural diagrams, project reports, and test metrics
```

### 3.2 Backend Core API Stack
- **Framework:** Spring Boot 4.0.9 (Java 21 LTS)
- **Persistence:** Spring Data JPA + Hibernate (PostgreSQL 16)
- **Database Migrations:** Flyway (`V<yyyyMMddHHmm>__<module>_<what>.sql`)
- **Security:** Spring Security (Stateless JWT + Google OAuth2 Client)
- **Messaging:** Spring AMQP (RabbitMQ 3 Management)
- **Real-Time Communication:** Spring WebSocket with STOMP protocol
- **Documentation:** SpringDoc OpenAPI 3 (`/swagger-ui.html`, `/api-docs`)
- **Testing:** JUnit 5, Mockito, Testcontainers, WireMock, JaCoCo coverage

### 3.3 Infrastructure (Docker Compose)
- **PostgreSQL 16**: Port `5432` — Core database
- **RabbitMQ 3 (with Management UI)**: Port `5672` (AMQP), Port `15672` (Web UI)
- **Mailpit**: Port `1025` (SMTP), Port `8025` (Web UI for reading sent emails)
- **Core API**: Port `8080` (Spring Boot REST API)

---

## 4. Apex10 Team Responsibility Matrix

The development team comprises 10 members, each responsible for a distinct module and paired for peer code reviews:

| Member | Name | Responsibilities | Review Partner |
| :--- | :--- | :--- | :--- |
| **M1 (Lead)** | Sithmi | Architectural leadership, RabbitMQ event orchestration, notification service | M10 (Sandavi) |
| **M2** | **Sadeepa** | **Google OAuth2 login, user profiles, address book, admin user management, receipt generation** | **M8 (Dinira)** |
| **M3** | Isuri | Parcel booking, delivery history, Docker orchestration, CI pipeline, JaCoCo | M4 (Naviru) |
| **M4** | Naviru | Zones, dynamic distance/weight pricing strategy, AI assistant bonus | M3 (Isuri) |
| **M5** | Nethmini | Parcel status lifecycle state machine, real-time tracking, STOMP WebSockets | M7 (Pamithra) |
| **M6** | Lamya | Dispatcher portal, courier workload balancing, analytics and operational reports | M9 (Binuwara) |
| **M7** | Pamithra | Courier mobile web application, parcel scanning, delivery proof file uploads | M5 (Nethmini) |
| **M8** | Dinira | Backend core foundation, local JWT authentication, Spring Security architecture | M2 (Sadeepa) |
| **M9** | Binuwara | Merchant management, API key generation/hashing, merchant order ingestion | M6 (Lamya) |
| **M10** | Sandavi | Webhook delivery engine, exponential backoff retries, Kubernetes deployment | M1 (Sithmi) |

---

## 5. Development Workflow & Git Rules

1. **Branching Model:**
   - `main`: Production-ready releases only.
   - `develop`: Integration branch. All features are merged here through Pull Requests.
   - `feature/<name>-<topic>`: Individual working branches (e.g., `feature/sadeepa-google-login`).
2. **Review & Quality Gate:**
   - PR must be approved by the assigned peer review partner.
   - GitHub Actions CI must pass (`mvn verify`, unit/integration tests, clean build).
   - Zero secrets committed: all credentials must use environment variables defined in `.env.example`.
3. **Coding Standards:**
   - REST Controllers must remain thin; business logic belongs in `@Service` classes.
   - Never expose JPA entities in controller responses; always return DTOs.
   - All REST routes must begin with `/api/v1/`.
   - All incoming request bodies must be validated with `@Valid`.
