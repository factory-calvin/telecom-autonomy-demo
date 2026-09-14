# Support Response-Time Transparency

## Summary

FactoryFone must make support-ticket response deadlines visible and reportable before National Telecom Regulatory Authority Consumer Protection Directive 2026/19 takes effect on 1 December 2026. The Admin Portal will calculate ticket age and acknowledgement and resolution deadlines from stored timestamps, identify tickets that are due soon or overdue, let Care filter by those states, add the same counts to the dashboard API, and raise resolution-overdue tickets to at least High priority without lowering Urgent tickets.

## Source

- Notion: [Market rule MR-2026-19: Support Response-Time Transparency](https://app.notion.com/p/Market-rule-MR-2026-19-Support-Response-Time-Transparency-3da2517972c7810a9a54fff30cd144d6)
- Authority: National Telecom Regulatory Authority, Consumer Protection Directive 2026/19

## Business context

The directive applies to all support tickets raised by consumer and business subscribers in FactoryFone's home market from **1 December 2026**. It requires operators to acknowledge every subscriber complaint within two business days and resolve or escalate it within ten business days. The source states: “From the effective date, operators must report the share of tickets that breached either deadline in the monthly regulator return. Repeated breaches above 5% trigger a formal audit.”

Care agents currently see status and priority, but not age or proximity to either deadline. Team leads recreate that view manually in a weekly spreadsheet. The operational population is material: roughly 200 open tickets at any time across approximately 500 subscribers. Finance needs ticket-breach counts in `GET /api/dashboard/stats`, the same API that provides current-cycle overage counts, so one source feeds the monthly regulator return.

The minimum rule applies equally to consumer and business subscribers. Whether Business plans carry a shorter contractual SLA remains a Pricing and Legal decision. This release does not send customer email or SMS notifications, add public-holiday calendars or per-market working hours, or change billing and plan models.

## Current state in the product

- `backend/src/main/java/com/example/demo/model/SupportTicket.java` stores customer, subject, description, priority, status, `createdAt`, and `resolvedAt`. It has no acknowledgement timestamp, escalation timestamp, or stored SLA outcome.
- `backend/src/main/java/com/example/demo/controller/SupportTicketController.java` exposes paginated CRUD through `/api/tickets`. The list accepts priority, status, and customer filters. Moving a ticket to Resolved or Closed writes `resolvedAt`, but moving it back to an active state does not clear that timestamp.
- `backend/src/main/java/com/example/demo/repository/SupportTicketRepository.java` sorts newest first and can filter by priority, status, and customer. It has no deadline-aware query or aggregate.
- `backend/src/main/java/com/example/demo/controller/DashboardController.java` exposes `GET /api/dashboard/stats`. It already combines subscription, ticket, device, and overage-protection KPIs, but has no acknowledgement-overdue, resolution-overdue, or due-soon counts.
- `hooks/use-tickets.ts`, `components/tickets-table.tsx`, and `app/tickets/page.tsx` expose the current ticket fields and filters. The table formats `created_at` as a date only and does not show age or SLA state.
- `hooks/use-dashboard-stats.ts` and `app/dashboard/page.tsx` model and render the existing dashboard KPIs, including the MR-2026-14 overage counts.
- `scripts/seed-database.py` creates the SQLite ticket schema and seeds 250 tickets up to 60 calendar days old. `DataSeeder.java` is the fallback seeder. Neither provides deterministic business-day deadline examples.
- Backend coverage includes `DashboardControllerTest`, but no `SupportTicketControllerTest` or deadline service test exists. Frontend coverage includes the dashboard hook and page, but no ticket hook, table, or page test exists.
- `docs/docs/backend/api.md`, `docs/docs/backend/models.md`, `docs/docs/frontend/components.md`, and `docs/docs/frontend/hooks.md` describe only the current ticket contract and filters.

## Proposed change

### Backend

1. Add an auditable `acknowledgedAt` timestamp to `SupportTicket`. Set it once when the approved acknowledgement event occurs. Keep `createdAt`, `acknowledgedAt`, and `resolvedAt` as the source data for calculations rather than persisting a mutable age.
2. Add one ticket-deadline service with an injectable `Clock`. Calculate whole elapsed age for display and business-time deadlines by counting Monday through Friday while excluding weekends. Public holidays and market-specific working hours are not part of this release.
3. Return `age_hours`, `age_days`, `acknowledgement_due_at`, `resolution_due_at`, and semantic deadline states from `/api/tickets`. The states are `ON_TRACK`, `DUE_SOON`, `ACKNOWLEDGEMENT_OVERDUE`, and `RESOLUTION_OVERDUE`; resolution overdue takes precedence when more than one condition applies. Resolved and Closed tickets retain reproducible deadline outcomes for reporting but do not appear as currently due soon.
4. Extend `GET /api/tickets` with a `deadlineState` filter. Apply the filter before pagination so `totalElements`, `totalPages`, and `hasNext` describe the filtered set.
5. Extend `GET /api/dashboard/stats` with separate `acknowledgement_overdue_tickets`, `resolution_overdue_tickets`, and `due_soon_tickets` values. Use the same deadline service and as-of time as ticket projections so Finance can reconcile aggregate counts to ticket rows.
6. After the trigger decision is approved, add idempotent reconciliation that raises every resolution-overdue Low or Medium ticket to High, preserves High and Urgent, and records `priorityEscalatedAt`. Never mutate ticket priority as a side effect of a GET request.
7. Fix active-state transitions so a ticket moved from Resolved or Closed back to Open or In Progress clears stale `resolvedAt`. Validate unknown filter enum values as client errors rather than server errors.

### Frontend

1. Extend the `Ticket` and dashboard contracts in `hooks/use-tickets.ts` and `hooks/use-dashboard-stats.ts` with the new API fields.
2. Show each active ticket's age and visible semantic deadline text in `components/tickets-table.tsx`. Use color only as a secondary cue and include exact deadline timestamps in accessible labels or supporting text.
3. Add an All, Due soon, Acknowledgement overdue, and Resolution overdue filter to `app/tickets/page.tsx`. Send the semantic state to the backend rather than recalculating business days in the browser.
4. Add separate dashboard cards for acknowledgement overdue, resolution overdue, and due soon. Explain that the counts are current ticket counts, not the monthly breach-rate denominator.

### Data & seeding

1. Update `scripts/seed-database.py` alongside the JPA model. Add nullable acknowledgement and priority-escalation timestamps without deleting or reclassifying existing ticket history.
2. Update both the Python seeder and `DataSeeder.java` with deterministic examples on Monday-to-Friday boundaries, across a weekend, exactly due soon, exactly due, and overdue. Include Open, In Progress, Resolved, and Closed examples.
3. Default existing tickets' new timestamps to null. Do not infer acknowledgement from priority, subject text, customer plan, or arbitrary historical updates.
4. If reconciliation needs persistent run state, add only the minimum idempotency data required by the approved architecture. Do not add billing or plan fields.

### Docs

1. Update `docs/docs/backend/models.md` with acknowledgement and escalation timestamps, state precedence, and the Monday-to-Friday calculation.
2. Update `docs/docs/backend/api.md` with the ticket projection, `deadlineState` filter, and dashboard counts.
3. Update `docs/docs/frontend/components.md` and `docs/docs/frontend/hooks.md` with age display, semantic filters, and dashboard cards.
4. Include worked examples for a ticket created on Monday and a ticket spanning a weekend so an auditor can reproduce every deadline from stored timestamps.

## Acceptance criteria

1. Given an active ticket and a fixed as-of instant, when `/api/tickets` is read, then its whole-hour and whole-day age is derived from `createdAt` and no mutable age is stored.
2. Given a ticket whose approved acknowledgement clock has fewer than 24 business hours remaining, when its projection is read, then it is `DUE_SOON`.
3. Given a ticket still Open after its two-business-day acknowledgement deadline, when its projection and dashboard are read, then it is `ACKNOWLEDGEMENT_OVERDUE` and counted exactly once in `acknowledgement_overdue_tickets`.
4. Given an Open or In Progress ticket after its ten-business-day resolution deadline, when its projection and dashboard are read, then it is `RESOLUTION_OVERDUE`, it is counted exactly once in `resolution_overdue_tickets`, and that state takes precedence over acknowledgement overdue.
5. Given a deadline interval that spans Saturday and Sunday, when the deadline is calculated, then weekend time does not consume either SLA and the result is reproducible with a fixed `Clock`.
6. Given a timestamp exactly on a deadline boundary, when the ticket is evaluated, then it is not overdue until the as-of instant is later than the deadline. Given exactly 24 business hours remaining, then it is `DUE_SOON`.
7. Given a Resolved or Closed ticket, when current attention counts are calculated, then it is excluded from `due_soon_tickets`; its timestamps remain available to reproduce whether either deadline was breached.
8. Given `deadlineState=RESOLUTION_OVERDUE`, when `/api/tickets` is read, then only matching tickets are returned and pagination metadata describes that filtered result.
9. Given a resolution-overdue Low or Medium ticket, when approved reconciliation runs repeatedly, then its priority is High and `priorityEscalatedAt` is written once. High stays High and Urgent stays Urgent.
10. Given Care views the ticket table, when a ticket is due soon or overdue, then age and readable state text are visible without relying on color, and each semantic filter returns the API-filtered set.
11. Given Finance reads `GET /api/dashboard/stats`, when it reconciles each new count to `/api/tickets` using the same as-of instant, then the counts match and remain separate from the existing `open_tickets`, `at_risk_customers`, and `over_limit_customers` values.
12. Automated tests cover weekday and weekend boundaries, exact due and due-soon boundaries, state precedence, active and resolved tickets, filtered pagination, dashboard consistency, idempotent priority escalation, and frontend rendering. `pnpm lint`, `pnpm test:frontend`, and `pnpm test:backend` pass.
13. Product documentation defines the source timestamps, calculation, state precedence, API fields, filtering, dashboard semantics, escalation behavior, and worked audit examples.

## Rollout & risk

- Ship timestamp capture and read-only deadline projections before enabling priority mutation. Compare seeded ticket projections with Care's manual spreadsheet and Finance's dashboard extract.
- Gate priority reconciliation until its execution trigger is approved. Run it in report-only mode first, and monitor candidate and updated ticket counts without logging customer contact details.
- Use one calculation service and injectable as-of time for rows, filters, aggregates, and tests. Avoid browser-side business-day calculations and GET side effects.
- Preserve null for unknown historical acknowledgements. Historical reports must distinguish missing evidence from a confirmed on-time acknowledgement.
- Main risks are ambiguity about the clock start and acknowledgement event, accidental calendar-day math, pagination after in-memory filtering, aggregate and row drift, repeated priority mutation, and applying contractual Business SLAs before Legal approval.

## Open questions

1. **Care Operations:** What starts the two-business-day acknowledgement clock, and what event proves acknowledgement? Options are ticket creation plus the first Open-to-In Progress transition, first agent assignment plus an explicit acknowledgement action, or another auditable event. The current model has no assignment timestamp.
2. **Pricing/Legal:** Do Business plan subscribers have a shorter contractual SLA than the regulatory minimum, and where should an override be stored? Options are one regulatory SLA for all tickets in this release, a customer-plan SLA profile, or a ticket-level snapshot of a centrally managed SLA policy.
3. **Architecture/Operations:** What should trigger overdue-priority reconciliation, and how quickly after a deadline must priority change? Options are a scheduled backend job, an externally invoked reconciliation endpoint, or an event-driven worker. Reads must remain side-effect free.

## Issue slicing

| Title                                                       | Label         | Why that label                                                                                                                                                |
| ----------------------------------------------------------- | ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Decide acknowledgement clock and evidence                   | `needs-human` | Care Operations must define the clock start and auditable acknowledgement event before timestamps and breach calculations can be implemented.                 |
| Decide Business subscriber SLA overrides                    | `needs-human` | Pricing and Legal must decide whether contractual SLAs differ and whether this release needs policy storage.                                                  |
| Decide overdue-priority reconciliation trigger              | `needs-human` | Architecture and Operations must choose the execution model and latency before a mutating reconciliation path is enabled.                                     |
| Add auditable ticket deadline calculations and reporting    | `agent-ready` | After the clock decision, the ticket projection, business-day service, API filter, dashboard counts, seeding, tests, and docs form one bounded backend slice. |
| Show ticket age, deadline filters, and dashboard SLA counts | `agent-ready` | Existing ticket and dashboard hooks, table, page, cards, and Vitest patterns make the frontend slice bounded once the backend contract is available.          |
