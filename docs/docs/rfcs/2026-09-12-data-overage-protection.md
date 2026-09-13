# Data Overage Protection (Bill Shock Directive)

## Summary

FactoryFone must make current-cycle mobile data consumption visible to Care and Finance before National Telecom Regulatory Authority Consumer Protection Directive 2026/14 takes effect on 1 November 2026. For finite-data plans, the Admin Portal will calculate usage against the plan allowance, classify subscribers at 80% as **At risk** and at 100% as **Over limit**, support filtering and dashboard reporting, record the applicable overage cap and uncapped-overage opt-in state, and ensure one follow-up ticket exists per over-limit subscriber per billing cycle. Unlimited plans remain exempt from threshold and cap treatment but still require usage visibility.

## Source

- Notion: [Market rule MR-2026-14: Data Overage Protection (Bill Shock Directive)](https://app.notion.com/p/Calvin_DemoDoc-3d92517972c780f79f99edc94f910ded)
- Authority: National Telecom Regulatory Authority, Consumer Protection Directive 2026/14

## Business context

The directive addresses unexpected data overage charges, or “bill shock.” It applies to all consumer and business mobile plans with a data allowance in FactoryFone's home market from **1 November 2026**.

The exposure is material: “Non-compliance after the effective date is fined at up to 2% of annual domestic mobile revenue per audit finding, and complaints escalate to the regulator after 15 days.” Customer Care also cannot currently see allowance consumption even though overage disputes are the second-most-common support-ticket category, and Finance estimates that 6% of monthly overage revenue is refunded after disputes.

The source identifies roughly 500 subscribers across eight plans. Basic, Standard, Premium, Family Share, Business Starter, Business Pro, and Student have finite data allowances. Unlimited is exempt from cap and threshold flags, but it is not exempt from usage visibility. The portal must support Care's subscriber-level workflow and Finance's monthly regulatory return.

This release does not send subscriber SMS or email notifications, change rating or invoicing in the billing platform, or include roaming data.

## Current state in the product

- `backend/src/main/java/com/example/demo/model/Plan.java` stores `dataLimitGb`; `null` represents the seeded Unlimited plan. It has no market, plan-family, billing-cycle, or cap policy fields.
- `backend/src/main/java/com/example/demo/model/UsageRecord.java` stores `DATA` records as `quantity`, `cost`, and `recordedAt`. `UsageRecordRepository.java` can filter records but has no current-cycle per-customer aggregate.
- `scripts/seed-database.py` seeds finite allowances in GB and `DATA` quantities between 10 and 800 without documenting the quantity unit. It builds SQLite tables explicitly, so data-model changes must also update this script.
- `Customer.java` and `CustomerController.java` have no overage preference, cap, consumption, or threshold fields. `GET /api/customers` returns the full customer list with flattened plan identity.
- `SupportTicket.java` has subject, description, priority, status, and timestamps, but no ticket kind or billing-cycle key. `SupportTicketRepository.java` therefore cannot enforce “one Overage notice per subscriber per cycle.”
- `DashboardController.java` exposes subscription, revenue, ticket, device, and chart metrics. `DashboardStats` has no at-risk or over-limit counts.
- `hooks/use-customers.ts` and `components/customers-table.tsx` render customer identity, plan, account status, and balance. `app/customers/page.tsx` has no data-risk filter.
- `hooks/use-dashboard-stats.ts` and `app/dashboard/page.tsx` fetch and render the existing four KPI values, but no overage-protection metrics.
- Existing backend controller tests cover customer response mapping and dashboard aggregation. Existing frontend tests cover the customer table and dashboard hook. There are no usage-aggregation or automatic-ticket tests.
- `docs/docs/backend/api.md`, `docs/docs/backend/models.md`, and the frontend documentation describe only the current fields and endpoints.

## Proposed change

### Backend

1. Add a single service responsible for current-cycle data calculations. For the existing product, a cycle is the half-open UTC interval from the first day of the calendar month through the first day of the next month. Sum only `UsageRecord.Type.DATA` records in that interval. Normalize the stored quantity into GB under the approved data contract, retain decimal precision through the calculation, and round only response display values.
2. Return a usage-protection projection for each customer through `GET /api/customers`: used data, allowance, percentage where an allowance is finite and positive, and a semantic state of `WITHIN_LIMIT`, `AT_RISK`, `OVER_LIMIT`, or `UNLIMITED`. The 80% boundary is inclusive for `AT_RISK`; the 100% boundary is inclusive for `OVER_LIMIT`; `OVER_LIMIT` takes precedence.
3. Extend `GET /api/dashboard/stats` with current-cycle `at_risk_customers` and `over_limit_customers`. Count each customer once, exclude Unlimited plans from both values, and use the same calculation service as the customer response to prevent drift.
4. After the policy decision is recorded, persist the customer's uncapped-overage opt-in state with auditable effective metadata and expose the cap and opt-in state in customer reads and updates. Keep the market and currency explicit rather than embedding EUR assumptions in presentation code.
5. After Care Operations confirms the workflow, add an idempotent reconciliation path that creates an `Overage notice` ticket when a finite-plan customer first reaches 100% in a cycle. Store a machine-readable ticket kind and cycle key, and enforce uniqueness for customer, kind, and cycle so repeated runs cannot create duplicates.
6. Return explicit validation errors for unsupported policy inputs and add OpenAPI-visible response fields. Do not alter rating or invoice amounts in this application.

### Frontend

1. Extend `hooks/use-customers.ts` with the usage-protection response fields and render current-cycle data used, allowance/percentage, semantic threshold text, applicable cap, and opt-in state in `components/customers-table.tsx`.
2. Add a customer-list filter for `At risk` and `Over limit` in `app/customers/page.tsx`. Unlimited customers show usage but no percentage flag or cap.
3. Extend `hooks/use-dashboard-stats.ts` and `app/dashboard/page.tsx` with separate current-cycle At-risk and Over-limit KPI cards.
4. Use visible text and programmatic labels for every state. Color may reinforce status but must not be the only way to distinguish 80% from 100%. Apply the approved Design treatment when available.

### Data & seeding

1. Update `scripts/seed-database.py` alongside JPA model changes. Document the canonical unit for `usage_records.quantity`, and seed deterministic boundary examples for 79.9%, 80%, 99.9%, 100%, and above 100%.
2. Seed Unlimited-plan usage to verify visibility without threshold or cap treatment.
3. Seed opt-in and cap-policy data only after Pricing approves its scope. Seed at most one Overage notice per customer and cycle.
4. Treat existing rows using a documented migration/default strategy. Do not infer opt-in consent from existing balance or usage data.

### Docs

1. Update `docs/docs/backend/models.md` with usage units, policy/consent fields, cycle boundaries, and ticket idempotency keys.
2. Update `docs/docs/backend/api.md` with customer usage-protection fields and dashboard counts.
3. Update `docs/docs/frontend/components.md` and `docs/docs/frontend/hooks.md` with the list filter, semantic states, cap display, and dashboard metrics.
4. Include worked audit examples that reproduce the 8.5 GB of 10 GB (85%) and 10.2 GB of 10 GB (102%) scenarios from stored records.

## Acceptance criteria

1. Given a finite 10 GB plan and 8.5 GB of `DATA` usage in the current cycle, when the customer and dashboard APIs are read, then the customer reports 8.5 GB, 85%, and `AT_RISK`, and the dashboard counts that customer exactly once as at risk.
2. Given the same plan and 10.2 GB of current-cycle data usage, when the APIs are read, then the customer reports 102% and `OVER_LIMIT`, the dashboard counts the customer exactly once as over limit and not at risk, and exactly one cycle-keyed `Overage notice` ticket exists after reconciliation.
3. Given a customer on an Unlimited plan, when usage is read, then current-cycle data used is visible, percentage and cap are not shown, no threshold flag is applied, and the customer is excluded from at-risk and over-limit counts.
4. Given records before the current cycle, non-`DATA` records, or roaming records when roaming can be identified, when usage is calculated, then those records do not contribute to the protected domestic data total.
5. Given exactly 80% or exactly 100% usage, when status is calculated, then the inclusive boundaries produce `AT_RISK` and `OVER_LIMIT` respectively.
6. Given repeated reconciliation for an over-limit customer in one cycle, when ticket creation runs, then only one `Overage notice` ticket exists for that customer and cycle.
7. Given a customer who has not explicitly opted in, when cap data is read, then the approved market/plan cap applies; no migration or default marks the customer as opted in.
8. Given an explicit uncapped-overage opt-in change, when the customer is read and the audit data is inspected, then the current state and its effective metadata are reproducible.
9. Given the customer table, when Care filters by At risk or Over limit, then only customers in the selected current-cycle state are shown and each row includes readable usage and state text without relying on color alone.
10. Given Finance reads `GET /api/dashboard/stats`, when it reconciles the values to customer projections for the same cycle, then both counts match.
11. Automated tests cover cycle boundaries, DATA-only aggregation, finite and Unlimited plans, 80% and 100% boundaries, dashboard consistency, opt-in defaults, and ticket idempotency. `pnpm lint`, `pnpm test:frontend`, and `pnpm test:backend` pass.
12. Product documentation defines the calculation, canonical units, API fields, policy data, ticket uniqueness rule, and worked regulatory examples.

## Rollout & risk

- Deliver the calculation and read-only visibility before enabling policy mutation or ticket automation. Verify Finance's aggregate against a seeded export before the effective date.
- Gate automatic ticket creation until Care Operations approves priority, assignment, and visibility. Run reconciliation in report-only mode first and compare candidate tickets with customer projections.
- Default all existing customers to **not opted in**. An uncapped state must never be inferred or enabled by migration.
- Keep cap enforcement out of this portal. The portal records and displays the policy and consent state; the billing platform remains responsible for rating and invoice enforcement.
- Main risks are ambiguous usage units, month-boundary/time-zone errors, divergence between row and dashboard calculations, duplicate tickets, inaccessible color-only states, and unsupported Family-plan semantics.
- Add structured logs for calculation cycle, customer, semantic state, and ticket reconciliation outcome without logging sensitive contact data.

## Open questions

1. **Pricing/Product/Architecture:** What cap applies by home market and plan family, is Family Share capped per line or per account, which currency and tax basis apply, can plans override the calendar-month cycle, and what canonical unit/time zone should govern existing `DATA` rows? Options include a single EUR 50 account cap with UTC calendar cycles, plan-family policy records, or explicit market/plan policy records with cycle configuration.
2. **Care Operations:** What priority, assignment queue, subscriber visibility, and lifecycle apply to automatic `Overage notice` tickets? Options include a medium-priority internal-only queue, plan-segment routing, or escalation based on overage amount.
3. **Design/Accessibility:** What approved visual treatment should distinguish At risk and Over limit in the customer table? Any option must retain visible text and non-color semantics; candidates are text badges with icons, a labeled progress meter, or both.

## Issue slicing

| Title                                                         | Label         | Why that label                                                                                                                                                     |
| ------------------------------------------------------------- | ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Expose current-cycle data usage and threshold metrics         | `agent-ready` | The UTC calendar-cycle calculation, inclusive thresholds, affected Java APIs, seed data, tests, and docs are bounded and have existing validation commands.        |
| Show usage risk states in the customer list and dashboard     | `agent-ready` | The API contract and existing hook/table/KPI patterns provide a clear frontend change with established Vitest coverage. It depends on the backend metrics issue.   |
| Decide overage policy, billing-cycle, and audit data contract | `needs-human` | Pricing, product, and architecture must choose cap scope, Family semantics, units, time zone, and cycle overrides before compliant persistence can be implemented. |
| Decide Overage notice workflow and ticket automation          | `needs-human` | Care Operations must decide priority, ownership, and visibility before the idempotent automation is enabled.                                                       |
| Approve accessible threshold indicator design                 | `needs-human` | Design must select the final non-color-only presentation for compliance and usability.                                                                             |
