# GA4 measurement and release checklist

## Events

The storefront uses `FE/src/analytics/Analytics.jsx` and loads one direct Google tag only after a visitor grants analytics consent. `AnalyticsRoot` owns SPA `page_view`; components reuse `trackAnalytics()`. PostgreSQL remains authoritative for orders, payments, workshops and refunds. The backend GA4 Data API reports visitor data through the existing numeric `GA4_PROPERTY_ID`. All browser events require consent and exclude Admin/preview.

| Event | Trigger | Parameters | Key event candidate |
| --- | --- | --- | --- |
| `page_view` | Initial public route and changed SPA route, including back/forward; ordinary query edits ignored | Safe `page_path`, `page_location`, `page_title` | No |
| `view_item_list` | Loaded visible homepage featured, catalog or related list | `item_list_id`, actual product `items` | No |
| `select_item` | Product card link clicked | `item_list_id`, selected `items` | No |
| `view_item` | Product detail data loaded | `currency`, `value`, `items` | No |
| `add_to_cart` | Guest cart changed or authenticated cart API succeeded | `currency`, `value`, `items` | No |
| `view_cart` | Populated cart first displayed on a route visit | `currency`, `value`, `items` | No |
| `remove_from_cart` | Successful reduction or deletion, using actual quantity removed | `currency`, removed `value`, `items` | No |
| `begin_checkout` | Nonempty checkout displayed | `currency`, `value`, `items` | No |
| `generate_lead` | Workshop booking API resolves successfully; this is a request, not staff confirmation | `lead_type: workshop_booking` | Yes |
| `order_created` | Checkout API resolves with an order; this is not payment | `checkout_type: cart` or `buy_now` | Optional |
| `purchase` | Owned order detail reports `paymentStatus: PAID` | `transaction_id`, `currency: VND`, `value`, product `items` | Yes |
| `click_zalo` | Visitor clicks a `zalo.me` link | None | Optional |
| `click_phone` | Visitor clicks a `tel:` link | None | Optional |
| `view_search_results` | Debounced catalog query succeeds, including zero results, with a permitted search term | `search_term` | No |
| `filter_products` | Visitor chooses a collection or finishes editing a price range | `filter_type`, low-cardinality `filter_value` | No |
| `sort_products` | Visitor selects a different sort value | `sort_order` | No |
| `select_content` | Visitor selects a workshop offering | `content_type: workshop`, `content_id: workshop_<id>` | No |
| `chat_open` | Visitor opens the chat panel | None | No |
| `chat_message_sent` | Chat send API accepts a message | None; never chat text | No |
| `login` | Google login API authenticates a customer | `method: Google` | No |

The existing `purchase` event and `order_created` use per-order localStorage markers; list/cart/checkout events use per-view refs or loaded-list keys; search uses the last completed term; other events fire only on actual successful actions. `items` use API product ID, Vietnamese product name, actual price/quantity and a collection name only when available. Monetary `value` is VND and always has `currency: VND`. GA4 receives no booking form content, contact number, customer identity, address or checkout note. A click indicates intent, not a completed call or conversation. Browser events are best effort: an ad blocker, denied consent, or a customer who never returns to paid order detail can prevent transmission. Server-side Measurement Protocol needs a separately approved consent, attribution, credential and deduplication design.

`page_view`, ecommerce events, `generate_lead`, `login` and `select_content` are Google documented names; `view_search_results` is an Enhanced Measurement name emitted explicitly here. `order_created`, `filter_products`, `sort_products`, `click_zalo`, `click_phone`, `chat_open` and `chat_message_sent` are custom. All are frontend-owned; API responses and backend statuses are their source of truth for successful operations. Verify each row with a consented test action, inspect the event and typed parameters in DebugView/Tag Assistant, and compare with the corresponding Vitest or Playwright assertions. `add_shipping_info`/`add_payment_info` are omitted because checkout has no distinct completed steps; `refund` and `qualify_lead` are omitted because those changes happen in admin/backend workflows without a consent-linked customer browser event. Their authoritative counts belong to business reporting.

Admin sessions, `/admin` and `presentationPreview=1` are excluded. Declining or revoking consent stops new events. Consent can be changed with the storefront privacy-settings button. Do not enable GTM `GTM-NPD4BLH3` alongside direct gtag events without first mapping consent and disabling duplicate tags/page views.

## Property configuration

**A — selected:** Keep `VITE_GA4_MEASUREMENT_ID=G-BB79X513WR` in the frontend production build and retain the backend's numeric `GA4_PROPERTY_ID`. The measurement ID is not the numeric property ID. The property owner reports that `dangxembautrucpottery@gmail.com` has Administrator access and `ga4-report-reader@webgom.iam.gserviceaccount.com` has Viewer access on *Dang Xem - Production*. The backend reporting service uses Application Default Credentials with the `analytics.readonly` scope; confirm the Google Analytics Data API is enabled in its Google Cloud project.

