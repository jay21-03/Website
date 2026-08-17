# Final Remediation Report - 2026-08-17

## 1. Executive Status

Implementation status: `SOURCE REMEDIATED`

Verification status: `PENDING INDEPENDENT RETEST`

PAYOS: `EXTERNAL VERIFICATION PENDING`

DEPLOYMENT: `PENDING`

This report reflects source changes after the latest remediation pass. No tests, lint, typecheck, build, E2E, Docker startup, backend startup, or runtime smoke verification were executed during this pass.

## 2. Frozen Change Decisions

- CD-001: Frontend remains React + Vite + React Router + TanStack Query + Zod.
- CD-002: Workshop, Workshop Booking, and Support Settings remain in scope.
- CD-003: Shipping remains out of scope. No shipping fee is shown or included.
- CD-004: payOS external contract is pending. Local code keeps mocked/provider-boundary separation.

## 3. Remediation Matrix

| Item | Implementation status | Verification status | Notes |
| --- | --- | --- | --- |
| Admin Product / Collection admin API | SOURCE REMEDIATED | PENDING INDEPENDENT RETEST | Admin catalog screens use admin endpoints, not public Store catalog. |
| Admin Product / Collection backend query | SOURCE REMEDIATED | PENDING INDEPENDENT RETEST | Admin product/collection query support remains database-backed in source. |
| Checkout idempotency terminal failure | SOURCE REMEDIATED | PENDING INDEPENDENT RETEST | Terminal `PAYOS_REQUEST_FAILED` clears frontend checkout idempotency key; ambiguous states retain it. |
| Admin Users / Orders pagination | SOURCE REMEDIATED | PENDING INDEPENDENT RETEST | Admin user/order screens use backend query params and page controls. |
| Admin Inventory pagination/filter/sort | SOURCE REMEDIATED | PENDING INDEPENDENT RETEST | Main inventory list sends `keyword`, `status`, `page`, `size`, and `sort`; search submit applies keyword. |
| Admin Notifications pagination/count | SOURCE REMEDIATED | PENDING INDEPENDENT RETEST | Notification management now uses pageable list params and a separate unread count query using `totalElements`. |
| Product collection selector over 100 collections | SOURCE REMEDIATED | PENDING INDEPENDENT RETEST | Product form/filter collection options load all admin collection pages until `last=true`. |
| Business timezone | SOURCE REMEDIATED | PENDING INDEPENDENT RETEST | Business display and discount datetime conversion are centralized around `Asia/Ho_Chi_Minh`. |
| Public/Admin i18n gaps | SOURCE REMEDIATED | PENDING INDEPENDENT RETEST | Key public checkout/order/workshop/support strings and touched admin inventory/notification labels use existing `pick`/admin text convention. |
| Local hostname consistency | SOURCE REMEDIATED | PENDING INDEPENDENT RETEST | Browser-facing local docs/config use `127.0.0.1`; Docker database service networking is preserved. |
| Playwright reporting clarity | DOCUMENTED | PENDING INDEPENDENT RETEST | Browser suite should be described as mock-backed UI contract coverage, not full-stack Spring/PostgreSQL E2E. |
| Typecheck reporting clarity | DOCUMENTED | PENDING INDEPENDENT RETEST | `tsc --noEmit` command exists, but JS semantic checking remains limited while `checkJs=false`. |

## 4. Backend Verification

Not executed in this remediation pass.

Required independent checks:

- Maven test lifecycle.
- Flyway/Testcontainers migration and repository regression.
- Admin catalog/order/inventory query regression.
- Security and concurrency regression.

## 5. Frontend Verification

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

Therefore `tsc --noEmit` is available as a command, but JavaScript semantic checking is intentionally limited unless `checkJs` is enabled later in a separately verified change.

## 8. Known Limitations

- Real payOS sandbox/provider verification remains external.
- AWS deployment and S3 production smoke verification remain external.
- This pass did not execute automated or runtime verification by instruction.

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
| payOS sandbox verification | YES, external |
| AWS deployment smoke | YES, external |
