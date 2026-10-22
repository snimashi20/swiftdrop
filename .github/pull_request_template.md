## What does this PR do?

<!-- One or two sentences. Which module, which task card item? -->

**Member:** M<!-- number -->
**Task card item:** <!-- e.g. "[BE] RabbitMQ as code in core-api (Oct 28)" -->
**Closes:** <!-- #issue, or "-" -->

## How to test it

<!-- Steps the reviewer can follow: which endpoint, which page, what to expect. -->

1.
2.

## Checklist

- [ ] I described what this PR does and how to test it
- [ ] Tests added or updated for this change, and `mvn test` / `npm test` passes locally
- [ ] **No secrets committed** — no passwords, API keys, tokens or the Google client
      secret; new variables are in `.env.example` (names only) and read from env
- [ ] Target branch is `develop` (not `main`)
- [ ] Follows the code rules: `/api/v1/` URLs, DTOs not entities, `@Valid` on request
      bodies, package `com.swiftdrop.<module>`
- [ ] Shared contracts unchanged (roles, parcel statuses, event fields, queue names,
      WebSocket topic, error JSON) — or the group was told and agreed
- [ ] New Flyway migration follows `V<yyyyMMddHHmm>__<module>_<what>.sql` and does not
      reuse an existing version number
- [ ] Frontend only: works on a phone screen (checked in Chrome's mobile view)
- [ ] My review partner is assigned as reviewer

## Screenshots

<!-- For any UI change. Delete this section if not relevant. -->
