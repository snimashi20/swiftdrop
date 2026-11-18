# SwiftDrop — Implementation Progress & Live Tracker

> **Last Updated:** 2026-11-18  
> **Active Branch:** `feature/sadeepa-google-login`  
> **Module Owner:** M2 (Sadeepa)  
> **Review Partner:** M8 (Dinira)  

---

## 1. M2 (Sadeepa) Official Task Checklist

### Deadline: Milestone 1 — By November 1 (Logins & Initial Docs)
| Tag | Task Item | Target Date | Status | Notes / Commit |
| :---: | :--- | :---: | :---: | :--- |
| **[BE]** | **Flyway Migration** (`V202610261100__sadeepa_users_and_profiles.sql`) | Oct 26 | ✅ **DONE** | Creates `users` & `user_addresses` tables |
| **[Ops]** | **Google Cloud OAuth Client credentials** (redirect URIs on localhost) | Oct 24 | ✅ **DONE** | Configured via `.env` (`GOOGLE_CLIENT_ID`, `GOOGLE_REDIRECT_URI`) |
| **[BE]** | **Spring OAuth2 Client Google login** (create/link user as `CUSTOMER` + issue JWT) | Oct 28 | ✅ **DONE** | Implemented in `OAuth2AuthenticationSuccessHandler.java` |
| **[FE]** | **"Continue with Google" button** & OAuth callback token receiver page | Oct 28 | ✅ **DONE** | `LoginPage.tsx` button + `OAuthCallbackPage.tsx` using `auth.storage.ts` |
| **[Doc]** | **OAuth 2.0 login sequence diagram** (Report Section 13) | Oct 29 | ✅ **DONE** | Mermaid.js sequence diagram + parameter handling integrated in Section 13 of M2 spec |
| **[Doc]** | **"Security and personal data" section** (Report Section 17) | Oct 29 | ✅ **COMPLETED** | Password hashing, phone/email validation, data masking, IDOR defense in Section 17 of M2 spec |

---

