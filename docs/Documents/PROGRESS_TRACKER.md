# SwiftDrop — Implementation Progress & Live Tracker

> **Last Updated:** 2026-11-01  
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
| **[Doc]** | **OAuth 2.0 login sequence diagram** (Report Section 13) | Oct 29 | ✅ **DONE** | Saved to `docs/diagrams/oauth2_google_login_sequence.png` |
| **[Doc]** | **"Security and personal data" section** (Report Section 17) | Oct 29 | ✅ **COMPLETED** | Saved to `docs/report/security_and_personal_data.md` |

---

### Deadline: Milestone 2 — By November 15 (Full System, PDF, Tests)
| Tag | Task Item | Target Date | Status | Notes / Deliverables |
| :---: | :--- | :---: | :---: | :--- |
| **[BE]** | **Profile API**: view/update name & phone (`07XXXXXXXX` / `+947XXXXXXXX`), saved addresses | Nov 4 | ✅ **DONE** | `AuthController.java` & `AddressController.java` |
| **[BE]** | **Admin user API**: list/search users with role filter, enable/disable, change role, create staff | Nov 6 | ✅ **DONE** | `AdminUserController.java` (`GET`, `PATCH /status`, `PUT /role`, `POST /staff`) |
| **[BE]** | **Delivery receipt PDF (OpenPDF)**: tracking #, addresses, weight, fee, status dates | Nov 8 | ✅ **DONE** | `ReceiptDataDto.java`, `ReceiptService.java` & `ReceiptController.java` (`GET /api/v1/parcels/{id}/receipt` with IDOR defense) |
| **[FE]** | **Profile page with address book** | Nov 9 | ✅ **DONE** | `UserProfilePage.tsx` & `AddressBookPage.tsx` under `src/features/profile/` |
| **[FE]** | **Admin Users page**: filter by role, toggle enable/disable, create staff modal | Nov 10 | ⏳ In Progress | Role-protected dashboard view |
| **[FE]** | **"Download receipt" button** in delivery history table | Nov 10 | ⏳ In Progress | Calls `/api/v1/receipts/{trackingNumber}` |
| **[Test]** | **Google success handler test** (new user vs existing user linking, disabled check) | Nov 12 | ✅ **DONE** | `OAuth2AuthenticationSuccessHandlerTest.java` (5 unit tests passing) |
| **[Test]** | **Admin-only endpoints authorization tests** | Nov 12 | ✅ **DONE** | `AdminUserControllerTest.java` (10 unit tests passing) |
| **[Test]** | **Receipt ownership authorization & PDF tests** (owner-only download, IDOR defense & %PDF- header) | Nov 13 | ✅ **DONE** | `ReceiptControllerTest.java` (6 tests) & `ReceiptServiceTest.java` (3 tests) passing |
| **[Test]** | **Address ownership & JWT validation tests** | Nov 13 | ✅ **DONE** | `AddressControllerTest.java` & `JwtServiceTest.java` passing |

---

## 2. Apex10 Team Integration Status

| Member | Module | Status | Latest Milestone from Git |
| :--- | :--- | :---: | :--- |
| **M1 (Sithmi)** | Leadership, Events, Notifications | 🔄 Active | Event model definition & notification service |
| **M2 (Sadeepa)** | Google Login, Profiles, Admin, Receipts | ✅ Milestone 1 Complete | Google OAuth, Admin User API, PDF receipts, address book, tests |
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

* **2026-11-01**: Enhanced `AdminUserController` with role query filter (`?role=...`), `PUT /api/v1/admin/users/{id}/role` endpoint, and `@Valid` constraints.
* **2026-11-01**: Added comprehensive test suite `OAuth2AuthenticationSuccessHandlerTest.java` covering auto-provisioning, linking, unverified emails, and disabled accounts.
* **2026-11-01**: Resolved frontend token key storage disparity (`swiftdrop_access_token`) and consolidated Axios API calls under `src/features/profile/profile.api.ts`.
* **2026-11-01**: Added "Continue with Google" button with error query parameter feedback on `LoginPage.tsx`.
* **2026-10-26**: Committed migration `V202610261100__sadeepa_users_and_profiles.sql` (Author: Sadeepa) for `users` and `user_addresses`.