**B — fallback, approval required:** Change the frontend build variable to `VITE_GA4_MEASUREMENT_ID=G-DDPBNLMNZX`; set backend `GA4_PROPERTY_ID` to the *numeric* property ID of that GA4 property; enable the Data API and grant the backend service account property read access. Rebuild and redeploy frontend for the Vite build-time variable, then restart the existing backend with the new environment variable. This starts a new data series; old property history does not migrate. Roll back by restoring both former variables, rebuilding/redeploying the frontend and restarting the backend. Do not publish both direct GA4 and GTM tags for the same property.

## Release and verification

Use `DOC/AWS-PRODUCTION-RUNBOOK.md` for the existing frontend S3/CloudFront procedure and backend EC2 procedure. For this frontend-only change, after approval: verify the target branch and production health, set the approved `VITE_GA4_MEASUREMENT_ID` for the build, run `npm ci`, `npm run lint`, `npm run typecheck`, `npm run test`, `npm run e2e` and `npm run build` from `FE`, then follow runbook section 6 to resolve the actual S3 bucket/distribution from CloudFormation, upload `FE/dist` and invalidate CloudFront. The runbook uses the `website-deploy` profile; verify identity/account and use the authorized profile for the actual release. No backend artifact or database migration is required for these code changes. Keep the prior frontend artifact or source commit for rollback per the runbook.

Manual acceptance after release:

1. With no consent, then declined consent, confirm no GA4 script/event is sent; allow consent and confirm a SPA `page_view`. Revoke and confirm further events stop.
2. In GA4 Realtime/DebugView and Tag Assistant, check one `generate_lead` after a successful workshop request, no event after a rejected request, one `order_created` after checkout, and no `purchase` while payment is pending.
3. Confirm `purchase` only after the owned order detail shows `PAID`, with the expected transaction ID and no duplicate after refresh. Check `view_item`, `add_to_cart`, and `begin_checkout` still occur.
4. Click actual phone and Zalo links on desktop/mobile and check their event names while navigation still works. Check admin and presentation preview emit nothing.
5. Open the admin analytics report under the chosen property and confirm its service account can read data. GA4 Realtime, DebugView and Data API may differ in processing delay; record the property ID and release SHA used for acceptance.

## Correctness and privacy release gates

`gtag('config', ..., {send_page_view:false})` suppresses the tag's initial config view, while the application sends one manual `page_view` per route. **The GA4 owner must inspect the production web stream's Enhanced Measurement settings before release:** disable the *advanced browser-history page-change* option for page views, since it can add another `page_view` on `pushState`/`popState`/`replaceState`; do not disable the entire Enhanced Measurement feature. Disable Enhanced Measurement **Site search** while this code emits `view_search_results`, because a landing URL containing `keyword` can otherwise duplicate it. Disable Enhanced Measurement **outbound clicks** if its `link_url` would expose a Zalo phone number; the app's own contact events omit destination URLs. Review automatic form interactions and do not mark generic `form_submit` as a workshop conversion. These settings have not been changed or verified by this PR. Confirm real `g/collect` payloads in Tag Assistant, since `dataLayer` tests cannot prove stream-side settings.

The code's `page_location` includes only known public paths and allowlisted `utm_source`, `utm_medium`, `utm_campaign`, `utm_content`, `utm_term`, `gclid`, `gbraid` and `wbraid` values; unknown paths become `/other` and order IDs become `/orders/detail`. `page_referrer` is origin-only. Search terms resembling email, URL or long digit strings are skipped; no heuristic can identify every personal name, so campaign creators must not put PII in links. In **GA4 Admin → Data streams → web stream → Redact data**, verify email redaction and add sensitive query keys used by the site (for example `email`, `phone`, `token`, `code`, `credential`), then test the redaction preview. This is defense in depth against Enhanced Measurement. Review data-retention settings against the organization's policy; do not change them without a decision. Analytics errors, missing Google script or blocked storage must not interrupt shopping, booking or chat.

## Marketing naming and reporting

Use lowercase stable UTM values without customer data: `utm_source` identifies publisher (`facebook`, `zalo`, `google`), `utm_medium` identifies channel (`paid_social`, `social`, `cpc`), `utm_campaign` identifies the initiative, `utm_content` identifies a creative, and `utm_term` is for a non-personal search term when applicable. Example manual links:

- Facebook: `https://dangxembautruc.com/workshop?utm_source=facebook&utm_medium=paid_social&utm_campaign=workshop_nov2026&utm_content=video_a`
- Zalo: `https://dangxembautruc.com/workshop?utm_source=zalo&utm_medium=social&utm_campaign=workshop_nov2026&utm_content=oa_post`
- Google: `https://dangxembautruc.com/products?utm_source=google&utm_medium=cpc&utm_campaign=pottery_nov2026&utm_term=bau_truc_pottery`