### Deadline: Milestone 2 — By November 15 (Full System, PDF, Tests & Integrations)
| Tag | Task Item | Target Date | Status | Notes / Deliverables |
| :---: | :--- | :---: | :---: | :--- |
| **[BE]** | **Profile API**: view/update name & phone (`07XXXXXXXX` / `+947XXXXXXXX`), saved addresses | Nov 4 | ✅ **DONE** | `AuthController.java` & `AddressController.java` |
| **[BE]** | **Admin user API**: list/search users with role filter, enable/disable, change role, create staff, KYC details & rejection reason | Nov 15 | ✅ **DONE** | `AdminUserController.java` (`GET`, `GET /{id}`, `PATCH /status`, `PUT /role`, `POST /staff`, `PATCH /{id}/approve`, `PATCH /{id}/reject` with reason) |
| **[BE]** | **Delivery receipt PDF (OpenPDF)**: tracking #, addresses, weight, fee, status dates | Nov 8 | ✅ **DONE** | `ReceiptDataDto.java`, `ReceiptService.java` & `ReceiptController.java` (`GET /api/v1/parcels/{id}/receipt` with IDOR defense) |
| **[FE]** | **Profile page with address book** | Nov 9 | ✅ **DONE** | `UserProfilePage.tsx` & `AddressBookPage.tsx` under `src/features/profile/` |
| **[FE]** | **Admin Users page**: filter by role, toggle enable/disable, create staff modal, role pill filters | Nov 10 | ✅ **DONE** | `AdminUsersPage.tsx`, `CreateStaffModal.tsx`, `RoleUpdateModal.tsx`, `StatusConfirmModal.tsx` |
| **[FE]** | **Dedicated Full-Page Admin Workflows (Zero Modals Alternative)** | Nov 15 | ✅ **DONE** | `ApprovalReviewPage.tsx` (`/admin/approvals/:id`), `CreateStaffPage.tsx` (`/admin/staff/new`), `AdminProfilePage.tsx` (`/admin/profile`) |
| **[FE]** | **Google OAuth Role Selection & Approval Flow** | Nov 12 | ✅ **DONE** | `RoleSelectionPage.tsx`, `WaitingApprovalPage.tsx`, query resolver & cookie tracking for Courier/Merchant approval |
| **[FE]** | **Admin Navigation & Route Alignment** | Nov 14 | ✅ **DONE** | Renamed navigation button to "Manage users" (`/admin/users`), redirected legacy `/people`, updated Admin dashboard quick action |
| **[FE/Sec]**| **Cross-Tab Session Synchronization & Role Guards** | Nov 15 | ✅ **DONE** | Implemented `StorageEvent` tracking in `AuthContext.tsx` and role-whitelisting on customer routes to eliminate tab session desync and route lingering |
| **[FE/UX]** | **Scroll-Free Responsive Modal Design** | Nov 15 | ✅ **DONE** | Redesigned `CreateStaffModal`, `RoleUpdateModal`, and `StatusConfirmModal` with 2-column compact inputs, Esc key listener, backdrop dismissal, zero scrollbar |
| **[BE/Integ]** | **M9 Merchant Registration Gap Resolution** | Nov 18 | ✅ **DONE** | Auto-provisions M9 `merchants` record upon Google merchant signup with `PENDING` status and `GOOG-<userId>` code; verified in `OAuth2AuthenticationSuccessHandler.java` |
| **[Sec/Val]** | **Dual-Layer Validation & Constraints Hardening** | Nov 18 | ✅ **DONE** | Frontend Zod/Regex feedback + button auto-disabling; backend `@Size(min = 8)` password constraint on `CreateStaffRequestDto.java` + `@Pattern` phone check returning `HTTP 400 VALIDATION_ERROR` |
| **[Ops]** | **Supabase Shared Cloud PostgreSQL Sync** | Nov 18 | ✅ **DONE** | Synchronized team cloud database on Supabase pooler (6543) & direct migration (5432) with live seeded demo accounts |
| **[FE]** | **"Download receipt" button** in delivery history table | Nov 10 | 🔄 Backend Ready | Endpoints `GET /api/v1/parcels/{id}/receipt` and `/api/v1/receipts/{trackingNumber}` verified live |
| **[Test]** | **Google success handler test** (new user vs existing user linking, disabled check, role selection, merchant auto-creation) | Nov 18 | ✅ **DONE** | `OAuth2AuthenticationSuccessHandlerTest.java` (mocking MerchantRepository, 100% passing) |
| **[Test]** | **Admin-only endpoints authorization tests** (user list, toggle, roles, approval/rejection, user details) | Nov 15 | ✅ **DONE** | `AdminUserControllerTest.java` (17 unit tests passing) |
| **[Test]** | **Receipt ownership authorization & PDF tests** (owner-only download, IDOR defense & %PDF- header) | Nov 13 | ✅ **DONE** | `ReceiptControllerTest.java` (6 tests) & `ReceiptServiceTest.java` (3 tests) passing |
| **[Test]** | **Address ownership & JWT validation tests** | Nov 13 | ✅ **DONE** | `AddressControllerTest.java` (5 tests) & `JwtServiceTest.java` (3 tests) passing |
| **[Test]** | **M2 Consolidated Test Suite Execution** | Nov 18 | ✅ **COMPLETED** | **43+ automated unit/integration tests PASS (0 failures, 100% success rate)** |

---

## 2. Apex10 Team Integration Status

