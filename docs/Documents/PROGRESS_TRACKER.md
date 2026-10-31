# SwiftDrop — Implementation Progress & Live Tracker

> **Last Updated:** 2026-10-26  
> **Active Branch:** `feature/sadeepa-google-login`  
> **Module Owner:** M2 (Sadeepa)  
> **Review Partner:** M8 (Dinira)  

---

## 1. M2 (Sadeepa) Official Task Checklist

### Deadline: Milestone 1 — By November 1 (Logins & Initial Docs)
| Tag | Task Item | Target Date | Status | Notes / Commit |
| :---: | :--- | :---: | :---: | :--- |
| **[BE]** | **Flyway Migration** (`V202610261100__sadeepa_users_and_profiles.sql`) | Oct 26 | ✅ **DONE** | Commit `bbc4efe` on branch |
| **[Ops]** | **Google Cloud OAuth Client credentials** (redirect URIs on localhost) | Oct 24 | ⏳ In Progress | Configured via `.env` |
| **[BE]** | **Spring OAuth2 Client Google login** (create/link user as `CUSTOMER` + issue JWT) | Oct 28 | ⏳ PENDING | Not implemented yet |
| **[FE]** | **"Continue with Google" button** & OAuth callback token receiver page | Oct 28 | ⏳ PENDING | Not implemented yet |
| **[Doc]** | **OAuth 2.0 login sequence diagram** (Report Section 13) | Oct 29 | ✅ **DONE** | Saved to `docs/diagrams/oauth2_google_login_sequence.png` |
| **[Doc]** | **"Security and personal data" section** (Report Section 17) | Oct 29 | ✅ **COMPLETED** | Saved to `docs/report/security_and_personal_data.md` |

---

### Deadline: Milestone 2 — By November 15 (Full System, PDF, Tests)
| Tag | Task Item | Target Date | Status | Notes / Deliverables |
| :---: | :--- | :---: | :---: | :--- |
| **[BE]** | **Profile API**: view/update name & phone (`07XXXXXXXX` / `+947XXXXXXXX`), saved addresses | Nov 4 | ⏳ PENDING | Address book CRUD |
| **[BE]** | **Admin user API**: list/search users, enable/disable, change role, create staff | Nov 6 | ⏳ PENDING | Restricted to `ADMIN` |
| **[BE]** | **Delivery receipt PDF (OpenPDF)**: tracking #, addresses, weight, fee, status dates | Nov 8 | ⏳ PENDING | Restricted to parcel owner |
| **[FE]** | **Profile page with address book** | Nov 9 | ⏳ PENDING | Mobile-friendly |
| **[FE]** | **Admin Users page**: filter by role, toggle enable/disable, create staff modal | Nov 10 | ⏳ PENDING | Role-protected |
| **[FE]** | **"Download receipt" button** in delivery history table | Nov 10 | ⏳ PENDING | Calls receipt endpoint |
| **[Test]** | **Google success handler test** (new user vs existing user linking) | Nov 12 | ⏳ PENDING | JUnit 5 + Mockito |
| **[Test]** | **Admin-only endpoints authorization tests** | Nov 12 | ⏳ PENDING | Spring Security test |
| **[Test]** | **Receipt ownership authorization test** (owner-only download) | Nov 13 | ⏳ PENDING | Unit & slice test |

---

## 2. Apex10 Team Integration Status

| Member | Module | Status | Latest Milestone from Git |
| :--- | :--- | :---: | :--- |
| **M1 (Sithmi)** | Leadership, Events, Notifications | 🔄 Active | Event model definition & team orchestration |
| **M2 (Sadeepa)** | Google Login, Profiles, Admin, Receipts | 🔄 Active | Migration `V202610261100` committed |
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

* **2026-10-26**: Committed migration `V202610261100__sadeepa_users_and_profiles.sql` (Author: Sadeepa) for `users` and `user_addresses`.
* **2026-10-26**: Synchronized official task cards, grading rubrics, and deadlines from `SwiftDrop-Project-Apex10.pdf` into `D:\Projects\Assingments\Documents`.
* **2026-10-26**: Locked module boundaries: Member M2 works strictly within `com.swiftdrop.auth` and M2-specific frontend directories.
