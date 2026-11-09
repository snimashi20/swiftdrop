# SwiftDrop Project Documentation Directory

This folder contains high-level architecture documents, technical specifications, and team guidelines for the **SwiftDrop** courier platform.

---

## Documentation Index

1. **[PROGRESS_TRACKER.md](./PROGRESS_TRACKER.md)**
   - **Live sprint tracker**: current status of M2 deliverables and team progress across branches.
   - Milestone changelog and commit history.

2. **[PROJECT_OVERVIEW.md](./PROJECT_OVERVIEW.md)**
   - Complete project overview, business objectives, and user roles.
   - Core parcel lifecycle state transitions (`CREATED` → `DELIVERED` / `FAILED`).
   - Repository layout, services, and tech stack details.
   - Full Apex10 team matrix and review partner assignments.
   - Git branching strategy, PR rules, and coding conventions.

3. **[SHARED_CONTRACTS_AND_API_SPECS.md](./SHARED_CONTRACTS_AND_API_SPECS.md)**
   - Frozen shared contracts (roles, tracking number format, event topics, queues, WebSocket paths).
   - Global standard error JSON structure and exception mappings.
   - RabbitMQ event schema (`ParcelStatusChangedEvent`).
   - Environment variables reference (`.env.example`).
   - Flyway database migration versioning conventions.

4. **[M2_SADEEPA_MODULE_SPECIFICATION.md](./M2_SADEEPA_MODULE_SPECIFICATION.md)**
   - Specific implementation blueprint for **Member M2 (Sadeepa)**.
   - Google OAuth 2.0 social authentication and token verification flow.
   - User profile and customer address book CRUD design.
   - Admin user management endpoints and staff account provisioning.
   - OpenPDF delivery receipt technical specification with IDOR defense architecture.
   - Official Report Section 13 (OAuth 2.0 Login Flow Mermaid Sequence Diagram) & Section 17 (Security & Personal Data Handling).
   - Official Test Suite & QA Specification (32 automated unit/integration/IDOR test cases cataloged with Java 25 LTS execution guidance).
   - Collaboration and review notes with **M8 (Dinira)**.

---

## Continuous Documentation Protocol

To maintain high documentation accuracy as development progresses:
- **Automatic Sync**: Whenever we create or modify database schemas, entities, endpoints, DTOs, configurations, or tests, we immediately update the respective documentation in this folder.
- **Progress Tracking**: All completed milestones and status changes are reflected in [PROGRESS_TRACKER.md](./PROGRESS_TRACKER.md).