| Member | Module | Status | Latest Milestone from Git |
| :--- | :--- | :---: | :--- |
| **M1 (Sithmi)** | Leadership, Events, Notifications | 🔄 Active | Event model definition & notification service |
| **M2 (Sadeepa)** | Google Login, Profiles, Admin, Receipts | ✅ Complete (43+ Tests Pass) | Google OAuth & Role Selection, Admin User API & Approvals, OpenPDF receipts, address book, Supabase sync, full test suite |
| **M3 (Isuri)** | Booking, History, Docker, CI | ✅ Merged | Core API Dockerfile, Compose, Parcels migration |
| **M4 (Naviru)** | Zones, Pricing, AI Assistant | ⏳ In Progress | Seed zones and pricing Strategy pattern |
| **M5 (Nethmini)** | Status Lifecycle, Tracking, WebSocket | 🔄 In Progress | Status transition state machine & test suite |
| **M6 (Lamya)** | Dispatcher, Workload, Reports | ⏳ In Progress | Dispatcher workload balancing foundation |
| **M7 (Pamithra)** | Courier Mobile App, Proof Uploads | 🔄 In Review | PR opened for Courier Onboarding (`/courier/register`, NIC photo upload, zone selection, and Admin review integration) |
| **M8 (Dinira)** | Foundation, JWT Login, Security | ✅ Merged | SecurityConfig, JWT foundation, BCrypt |
| **M9 (Binuwara)** | Merchant Management, API Keys | ✅ Merged (PR #115) | PR #115 merged into `develop`; M9 merchant registration, hashed API keys, idempotent orders verified. M2 integration gap closed |
| **M10 (Sandavi)**| Webhooks, Retry, Kubernetes | ⏳ In Progress | Webhook consumer and dead-letter queues |

---

## 3. Changelog & Project Milestones

* **2026-11-18**: Enforced dual-layer form and API validation across M2 module. Added `@Size(min = 8)` constraint to `CreateStaffRequestDto.java` for staff passwords, verified Sri Lankan phone regex format (`^(?:0|\+94)7\d{8}$`) across `AdminProfilePage.tsx` and `UpdateUserRequestDto.java` returning `HTTP 400 VALIDATION_ERROR`.
* **2026-11-18**: Resolved M9 Merchant integration gap where Google merchant registration created a user without the corresponding M9 `merchants` record. Updated `OAuth2AuthenticationSuccessHandler.java` to auto-provision pending `Merchant` entity with registration code `GOOG-<userId>`. Updated unit tests in `OAuth2AuthenticationSuccessHandlerTest.java` to verify repository persistence.
* **2026-11-18**: Synchronized local environment with team's centralized Supabase Cloud PostgreSQL database (`aws-0-ap-southeast-1.pooler.supabase.com`), verified Flyway migrations, and confirmed all 7 Docker containers running cleanly.
* **2026-11-15**: Completed dedicated full-page admin views (`ApprovalReviewPage.tsx`, `CreateStaffPage.tsx`, `AdminProfilePage.tsx`) offering a comprehensive zero-modal experience alongside the scroll-free responsive modal designs. Added `GET /api/v1/admin/users/{id}` (deep KYC payload linking merchant & courier profiles) and `PATCH /api/v1/admin/users/{id}/reject` with rejection reason. Expanded `AdminUserControllerTest` to 17 automated tests (100% passing).
* **2026-11-15**: Implemented Cross-Tab Session Synchronization via browser `StorageEvent` API in `AuthContext.tsx` and route-level whitelisting to eliminate tab state drift and session fixation. Redesigned all M2 admin modals (`CreateStaffModal`, `RoleUpdateModal`, `StatusConfirmModal`) with 2-column compact inputs, Esc key listener, backdrop dismissal, and a 100% scroll-free layout.
* **2026-11-14**: Aligned Admin navigation ("Manage users" pointing to `/admin/users`), redirected `/people`, connected Admin quick actions, verified live end-to-end browser workflows, and integrated Google OAuth Role Selection & Pending Admin Approval pipeline.
* **2026-11-04**: Added official Section 6 (Comprehensive Testing & Quality Assurance Specification) in `M2_SADEEPA_MODULE_SPECIFICATION.md` detailing all unit/integration/IDOR test cases and Java 25 execution guidance.
* **2026-11-04**: Drafted official Report Section 13 (OAuth 2.0 Sequence Diagram in Mermaid.js & flow walkthrough) and Section 17 (Security & Personal Data Handling) in `M2_SADEEPA_MODULE_SPECIFICATION.md`.
* **2026-11-04**: Completed OpenPDF delivery receipt generation feature (`ReceiptDataDto`, `ReceiptService`, `ReceiptController`) with IDOR defenses (`isOwner || isAdmin`), milestone tracking timeline, and itemized LKR breakdown table.
* **2026-11-04**: Implemented and passed all 9 receipt tests (`ReceiptControllerTest` 6 tests + `ReceiptServiceTest` 3 tests). Consolidated M2 test suite at 32/32 passing tests.
* **2026-11-02**: Completed Admin User Management frontend interface (`AdminUsersPage.tsx`, `CreateStaffModal.tsx`, `RoleUpdateModal.tsx`, `StatusConfirmModal.tsx`, `RoleBadge.tsx`, `StatusBadge.tsx`).
* **2026-11-01**: Enhanced `AdminUserController` with role query filter (`?role=...`), `PUT /api/v1/admin/users/{id}/role` endpoint, and `@Valid` constraints.
* **2026-11-01**: Added comprehensive test suite `OAuth2AuthenticationSuccessHandlerTest.java` covering auto-provisioning, linking, unverified emails, and disabled accounts.
* **2026-11-01**: Resolved frontend token key storage disparity (`swiftdrop_access_token`) and consolidated Axios API calls under `src/features/profile/profile.api.ts`.
* **2026-11-01**: Added "Continue with Google" button with error query parameter feedback on `LoginPage.tsx`.
* **2026-10-26**: Committed migration `V202610261100__sadeepa_users_and_profiles.sql` (Author: Sadeepa) for `users` and `user_addresses`.
