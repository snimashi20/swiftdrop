# Member M2 (Sadeepa) — Official Task Card & Module Specification

> **Module Owner:** Sadeepa (M2)  
> **Review Partner:** Dinira (M8 — Foundation, JWT login and security)  
> **Branch:** `feature/sadeepa-google-login`  
> **Flyway Migration:** `V202610261100__sadeepa_users_and_profiles.sql`  
> **Rule:** Only work on M2 module files and packages. Do not touch other members' components.

---

## 1. Executive Summary & Grading Breakdown

From the official project brief (`SwiftDrop-Project-Apex10.pdf`):
* **Mark Allocation:**
  * Design documentation: 20 marks
  * Implementation: 50 marks
  * Testing (unit, integration, event flow, authorization, coverage): 15 marks
  * Presentation & Live Demo: 15 marks
  * Bonus Marks: +5 Kubernetes (Helm/Minikube), +5 AI Assistant (Spring AI / MCP)
* **M2 Demo Moments:**
  * **Step 3 of Demo:** Customer logs in with Google live on localhost.
  * **Step 11 of Demo:** Customer downloads the delivery receipt PDF.

---

## 2. Official Task Card for Member M2

### Milestone 1: By November 1 (Working Logins & Initial Docs)
| Tag | Task | Details | Target Date | Status |
| :---: | :--- | :--- | :---: | :---: |
| **[Ops]** | **Google Cloud OAuth Client** | Create OAuth 2.0 client credentials in Google Cloud Console with `localhost` redirect URIs. Share client secret privately (never commit to Git). Put keys in `.env` only. | Oct 24 | In Progress |
| **[BE]** | **Flyway Migration** | Database migration `V202610261100__sadeepa_users_and_profiles.sql` creating `users` and `user_addresses`. | Oct 26 | ✅ **DONE** |
| **[BE]** | **"Login with Google" API** | Spring OAuth2 Client integration: on successful Google authentication, create or link user as `CUSTOMER` and issue our own SwiftDrop JWT access token. | Oct 28 | ⏳ PENDING |
| **[FE]** | **"Continue with Google" UI** | "Continue with Google" button and OAuth callback page that captures and stores the application JWT. | Oct 28 | ⏳ PENDING |
| **[Doc]** | **OAuth 2.0 Login Sequence Diagram** | Report Section 13: Draw sequence diagram using draw.io / PlantUML following M1's template; save to `docs/diagrams/`. | Oct 29 | ✅ **DONE** |
| **[Doc]** | **"Security & Personal Data" Section** | Report Section 17: Write report section covering BCrypt password hashing, request validation, phone/email masking, HTTPS, and environment secrets management. | Oct 29 | ✅ **COMPLETED** |

---

