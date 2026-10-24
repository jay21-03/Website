# Analytics and business reporting architecture

## Sources and boundaries

- Website activity comes only from GA4 Property `560090227`. Admin API confirms web stream `16179028870` uses Measurement ID `G-BB79X513WR`. The browser Google tag is gated by the existing Consent Mode v2 choice and excludes Admin and presentation preview routes. The Backend reads GA4 with the existing external-account ADC/WIF identity; no key file is introduced.
- Store business data comes only from PostgreSQL. Revenue is the current value of orders with `COMPLETED` status and one linked `PAID` payment, grouped by `orders.completed_at`. `payments.order_id` is UNIQUE from Flyway V5. Workshop bookings do not establish collected Workshop revenue. Order and payment status breakdowns describe current states, not historical status snapshots.
- GA4 and database counts have different populations, delays, and date bases. The UI labels their sources and periods separately. It does not divide database orders by GA4 sessions to claim a conversion rate.

## API contracts

All routes use the existing `ApiResponse` envelope and the existing ADMIN-only Spring Security boundary. Requests from unauthenticated users receive 401; authenticated non-Admin users receive 403. No user identifiers, guest tokens, message bodies, payment secrets, or Google credentials enter reporting payloads.

| Route | Data and period | Cache |
|---|---|---|
| `GET /api/v1/admin/analytics` | Existing GA4 Core report, previous 30 completed days through yesterday in the Property timezone | 10 minutes |
| `GET /api/v1/admin/analytics/historical?days=7\|30\|90` | Same Core report contract, selected completed-day range; daily trend, channel/source, page, device, browser, OS, country and event breakdowns; equivalent preceding-period comparison | 10 minutes per range |
| `GET /api/v1/admin/analytics/realtime` | GA4 last 30 minutes: whole-window active users, page views, events and key events; 30 minute positions, page titles, events, devices and countries | 25 seconds |
| `GET /api/v1/admin/analytics/business?days=7\|30\|90` | PostgreSQL business interval including today, `[start,endExclusive)` in `Asia/Ho_Chi_Minh`; orders, payments, revenue, products, customers, Workshop, reviews and operations | No API cache; read-only transaction |

Realtime payloads include `source=GA4_REALTIME`, `period=LAST_30_MINUTES`, `status=LIVE|EMPTY`, `fetchedAt`, `metrics`, `trend`, `pages`, `events`, `devices`, and `countries`. `trend.activeUsers` is a per-minute unique count and is never summed to produce the whole-window unique count. The pages dimension is `unifiedScreenName`, so the UI labels it as a title, not a URL. Empty windows carry `status=EMPTY`; Google API errors return non-2xx and are not converted to zero.

Historical payloads retain existing metric fields and add `source=GA4_CORE`, `status=READY|EMPTY`, `timezone`, `fetchedAt`, `thresholded`, `otherRows`, `comparison`, and breakdown arrays. If either comparison period has no rows, its status is `UNAVAILABLE`. Growth is undefined when the previous value is zero. A zero-row Core response is displayed as an empty report rather than a claim of zero website visitors. The Property was created on 2026-10-22; historical processing is still being observed.

Business money values `completed_paid_revenue`, `paid_payment_amount`, `refunded_payment_amount`, `average_order_value`, and `revenueTrend[].revenue` are decimal strings in VND. The UI formats them with `BigInt`, avoiding JavaScript safe-integer loss. The Business API also includes `period`, `timezone`, `dateBasis`, `limitations`, `metrics`, `revenueTrend`, `bestSelling`, and `workshopInterest` grouped by offering (or custom request). Average order value is unavailable when there is no completed paid order. Visible average rating is unavailable with no visible reviews.

## Cache, polling and failures

- Each Backend instance uses separate in-memory locks for Realtime and historical reports. A cache miss is single-flight within its domain. Realtime expires after 25 seconds; historical results expire after 10 minutes. Business reads are not cached.
- The Realtime UI schedules its next request 30 seconds after the prior response. It aborts outstanding work and clears its timer when hidden or unmounted, and restarts on visibility. Historical and Business tabs fetch on entry, range change, or manual refresh; they do not poll.
- An invalid range is rejected. GA4 permission, quota and invalid report responses have distinct error messages. A failed refresh does not leave stale data presented as Live. The existing `/api/v1/admin/analytics` route remains available for current consumers.

## Supported and unsupported interpretation

- The browser emits `view_item`, `add_to_cart`, `begin_checkout`, and `purchase` only after Analytics consent. `purchase` requires a `PAID` order and uses the stable order code as `transaction_id`; a persistent same-browser marker prevents replay on reload or a new tab. This is not an exactly-once guarantee across devices, cleared storage or concurrent browser contexts.
- Other recommended ecommerce events have not been added without a corresponding confirmed user action. Aggregated event counts are not an ordered conversion funnel. GA4 Core historical reports may remain empty while newer Realtime events are visible. No paid Workshop revenue, historical stock trend, marketing ROI or net revenue is inferred from the available data.

Metric definitions and Production observations are recorded in [ANALYTICS_METRIC_CATALOG.md](ANALYTICS_METRIC_CATALOG.md).
