# GA4 tracking and release checklist

## Events

The storefront uses `FE/src/analytics/Analytics.jsx` and loads `gtag.js` only after a visitor grants analytics consent. SPA navigation emits `page_view`; product detail, cart and checkout retain `view_item`, `add_to_cart`, and `begin_checkout`. The additions are:

| Event | Trigger | Parameters | Key event candidate |
| --- | --- | --- | --- |
| `generate_lead` | Workshop booking API resolves successfully; this is a request, not staff confirmation | `lead_type: workshop_booking` | Yes |
| `order_created` | Checkout API resolves with an order; this is not payment | `checkout_type: cart` or `buy_now` | Optional |
| `purchase` | Owned order detail reports `paymentStatus: PAID` | `transaction_id`, `currency: VND`, `value`, product `items` | Yes |
| `click_zalo` | Visitor clicks a `zalo.me` link | None | Optional |
| `click_phone` | Visitor clicks a `tel:` link | None | Optional |

The existing `purchase` event and `order_created` use per-order localStorage markers. GA4 receives no booking form content, contact number, customer identity, address or checkout note. A click indicates intent, not a completed call or conversation. The browser events are best effort: an ad blocker, denied consent, or a customer who does not return to the paid order detail can prevent transmission. For purchase coverage independent of return visits, add a server-side Measurement Protocol sender only after its credentials, consent handling and transaction deduplication are designed and approved.

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
