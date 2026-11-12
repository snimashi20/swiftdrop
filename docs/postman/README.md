# SwiftDrop Merchant API Postman guide

This collection exercises the M9 merchant workflow against a local SwiftDrop Core API. Run it with a local development database only: it registers merchants, creates API keys, and creates orders.

## Start the API and seed demo accounts

From the repository root, create `.env` from `.env.example` if needed, then start the local services:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
# Edit .env with local values, then:
docker compose up -d
```

Wait until Core API is healthy (`docker compose ps`). Load the demo users, merchant fixtures, and parcel fixtures from the repository root. The seed script preserves existing records when rerun.

PowerShell:

```powershell
Get-Content scripts\demo-seed.sql -Raw | docker compose exec -T postgres psql -U swiftdrop -d swiftdrop -v ON_ERROR_STOP=1
```

macOS/Linux:

```bash
docker compose exec -T postgres psql -U swiftdrop -d swiftdrop -v ON_ERROR_STOP=1 < scripts/demo-seed.sql
```

The seeded accounts used by this collection are `admin@demo.swiftdrop.lk` (ADMIN) and `merchant@demo.swiftdrop.lk` (MERCHANT). Their shared demo password is documented in the SQL seed file. It is intentionally not stored in this exported Postman environment or collection.

## Import and configure Postman

1. Import `SwiftDrop-Merchant-API.postman_collection.json` and `SwiftDrop-Merchant-API.postman_environment.json` from this folder.
2. Select the **SwiftDrop local merchant API** environment.
3. Set `demoPassword` in Postman to the demo password from `scripts/demo-seed.sql`. Keep it in your local Postman environment; do not export or commit it.
4. Confirm `baseUrl` points at the local API (`http://localhost:8080/api/v1` by default).
5. Run the entire collection sequentially in the Postman Collection Runner. Requests are ordered so login tokens, merchant IDs, API keys, tracking numbers, and idempotency values are captured into the selected environment as their responses arrive.

The collection uses `POST /api/v1/auth/login` with `{ "email", "password" }`. A successful response is HTTP 200 and includes `token`, `tokenType`, `id`, `email`, `name`, and `roles`. The scripts save `token` as `adminToken`, `merchantToken`, `pendingMerchantToken`, or `rejectedMerchantToken`. Protected management calls send `Authorization: Bearer <token>`.

Do **not** use `/api/v1/auth/register` for merchant setup. It is outside the M9 onboarding flow and has a known role issue. The M9 public registration endpoint is `/api/v1/merchants/register`.

## Workflow order

The Collection Runner follows these folders and requests in order:

1. **Authentication and seeded accounts:** log in as seeded admin and approved merchant; generate a key for the seeded merchant for the later ownership check.
2. **Register and review merchants:** register a uniquely named merchant, log in while it is pending, verify key generation is denied, list pending applications, verify merchant-token access to admin routes is forbidden, verify unauthenticated admin access is rejected, then approve the merchant.
3. **Merchant API-key management:** generate a machine key, list keys (the list must not contain raw keys), generate a second key, and revoke that key.
4. **Machine orders and merchant dashboard:** create one order, retry the exact same request with the same key, create a fresh bulk batch, list orders and read order status using the API key, then read dashboard orders and status totals using the merchant JWT.
5. **Machine API negative cases:** check missing, invalid, and revoked API keys; missing `Idempotency-Key`; and a second merchant trying to read the new merchant's order.
6. **Rejection workflow:** register a separate unique merchant, log in, reject it with a reason, and confirm it cannot generate an API key.

### Authentication and key handling

- Admin review, merchant dashboard, and merchant API-key management endpoints use a JWT bearer token.
- Machine order endpoints use `X-API-Key`; they do not use the JWT for merchant ownership.
- Successful API-key generation returns the raw `apiKey` once. The collection stores it only in the selected local Postman environment for later requests. Key listing returns metadata, not the raw key.
- New merchant registrations begin in `PENDING`. Admin approval/rejection requires an ADMIN token and returns the updated merchant record.

## Create and retry an order

The single-order request is sent to `POST /api/v1/merchant-api/orders` with both headers:

```http
X-API-Key: <raw key captured from successful API-key generation>
Idempotency-Key: <fresh unique value for this order>
Content-Type: application/json
```

Example body (also used by the collection):

```json
{
  "pickup": { "line1": "10 Main Street", "city": "Colombo", "district": "Colombo" },
  "dropoff": { "line1": "20 Lake Road", "city": "Kandy", "district": "Kandy" },
  "receiverName": "Postman Receiver",
  "receiverPhone": "0771234567",
  "size": "M",
  "weightKg": 2.0,
  "externalOrderId": "optional merchant reference"
}
```

A new order returns HTTP `201` with its tracking number. Retry by sending the **same merchant key, same Idempotency-Key, and same body**; the API returns HTTP `200` with the original order. The collection generates a fresh key for each new order and automatically reuses it for the retry. Bulk order creation uses `POST /api/v1/merchant-api/orders/bulk`, the same two headers, and a JSON array of up to 50 order objects.

## Request and response expectations

| Operation | Expected success | Notes |
| --- | --- | --- |
| Login | `200` | Response contains a JWT in `token`. |
| Merchant registration | `201` | Response contains merchant `id` and `status: PENDING`. |
| Admin list, approve, reject | `200` | Review actions return the updated merchant record. Rejection body is `{ "reason": "..." }`. |
| Generate API key | `201` | Response contains one-time `apiKey`, `id`, `keyPrefix`, and metadata. |
| List/revoke API keys | `200` | List omits raw key/hash; revoke response has `active: false`. |
| New single/bulk order | `201` | Single order response includes `trackingNumber`, `status`, `fee`, `createdAt`, and `externalOrderId`; bulk returns an array. |
| Idempotent single-order retry | `200` | Reusing the same merchant key, idempotency key, and body returns the original order. |
| Machine order list/status | `200` | Results are scoped to the API key's merchant. |
| Dashboard order list/summary | `200` | Uses the merchant JWT and returns page data / status-count map. |

Expected negative responses include:

- Missing, invalid, or revoked `X-API-Key`: `401`, error code `INVALID_API_KEY`.
- Missing `Idempotency-Key` on single/bulk create: `400`, code `IDEMPOTENCY_KEY_REQUIRED`.
- Pending/rejected merchant trying to generate a key: `403`, code `MERCHANT_NOT_APPROVED`.
- No JWT on protected admin routes: `401`; a merchant JWT on an admin route: `403`.
- Another merchant requesting an order by tracking number: `404`, code `NOT_FOUND` (ownership is not disclosed).

## Repeat the demo

The registration pre-request scripts generate unique emails and business registration numbers for each run. The create-order scripts generate fresh single-order and bulk idempotency keys. The retry request deliberately reuses the exact single-order key and body created immediately before it.

Run the full collection sequentially again to create another set of demo records. The seeded login accounts are reused. Each run creates two merchant accounts, three API keys (including the seeded merchant key), and orders; the collection does not delete database records. If you only want to replay the idempotency check, run **Create single merchant order** followed by **Retry the same order with the same idempotency key and body** from one run, without rerunning the create request between them.

Never export a Postman environment after adding the password or generated JWT/API-key values. The committed environment file deliberately has blank values for those fields. Clear local token/key values when you finish if the environment is shared.
