# Final Remediation Report - 2026-08-17

## 1. Executive Status

Implementation status: `SOURCE REMEDIATED`

Retest status: `INDEPENDENT RETEST PENDING`

PAYOS: `EXTERNAL CONTRACT CHECK PENDING`

DEPLOYMENT: `PENDING`

This report reflects source edits after the latest remediation pass. No tests, lint, typecheck, build, E2E, Docker startup, backend startup, or runtime smoke commands were executed during this pass.

## 2. Frozen Change Decisions

- CD-001: Frontend remains React + Vite + React Router + TanStack Query + Zod.
- CD-002: Workshop, Workshop Booking, and Support Settings remain in scope.
- CD-003: Shipping remains out of scope. No shipping fee is shown or included.
- CD-004: payOS external contract remains pending. Local code keeps mocked/provider-boundary separation.

## 3. Remediation Matrix

| Item | Implementation status | Retest status | Notes |
| --- | --- | --- | --- |
| Admin Product / Collection admin API | SOURCE REMEDIATED | INDEPENDENT RETEST PENDING | Admin catalog screens use admin endpoints, not public Store catalog. |
| Admin Product / Collection backend query | SOURCE REMEDIATED | INDEPENDENT RETEST PENDING | Admin product/collection query support remains database-backed in source. |
| Checkout idempotency terminal failure | SOURCE REMEDIATED | INDEPENDENT RETEST PENDING | Terminal `PAYOS_REQUEST_FAILED` clears frontend checkout idempotency key; ambiguous states retain it. |
| Admin Users / Orders pagination | SOURCE REMEDIATED | INDEPENDENT RETEST PENDING | Admin user/order screens use backend query params and page controls. |
| Admin Inventory pagination/filter/sort | SOURCE REMEDIATED | INDEPENDENT RETEST PENDING | Main inventory list sends `keyword`, `status`, `page`, `size`, and `sort`; search submit applies keyword. |
| Admin Notifications pagination/count | SOURCE REMEDIATED | INDEPENDENT RETEST PENDING | Notification management uses pageable list params and a separate unread count query using `totalElements`. |
| Product collection selector over 100 collections | SOURCE REMEDIATED | INDEPENDENT RETEST PENDING | Product form/filter collection options load all admin collection pages until `last=true`. |
| Business timezone | SOURCE REMEDIATED | INDEPENDENT RETEST PENDING | Business display and discount datetime conversion are centralized around `Asia/Ho_Chi_Minh`. |
| Public i18n VI/EN | SOURCE REMEDIATED | INDEPENDENT RETEST PENDING | Public cart, checkout, orders, product detail, collections, workshop, support, FAQ/policy, and auth-facing messages use the existing `pick(lang, vi, en)` convention where touched. |
| Admin i18n VI/EN | SOURCE REMEDIATED | INDEPENDENT RETEST PENDING | Admin shell, dashboard, reports, products, collections, workshops, inventory, orders, users, notifications, and support settings now use the existing admin text helper for user-facing labels/messages where touched. |
| Local hostname consistency | SOURCE REMEDIATED | INDEPENDENT RETEST PENDING | Browser-facing local defaults use `127.0.0.1:3000` for frontend and `127.0.0.1:8080` for backend; Docker database service networking is preserved. |
| E2E fixture enum cleanup | SOURCE REMEDIATED | INDEPENDENT RETEST PENDING | Inventory mock status was aligned to backend inventory status values. |
| Playwright reporting clarity | DOCUMENTED | INDEPENDENT RETEST PENDING | Browser suite should be described as mock-backed UI contract coverage, not full-stack Spring/PostgreSQL E2E. |
| Typecheck reporting clarity | DOCUMENTED | INDEPENDENT RETEST PENDING | `tsc --noEmit` command exists, but JS semantic checking remains limited while `checkJs=false`. |
| FINAL-FIX-17 Complete i18n | SOURCE REMEDIATED | INDEPENDENT RETEST PENDING | Checkout validation, generic API fallbacks, audited backend error-code mappings including checkout/cancel/refund edge cases, and admin inventory product names support VI/EN via existing language helpers. |
| FINAL-FIX-18 Admin Workshop Booking pagination | SOURCE REMEDIATED | INDEPENDENT RETEST PENDING | Workshop booking admin list uses backend `status`/`page`/`size` pagination with Previous/Next controls; status updates reset to page 0. |

## 4. Backend Retest Needed

Not executed in this remediation pass.

Required independent checks:

- Maven test lifecycle.
- Flyway/Testcontainers migration and repository regression.
- Admin catalog/order/inventory query regression.
- Security and concurrency regression.

## 5. Frontend Retest Needed

Not executed in this remediation pass.

Required independent checks:

- `npm test`
- `npm run lint`
- `npm run typecheck`
- `npm run build`
- `npm run e2e`

## 6. Playwright Coverage Note

The configured Playwright suite runs scenarios across Desktop Chromium and Mobile Chromium projects. Report scenario count separately from project executions. If a test file has 8 scenarios and 2 projects, describe that as 8 mock-backed scenarios executed across 2 browser projects, not 16 independent full-stack scenarios.

The current E2E suite uses mocked API routing through `page.route('**/api/v1/**', ...)`. It should be documented as mock-backed browser UI/API-contract coverage, not as React-to-Spring-Boot-to-PostgreSQL full-stack coverage.

## 7. Typecheck Note

Frontend `tsconfig.json` keeps:

```json
{
  "allowJs": true,
  "checkJs": false
}
```

Therefore `tsc --noEmit` is available as a command, but JavaScript semantic checking is intentionally limited unless `checkJs` is enabled later in a separately retested change.

## 8. Known Limitations

- Real payOS sandbox/provider contract check remains external.
- AWS deployment and S3 production smoke check remain external.
- This pass did not execute automated or runtime commands by instruction.

## 9. Retest Checklist

| Area | Retest required |
| --- | --- |
| Backend tests | YES |
| Frontend tests | YES |
| Frontend lint | YES |
| Frontend typecheck | YES |
| Frontend build | YES |
| Playwright | YES |
| Docker local startup | YES |
| payOS sandbox/provider contract check | YES, external |
| AWS deployment smoke | YES, external |

## 10. Latest Fix Entries

### FINAL-FIX-17 — Complete i18n

Implementation:
SOURCE REMEDIATED

Changes:
- Checkout validation supports VI/EN.
- Generic API fallbacks support VI/EN.
- All audited user-facing Backend error codes have VI/EN mappings.
- Audited user-facing Backend error codes now include mappings for:
  - `CHECKOUT_NOT_FOUND`
  - `ORDER_CANCELLATION_NOT_ALLOWED`
  - `REFUND_NOT_ALLOWED`
- Inventory product name follows current language.

Verification:
INDEPENDENT RETEST PENDING

### FINAL-FIX-18 — Admin Workshop Booking pagination

Implementation: `SOURCE REMEDIATED`

Verification: `INDEPENDENT RETEST PENDING`
