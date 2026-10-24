# Analytics and Business QA — 2026-10-24

## Scope and source checks

- GA4 Property `560090227`, stream `16179028870`, Measurement ID `G-BB79X513WR` confirmed through Analytics Admin API using the Production WIF identity. Metadata: 95 metrics, 375 dimensions. Realtime API directly returned 1 active user, 4 page views, 7 events, 0 key events; individual page/event/device/country combinations returned HTTP 200. Core API returned HTTP 200 with zero rows for the completed-day period. The Dashboard presents that as `EMPTY`.
- The user independently verified ordinary Edge `g/collect` HTTP 204 for consented `page_view` and GA4 UI Realtime 1 active user/4 page views. The prior automated Chromium canceled request was not reproduced in Edge.
- Business queries were tested against an isolated PostgreSQL 16 Testcontainers database, never Production. Revenue includes only completed orders with a paid payment, excludes cancelled/expired orders, and preserves VND as decimal strings. API authorization checks cover Admin, USER and anonymous access.

## Local automated gates

| Gate | Result |
|---|---|
| `FE: npm run lint` | Pass |
| `FE: npm run typecheck` | Pass |
| `FE: npm run test -- --run` | 58 passed, 0 failed |
| `FE: npm run build` | Pass |
| `FE: npx playwright test` | 54 passed, 0 failed; desktop Chromium and mobile 390 px, including Analytics tabs, consent, purchase replay guard, checkout and QA-02B Chat |
| `BE: mvn -q test` | 250 passed, 0 failed, 0 skipped on Java 21 and isolated PostgreSQL 16 |
| `BE: mvn -q -DskipTests package` | Pass; Spring Boot JAR built |

The Backend test and build gates were repeated after the final Workshop breakdown change and passed. UI E2E for the Analytics tabs was repeated after that change and passed 2/2. The Frontend build also passed after the change.

## Known limits

- Historical GA4 Core has no observed Production rows yet; comparisons, cohorts and historical conversion trends cannot be proven with live data. The Property was created on 2026-10-22.
- `purchase` is gated on a paid order and deduplicated in the same browser storage. It is not exactly once across different devices, cleared storage or parallel browser contexts.
- Workshop revenue, marketing ROI, net revenue, inventory history and an ordered ecommerce funnel have no reliable source in the current system; no synthetic values are shown.
- Production API/UI acceptance remains pending deployment; this report does not mark it passed.
