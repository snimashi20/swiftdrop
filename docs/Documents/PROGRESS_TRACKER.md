# SwiftDrop — Implementation Progress & Live Tracker

> **Last Updated:** 2026-11-04  
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

### Deadline: Milestone 2 — By November 15 (Full System, PDF, Tests)
| Tag | Task Item | Target Date | Status | Notes / Deliverables |
| :---: | :--- | :---: | :---: | :--- |
| **[BE]** | **Profile API**: view/update name & phone (`07XXXXXXXX` / `+947XXXXXXXX`), saved addresses | Nov 4 | ✅ **DONE** | `AuthController.java` & `AddressController.java` |
| **[BE]** | **Admin user API**: list/search users with role filter, enable/disable, change role, create staff | Nov 6 | ✅ **DONE** | `AdminUserController.java` (`GET`, `PATCH /status`, `PUT /role`, `POST /staff`) |
| **[BE]** | **Delivery receipt PDF (OpenPDF)**: tracking #, addresses, weight, fee, status dates | Nov 8 | ✅ **DONE** | `ReceiptDataDto.java`, `ReceiptService.java` & `ReceiptController.java` (`GET /api/v1/parcels/{id}/receipt` with IDOR defense) |
| **[FE]** | **Profile page with address book** | Nov 9 | ✅ **DONE** | `UserProfilePage.tsx` & `AddressBookPage.tsx` under `src/features/profile/` |
| **[FE]** | **Admin Users page**: filter by role, toggle enable/disable, create staff modal | Nov 10 | ✅ **DONE** | `AdminUsersPage.tsx`, `CreateStaffModal.tsx`, `RoleUpdateModal.tsx`, `StatusConfirmModal.tsx` |
| **[FE]** | **"Download receipt" button** in delivery history table | Nov 10 | 🔄 Backend Ready | Endpoints `GET /api/v1/parcels/{id}/receipt` and `/api/v1/receipts/{trackingNumber}` verified live |
| **[Test]** | **Google success handler test** (new user vs existing user linking, disabled check) | Nov 12 | ✅ **DONE** | `OAuth2AuthenticationSuccessHandlerTest.java` (5 unit tests passing) |
| **[Test]** | **Admin-only endpoints authorization tests** | Nov 12 | ✅ **DONE** | `AdminUserControllerTest.java` (10 unit tests passing) |
| **[Test]** | **Receipt ownership authorization & PDF tests** (owner-only download, IDOR defense & %PDF- header) | Nov 13 | ✅ **DONE** | `ReceiptControllerTest.java` (6 tests) & `ReceiptServiceTest.java` (3 tests) passing |
| **[Test]** | **Address ownership & JWT validation tests** | Nov 13 | ✅ **DONE** | `AddressControllerTest.java` (5 tests) & `JwtServiceTest.java` (3 tests) passing |
| **[Test]** | **M2 Consolidated Test Suite Execution** | Nov 13 | ✅ **COMPLETED** | **32 / 32 tests PASS (0 failures, 100% success rate, ~10.3s runtime)** |

---

## 2. Apex10 Team Integration Status

| Member | Module | Status | Latest Milestone from Git |
| :--- | :--- | :---: | :--- |
| **M1 (Sithmi)** | Leadership, Events, Notifications | 🔄 Active | Event model definition & notification service |
| **M2 (Sadeepa)** | Google Login, Profiles, Admin, Receipts | ✅ Complete (32 Tests Pass) | Google OAuth, Admin User API, OpenPDF receipts, address book, full test suite |
| **M3 (Isuri)** | Booking, History, Docker, CI | ✅ Merged | Core API Dockerfile, Compose, Parcels migration |
| **M4 (Naviru)** | Zones, Pricing, AI Assistant | ⏳ In Progress | Seed zones and pricing Strategy pattern |
| **M5 (Nethmini)** | Status Lifecycle, Tracking, WebSocket | 🔄 In Progress | Status transition state machine & test suite |
| **M6 (Lamya)** | Dispatcher, Workload, Reports | ⏳ In Progress | Dispatcher workload balancing foundation |
| **M7 (Pamithra)** | Courier Mobile App, Proof Uploads | ⏳ In Progress | Courier mobile flows & proof upload |
| **M8 (Dinira)** | Foundation, JWT Login, Security | ✅ Merged | SecurityConfig, JWT foundation, BCrypt |
| **M9 (Binuwara)** | Merchant Management, API Keys | 🔄 In Progress | Merchant, ApiKey, MerchantOrder entities |
| **M10 (Sandavi)**| Webhooks, Retry, Kubernetes | ⏳ In Progress | Webhook consumer and dead-letter queues |

---

## 3. Changelog & Project Milestones

* **2026-11-04**: Added official Section 6 (Comprehensive Testing & Quality Assurance Specification) in `M2_SADEEPA_MODULE_SPECIFICATION.md` detailing all 32 unit/integration/IDOR test cases and Java 25 execution guidance.
* **2026-11-04**: Drafted official Report Section 13 (OAuth 2.0 Sequence Diagram in Mermaid.js & flow walkthrough) and Section 17 (Security & Personal Data Handling) in `M2_SADEEPA_MODULE_SPECIFICATION.md`.
* **2026-11-04**: Completed OpenPDF delivery receipt generation feature (`ReceiptDataDto`, `ReceiptService`, `ReceiptController`) with IDOR defenses (`isOwner || isAdmin`), milestone tracking timeline, and itemized LKR breakdown table.
* **2026-11-04**: Implemented and passed all 9 receipt tests (`ReceiptControllerTest` 6 tests + `ReceiptServiceTest` 3 tests). Consolidated M2 test suite at 32/32 passing tests.
* **2026-11-02**: Completed Admin User Management frontend interface (`AdminUsersPage.tsx`, `CreateStaffModal.tsx`, `RoleUpdateModal.tsx`, `StatusConfirmModal.tsx`, `RoleBadge.tsx`, `StatusBadge.tsx`).
* **2026-11-01**: Enhanced `AdminUserController` with role query filter (`?role=...`), `PUT /api/v1/admin/users/{id}/role` endpoint, and `@Valid` constraints.
* **2026-11-01**: Added comprehensive test suite `OAuth2AuthenticationSuccessHandlerTest.java` covering auto-provisioning, linking, unverified emails, and disabled accounts.
* **2026-11-01**: Resolved frontend token key storage disparity (`swiftdrop_access_token`) and consolidated Axios API calls under `src/features/profile/profile.api.ts`.
* **2026-11-01**: Added "Continue with Google" button with error query parameter feedback on `LoginPage.tsx`.
* **2026-10-26**: Committed migration `V202610261100__sadeepa_users_and_profiles.sql` (Author: Sadeepa) for `users` and `user_addresses`.
