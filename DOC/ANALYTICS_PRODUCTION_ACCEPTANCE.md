# Analytics Production acceptance — 2026-10-24

| Check | Result | Evidence / limit |
|---|---|---|
| GA4 browser collection after consent | PASS | User verified ordinary Edge `g/collect` `page_view` HTTP 204 for `G-BB79X513WR`; GA4 UI Realtime Property `560090227` showed 1 active user/4 page views. This was before the V2 bundle; GA4 initialization, consent and Measurement ID were unchanged in V2. |
| No collection before consent or on Admin | PARTIAL | New Production bundle emitted no `g/collect` before consent in 1440/390 px browser smoke. Admin exclusion is covered by existing E2E and unchanged initialization; no authenticated Admin browser session was available for live retest. |
| GA4 Property mapping and Backend WIF | PASS | Post-restart Admin API and Metadata HTTP 200; stream `16179028870` maps to `G-BB79X513WR`; `runRealtimeReport` and `runReport` HTTP 200. Later Realtime window had 0 rows and Core historical 0 rows, both valid empty results. |
| Backend service and schema | PASS | JAR checksum verified; systemd active, health 200, Flyway V22 success, no failed migration, public product API 200, Admin routes 401 without authentication. |
| Business source query | PASS | Exact read-only aggregate SQL executed successfully on Production PostgreSQL. Local integration suite validates paid/completed exclusions, monetary strings and 401/403 boundaries. |
| Authenticated Admin Analytics API response | PENDING | Requires a genuine Admin session; no Production Admin token was present in the Codex environment. Anonymous request returned 401 as expected. |
| Five Admin tabs and VI/EN in Production | PENDING | Local desktop/mobile E2E passed with mocked API. Live authenticated UI must be checked by an Admin. |
| Frontend/Chat/public site | PASS | CloudFront invalidation complete; HTML/JS/CSS SHA match; home/product HTTP 200; desktop/mobile Chat layout valid; no page errors or pre-consent GA request. |

Current verdict: **PARTIALLY COMPLETE**. Production deployment is active and safe smoke checks passed; full authenticated Admin acceptance is pending. Historical GA4 data is genuinely empty for the completed periods observed, so the Dashboard should show an empty state rather than fabricated zero traffic.