### Milestone 2: By November 15 (Full System, APIs, PDF & Testing)
| Tag | Task | Details | Target Date | Status |
| :---: | :--- | :--- | :---: | :---: |
| **[BE]** | **Profile API** | View and update profile (`fullName`, `phoneNumber`). Must validate Sri Lankan phone format (`07XXXXXXXX` or `+947XXXXXXXX`). CRUD for saved addresses in address book. | Nov 2 – Nov 8 | ⏳ PENDING |
| **[BE]** | **Admin User Management API** | List/search users with pagination and role filters, enable/disable account toggling, change user role, create courier and dispatcher staff accounts. | Nov 4 – Nov 8 | ⏳ PENDING |
| **[BE]** | **Delivery Receipt PDF** | PDF generator using **OpenPDF** library. Includes tracking number, pickup/drop-off addresses, weight, fee, and status transition timestamps. Strictly restricted: only the parcel owner can download. | Nov 8 – Nov 11 | ⏳ PENDING |
| **[FE]** | **Profile & Address Book Page** | User profile page with address book management (add, edit, delete, set default). | Nov 8 – Nov 11 | ⏳ PENDING |
| **[FE]** | **Admin Users Page** | Filter by role (`CUSTOMER`, `COURIER`, `DISPATCHER`, `MERCHANT`, `ADMIN`), toggle enable/disable, modal to create staff accounts. | Nov 8 – Nov 11 | ⏳ PENDING |
| **[FE]** | **"Download Receipt" Button** | Add download receipt button in customer delivery history table. | Nov 8 – Nov 11 | ⏳ PENDING |
| **[Test]** | **M2 Test Suite** | 1. Google success handler tests (new user auto-provisioning vs existing user linking).<br>2. Admin-only authorization tests (`@PreAuthorize("hasRole('ADMIN')")`).<br>3. Receipt ownership authorization test (ensuring Customer A cannot download Customer B's receipt). | Nov 11 – Nov 13 | ⏳ PENDING |

---

## 3. Written Report Responsibilities for M2

From Table 9 in the brief:
* **Section 13 — Sequence: OAuth 2.0 (Google) login flow**:
  * **Owner:** **M2 (Sadeepa)** | Helper: M1
  * Must match actual class names (`SecurityConfig`, `OAuth2AuthenticationSuccessHandler`, `JwtTokenProvider`, `UserRepository`).
* **Section 17 — Security and personal data handling**:
  * **Owner:** **M2 (Sadeepa)** | Helper: M1
  * Status: ✅ **Approved Draft Ready** (Detailed below)

### 3.1 Official Report Draft: Section 17 — Security & Personal Data Protection

> **Section Title:** Security & Personal Data Protection  
> **Author:** Sadeepa (Member 2)  
>
> Enterprise application resilience and customer trust depend directly on adherence to modern application security standards and data privacy compliance. SwiftDrop incorporates security-by-design principles across authentication, authorization, log sanitization, and secrets management:
>
> 1. **Password Hashing & Authentication:**  
>    User passwords for local accounts are never stored in plaintext. They are salted and hashed using **BCrypt** with a configurable strength factor (work factor 12) before database persistence. Authenticated sessions utilize stateless **JSON Web Tokens (JWT)** cryptographically signed using HMAC-SHA256 (`HS256`). Tokens carry minimal user claims (`sub`, `role`, `email`) and strict expiration windows, preventing server-side session state overhead and mitigating session fixation risks.
>
> 2. **OAuth 2.0 Social Authentication:**  
>    Customer authentication is streamlined via Google OAuth 2.0 (Authorization Code Flow with `state` validation against Login CSRF). Confidential credentials (`GOOGLE_CLIENT_SECRET`) reside strictly within the backend server environment and are never exposed to the client bundle. Upon callback exchange, ID tokens are verified against Google's public keys before user identity is linked or provisioned (`OAuth2AuthenticationSuccessHandler`).
>
> 3. **Role-Based Access Control (RBAC) & Object-Level Authorization:**  
>    Fine-grained authorization is enforced via Spring Security's `@PreAuthorize` method security. Endpoints are strictly partitioned across roles (`CUSTOMER`, `COURIER`, `DISPATCHER`, `MERCHANT`, `ADMIN`). To protect against **Insecure Direct Object Reference (IDOR)** vulnerabilities, sensitive operations—such as downloading delivery receipt PDFs or accessing address books—validate that the authenticated `userId` matches the resource owner, unless overridden by an `ADMIN` role.
>
> 4. **Personal Data Masking & Log Hygiene (Sri Lanka PDPA Compliance):**  
>    In alignment with privacy standards and the Sri Lanka Personal Data Protection Act (PDPA No. 9 of 2022), Personally Identifiable Information (PII) is safeguarded:
>    * Customer mobile numbers (`+94 7X XXX XXXX`) and email addresses are masked in application logs and tracebacks (e.g., `+94 77 **** 890`, `s***@gmail.com`).
>    * Public tracking endpoints expose only sanitized, unidentifiable metadata (e.g., receiver initial and city) to prevent unauthorized parcel tracking.
>
> 5. **Input Validation & Exception Sanitization:**  
>    All incoming REST request payloads are validated using Jakarta Bean Validation (`@Valid`). Phone numbers are strictly constrained via regular expressions to valid Sri Lankan formats (`^(?:0|\+94)7[0-9]{8}$`). A centralized `@RestControllerAdvice` sanitizes error responses, suppressing raw database exceptions and internal stack traces from leaking to clients.
>
> 6. **Secrets Management & Transport Security:**  
>    Database credentials, JWT signing secrets, and OAuth keys are injected exclusively via environment variables (`.env`), ensuring no sensitive secrets are committed to version control. Production traffic is enforced over **HTTPS/TLS 1.3** to eliminate eavesdropping and man-in-the-middle (MITM) attacks.
* **Helper Roles**:
  * **Section 2 — Use case diagram: Customer** (Owner: M3, Helpers: M2, M5)
  * **Section 5 — Use case diagram: Dispatcher/Admin** (Owner: M6, Helpers: M2, M4)
  * **Section 15 — Design patterns & principles** (Provide 1 concrete example from M2 code, e.g. Strategy/Factory/DTO pattern).

---

## 4. Strict Code Boundaries (What M2 Touches)

### Backend (`backend/core-api`):
* Package `com.swiftdrop.auth`:
  * `domain/`: `User.java`, `UserAddress.java`, `Role.java`, `AuthProvider.java`
  * `repository/`: `UserRepository.java`, `UserAddressRepository.java`
  * `dto/`: Profile DTOs, address DTOs, admin user DTOs, OAuth2 DTOs
  * `service/`: `GoogleOAuth2UserService.java`, `UserProfileService.java`, `AdminUserService.java`, `ReceiptService.java`
  * `api/`: `GoogleAuthController.java`, `UserProfileController.java`, `AdminUserController.java`, `ReceiptController.java`
* Database Migration:
  * Only `db/migration/V202610261100__sadeepa_users_and_profiles.sql`
* Dependencies in `pom.xml`:
  * OpenPDF (`com.github.librepdf:openpdf`) and Spring Security OAuth2 Client are already present.

### Frontend (`frontend`):
* Directory: `src/features/auth/` and `src/features/profile/` and `src/features/admin/users/`
* Pages:
  * Google OAuth callback redirect handler
  * Profile & Address Book page
  * Admin Users management page
  * Download Receipt button component for parcel history

### What M2 Does NOT Touch:
* Do not touch `com.swiftdrop.parcel`, `com.swiftdrop.tracking`, `com.swiftdrop.pricing`, `com.swiftdrop.merchant`, `com.swiftdrop.courier`, or `com.swiftdrop.dispatch`.
* Do not edit other members' Flyway migrations.
* Do not modify `docker-compose.yml` or `ci.yml` (managed by M3).
