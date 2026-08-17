# Change Decisions - 2026-08-17

These change decisions supersede only the conflicting parts of `02_SRS_SPEC`, `03_SYSTEM_ARCHITECTURE`, and `04_IMPLEMENTATION_SPEC`.
All other frozen business invariants remain unchanged.

## CD-001 - Frontend React + Vite

The frontend implementation is officially React + Vite with React Router, TanStack Query, Zod, and Vitest / React Testing Library.
The project must not migrate to Next.js as part of remediation.
Playwright may be added for E2E coverage.

## CD-002 - Workshop + Support Settings In Scope

Workshop, Workshop Offering Management, Workshop Booking, Admin Workshop Booking Management, and Support Settings are officially in scope.
Existing `workshop` and `support` modules, `V9__workshop_booking.sql`, and `V10__support_settings.sql` must be retained.

Current implementation:

- Public workshop offerings are readable by guests and customers.
- Admin users manage workshop offerings.
- Customers can submit workshop booking requests.
- Admin users manage workshop booking status.
- Support Settings owns public contact configuration: email, Zalo phone, secondary phone, Facebook URL, address, map URL, and opening hours.
- Support Settings public read is available to all users; update is admin-only.

Workshop entities and rules:

- `WorkshopOffering` fields: `id`, `title`, `description`, `priceAmount` in VND, `durationMinutes`, `maxParticipants`, optional `imageUrl`, `status`, `createdAt`, `updatedAt`.
- `WorkshopOfferingStatus`: `ACTIVE`, `INACTIVE`.
- Public workshop routes return only `ACTIVE` offerings; admin routes return/manage all offerings.
- `WorkshopBooking` fields: `id`, optional `workshopId`, `fullName`, `email`, `phone`, `preferredAt`, `participants`, optional `note`, `status`, `createdAt`, `updatedAt`.
- `WorkshopBookingStatus`: `NEW`, `CONFIRMED`, `CANCELLED`, `COMPLETED`.
- Booking validation: preferred time must be in the future, participants must be within request limits, inactive offerings cannot be booked, and participants cannot exceed the selected offering capacity.
- Public booking creation is available to guest/customer users; admin booking list/status transition is protected by `/api/v1/admin/**`.
- Business timezone remains `Asia/Ho_Chi_Minh`; persisted timestamps use the existing UTC persistence pattern.

Workshop endpoints:

- `GET /api/v1/workshops`
- `GET /api/v1/workshops/{id}`
- `POST /api/v1/workshop/bookings`
- `GET /api/v1/admin/workshops`
- `POST /api/v1/admin/workshops`
- `PUT /api/v1/admin/workshops/{id}`
- `DELETE /api/v1/admin/workshops/{id}`
- `GET /api/v1/admin/workshop/bookings`
- `PATCH /api/v1/admin/workshop/bookings/{id}/status`

Support Settings entities and rules:

- `SupportSettings` is a singleton row with `id = 1`.
- Fields: `email`, `zaloPhone`, optional `secondaryPhone`, optional `facebookUrl`, `address`, optional `mapUrl`, optional `openingHours`, `updatedAt`.
- It is contact/support configuration only, not a generic CMS and not a secret store.
- Public route reads the current support settings; admin route updates the singleton only.

Support endpoints:

- `GET /api/v1/support/settings`
- `GET /api/v1/admin/support/settings`
- `PUT /api/v1/admin/support/settings`

## CD-003 - No Shipping

Shipping Module, Shipping Fee, and Shipping Tracking remain out of scope.
Checkout and frontend summaries must show only subtotal and total.
`Order.totalAmount` remains the authoritative amount and `Payment.amount = Order.totalAmount`.

## CD-004 - payOS External Verification Pending

The final external payOS provider contract is not frozen for production deployment.
The code must keep the adapter boundary and must not hard-code credentials, fake paid status, or require a live provider call for remediation.

Status: `PAYOS_EXTERNAL_VERIFICATION_PENDING`.