If Google Ads auto-tagging is in use, reconcile manual UTMs with existing attribution settings before distributing links. No historical campaign data is inferred by these examples.

In **GA4 Admin → Data display → Key events**, inspect existing settings and mark `purchase` and `generate_lead` if absent; do not make `order_created`, chat or contact clicks key events without an explicit business definition. The recommended minimal custom definition is **Checkout type** → `checkout_type` → event scope if cart-versus-buy-now reporting is needed. `item_list_id`, `item_category`, `search_term`, UTM data and currency are standard data; do not register them again. Defer `lead_type` (one current value), `filter_type` and `sort_order` unless a specific report needs them. These UI actions are manual; no quota is consumed by this PR.

Build GA4 Explorations manually after event validation. **Ecommerce:** `view_item_list` → `select_item` → `view_item` → `add_to_cart` → `view_cart` → `begin_checkout` → `order_created` → `purchase`; use an *open* funnel for buy-now paths that skip cart, segment by session source/medium, campaign, device and `item_list_id`. **Workshop:** `page_view` filtered to `/workshop` or `/trai-nghiem` → `select_content` with `content_type=workshop` → `generate_lead` with `lead_type=workshop_booking`; requests without a selected offering skip the middle step. **Contact:** landing `page_view` → product `view_item` or workshop page → `click_zalo`, `click_phone` or `chat_message_sent`. Consent, blockers, cross-device journeys and returnless payments make these partial behavioral funnels, not accounting totals. No Exploration has been created by this PR.

The existing Admin Realtime/Historical reports already show GA4 visitors, channels, pages, events and key events through the Data API. The existing PostgreSQL business report covers paid orders and workshop states. Label GA4 revenue and conversions as consent-dependent estimates; reconcile finance against backend `PAID` totals. A future Looker Studio dashboard could show GA4 users/sessions/source-medium/campaign/funnel counts next to clearly labeled backend paid-order/revenue/workshop counts, but an approved read-only backend source is not connected yet, so no combined dashboard exists.

Search Console linkage is optional manual setup: verify ownership of `dangxembautruc.com` in Search Console, then in **GA4 Admin → Product links → Search Console Links → Link** select the verified property and production stream, review and submit. This requires a verified Search Console owner and GA4 Editor or higher. Review internal/developer traffic exclusions only if staff/test visits distort public reports; Admin routes are already excluded in code. Review unwanted referrals if payOS return traffic appears as a new referral. Google Ads linking is justified only if paid ads are actually operated. Audiences, BigQuery export, GTM and new cloud billing are outside this release; none is enabled here.

## Data-quality acceptance and rollback

Before release, record the frontend SHA, numeric property ID, GA4 stream settings, test devices and date. Verify no analytics before consent or after decline/withdrawal; one safe page view on initial route, SPA/back/forward and valid UTM landing; no email/token/phone in any GA payload; list/product/cart/checkout/order/paid-purchase payloads and quantities; empty-result and PII-looking searches; filters/sort; workshop selection/request; chat and contact clicks; customer login; admin/preview exclusion; and backend report read access. Compare Realtime/DebugView with browser Tag Assistant and backend facts. Production acceptance remains pending until those manual checks pass.

The frontend-only release uses `DOC/AWS-PRODUCTION-RUNBOOK.md` section 6 after approval: verify the actual AWS account/profile/resources and current health, keep `VITE_GA4_MEASUREMENT_ID=G-BB79X513WR`, run `npm ci`, `npm run lint`, `npm run typecheck`, `npm run test`, `npm run e2e`, `npm run build` from `FE`, upload `dist` to the resolved existing bucket and invalidate its CloudFront distribution. Keep backend `GA4_PROPERTY_ID`, credentials and database unchanged. Roll back by rebuilding the previous known-good frontend commit/artifact and following the runbook's frontend rollback/invalidation procedure. No deployment, GA4 UI change or Google integration has been executed here.

Official references: [ecommerce](https://developers.google.com/analytics/devguides/collection/ga4/ecommerce), [recommended events](https://developers.google.com/analytics/devguides/collection/ga4/reference/events), [validation](https://developers.google.com/analytics/devguides/collection/ga4/validate-ecommerce), [Enhanced Measurement](https://support.google.com/analytics/answer/9216061), [custom dimensions](https://support.google.com/analytics/answer/14240153), [data redaction](https://support.google.com/analytics/answer/13544947), [key events](https://support.google.com/analytics/answer/12946393), [Search Console](https://support.google.com/analytics/answer/10737381).
