# Technical SEO release checklist

This change spans Spring Boot, additive Flyway migrations V24–V35, the React build, and the existing CloudFront Function. Do not deploy only one component. The branch remains a draft until the gates below pass.

## Before release

1. Verify the target AWS account is `007561532703`, profile `website-audit`, and check the active CloudFormation stack and resources against `DOC/AWS-PRODUCTION-RUNBOOK.md`. That runbook uses a different profile name; use `website-audit` for this release.
2. Back up PostgreSQL and confirm restore access. Apply migrations to a copy of Production data first. V24 and V26 add stable slugs; V28, V33 and V35 update only known default content and must leave customized Admin values intact. Keep the pre-release DB backup until acceptance.
3. Confirm that the edge template still matches the deployed stack. Review a CloudFormation Change Set before execution; expected changes are to the existing CloudFront Function and distribution only. Do not create a new distribution, open S3 or alter payOS/GA4.
4. Run `mvn test` in `BE`; run `npm run lint`, `npm run typecheck`, `npm run test`, `npm run e2e`, and `npm run build` in `FE`. Review the diff and confirm the PR contains no secrets or generated test output.
5. Capture the current backend artifact, frontend S3 object versions, CloudFront Function version and distribution configuration for rollback. Use the deployment branch procedure in the existing runbook.

## Release order

Deploy the compatible backend and validate migrations/API/health, then upload the verified FE build to the existing private S3 bucket, then publish the reviewed CloudFront Function/distribution change. Invalidate the distribution after FE and edge changes. Keep the interval between FE and edge updates short: FE `index.html` defaults to `noindex` until the backend serves indexable public HTML. Do not route crawler URLs to the backend before the backend is healthy.

The backend serves public SEO HTML and a dynamic sitemap from live catalog, Blog, presentation, support and SEO metadata data. It fetches the current FE `index.html` shell and caches that shell for up to 60 seconds. HTML and sitemap responses request no-cache. The CloudFront managed default cache policy may retain a response for its minimum TTL; verify the actual distribution response headers and freshness after release. Admin content updates do not require an application build.

## Acceptance

Check HTTP status and initial response body with GET, before JavaScript: `/`, all six `/san-pham/{collection-slug}` routes, an active and a hidden product, `/products/{id}` 301, `/blog` and each draft/scheduled/published article, `/faq`, `/chinh-sach`, `/sitemap.xml`, and a nonexistent path 404. Confirm www 301 preserves path and query, one canonical and H1 per indexable page, expected title/description/OG and valid Product/FAQ JSON-LD. Verify Admin save/read for SEO, product, collection, Blog, FAQ and policy; confirm visible changes reach initial HTML and sitemap. Smoke test login, catalog, cart, checkout without real payment, workshop, Admin and GA4 consent. Blog article 2 must remain private before 1 December 2026.

## Rollback

If routing or SEO HTML fails, restore the previous CloudFront Function/distribution first, then restore the previous FE objects and backend artifact using the runbook. The new migrations are additive; do not drop the new columns/tables during an emergency rollback. The previous backend artifact must be checked against the migrated schema in staging before Production release. Restore the DB backup only if a data-corrupting migration is proven, with a separate impact review because restoring it discards writes made since backup.

The approved policy copy contains a payOS payment sentence. It does not activate payOS; verify the wording against the actual Production payment flow before acceptance.
