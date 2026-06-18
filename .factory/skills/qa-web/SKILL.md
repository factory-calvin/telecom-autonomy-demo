---
name: qa-web
description: >
  QA sub-skill for the FactoryFone Next.js frontend. Drives a real browser through
  the admin pages (dashboard, customers, plans, devices, usage, tickets), asserts
  tables and charts render with seeded data, watches /api/* responses for 5xx,
  and saves a full-page screenshot per page. Invoked by the qa orchestrator.
---

# qa-web

Web QA sub-skill for the FactoryFone admin portal. The orchestrator passes you the run dir, base URL, and a subset of flows. It also tells you which flows to run based on the git diff.

## Testing target

This project does NOT use Vercel/Netlify preview deployments. Always test against a locally-running dev server:

- **Local dev:** the orchestrator/CI starts `pnpm dev:frontend` (or `pnpm dev`) before invoking this skill. Poll `http://localhost:3000` until 200 before the first navigation. Default base URL: `http://localhost:3000`.
- **Never** fall back to a remote/staging/prod URL. Those run different code than the PR branch.

## Authentication

None. Auth is disabled in this demo. No cookies, headers, or env vars are required to access any page.

## Tooling

Primary: use the `agent-browser` skill. Drive the page, take an accessibility-tree snapshot for inline evidence, and save a full-page screenshot to `qa-results/screenshots/web-<slug>.png`.

Fallback: if `agent-browser` is unavailable in the environment (e.g., dependency missing in CI), use Playwright directly. The repo already has `@playwright/test` installed and `pnpm exec playwright install chromium` is part of the workflow setup.

A reference Playwright runner script may be created at `.factory/skills/qa-web/run.mjs` if needed. Use it only as a last resort; prefer agent-browser for diff-targeted exploration.

## Available test flows (menu)

The orchestrator picks flows based on what changed. Do not run all flows blindly.

In addition, **every web flow evaluates the applicable checks in the Accessibility assertions section** below and records each as an individual `checks[]` entry (with the stable `id` shown), so the report shows FAIL on `main` and PASS on the fix PR.

### web.dashboard

**When to run:** any change under `app/dashboard/**`, `components/dashboard-*`, `hooks/use-dashboard-stats.ts`, or any backend change to `/api/dashboard/*`.

1. Navigate to `/dashboard`.
2. Wait for `networkidle` (15s timeout).
3. Assert at least one element with `[data-slot="card"]` is visible (KPI cards).
4. Assert at least one Recharts SVG (`svg.recharts-surface`, or any `<svg>` inside `[data-chart]`) is visible.
5. Capture screenshot to `screenshots/web-dashboard.png`.
6. Watch network: every `/api/dashboard/*` response must be 2xx.

Pass criteria: cards visible AND chart visible AND no /api 5xx.

### web.customers

**When to run:** changes under `app/customers/**`, `components/customers-table.tsx`, `hooks/use-customers.ts`, or backend `/api/customers`.

1. Navigate to `/customers`.
2. Wait for `networkidle`.
3. Assert `<table>` is visible AND has at least one `<tbody><tr>` (seeded data has 500 customers).
4. Capture screenshot.
5. Negative: open the URL with a query param the page should ignore (e.g., `/customers?invalid=1`); page should still render normally.

### web.plans

**When to run:** changes under `app/plans/**`, `components/plans-table.tsx`, `hooks/use-plans.ts`, or backend `/api/plans`.

1. Navigate to `/plans`.
2. Wait for `networkidle`.
3. Assert table visible.
4. Assert at least 8 plan rows (seeded count). If fewer, mark FAIL with the actual count.
5. Capture screenshot.

### web.devices

**When to run:** changes under `app/devices/**`, `components/devices-table.tsx`, `hooks/use-devices.ts`, or backend `/api/devices`.

1. Navigate to `/devices`.
2. Wait for `networkidle`.
3. Assert table visible with at least one row.
4. Capture screenshot.

### web.usage

**When to run:** changes under `app/usage/**`, `components/usage-table.tsx`, `hooks/use-usage.ts`, or backend `/api/usage`.

1. Navigate to `/usage`.
2. Wait for `networkidle` with extended 30s timeout (usage page returns ~220k rows; pagination may be in flight).
3. Assert table visible with at least one row.
4. Capture screenshot.

### web.tickets

**When to run:** changes under `app/tickets/**`, `components/tickets-table.tsx`, `hooks/use-tickets.ts`, or backend `/api/tickets`.

1. Navigate to `/tickets`.
2. Wait for `networkidle`.
3. Assert table visible with at least one row.
4. Capture screenshot.

### web.navigation

**When to run:** changes under `components/app-sidebar.tsx`, `app/layout.tsx`, or any new top-level route added in `app/**`.

1. Navigate to `/`.
2. Verify the sidebar contains links for: Dashboard, Customers, Plans, Devices, Usage, Tickets.
3. Click each sidebar link and verify the URL updates and the destination page renders without console errors.
4. Capture one screenshot per landed page.

### web.console_errors_global

**When to run:** ALWAYS, alongside any other web flow. (This is a passive check, not a standalone flow.)

- For every page visited, collect `console.error` messages.
- Ignore: React DevTools warnings, hydration mismatch warnings ONLY if listed in the Known Failure Modes section below.
- Any other console error → mark the page-level flow as FAIL with the verbatim error message.

## Accessibility assertions (a11y red->green)

These back the `[A11Y]` demo tickets. Evaluate each from the accessibility-tree snapshot
you already capture for the page; record each as an individual `checks[]` entry using the
stable `id` shown, so the report flips FAIL (on `main`) -> PASS (on the fix PR).

- **`a11y.action_button_names`** - applies to customers, plans, devices, tickets. Every row action control exposes a descriptive accessible name (matches `/edit/i` and `/delete/i`). A bare `button` with no name is FAIL. (PRO-543)
- **`a11y.table_semantics`** - applies to customers, plans, devices, tickets. The data table exposes an accessible name (a `<caption>` or `aria-label`) and column headers use `scope="col"`. Missing name or unscoped headers is FAIL. (PRO-545)
- **`a11y.chart_names`** - applies to dashboard. Every chart exposes a non-empty accessible name (`role="img"` + `aria-label`). An unnamed chart graphic is FAIL. (PRO-544)
- **`a11y.skip_link_and_main`** - applies to navigation + every page. The first focusable element is a "Skip to main content" link AND the page exposes exactly one `main` landmark. Missing either is FAIL. (PRO-542)
- **`a11y.page_h1`** - applies to every page. The page exposes exactly one level-1 heading (`<h1>`) with non-empty text. Zero `<h1>` is FAIL. (PRO-546)

Evidence: include the relevant accessibility-tree node(s) in the report's collapsed
evidence so the before/after diff is visible. When ImageMagick is enabled and a baseline
screenshot exists, also attach a before/after GIF for `a11y.skip_link_and_main` (focus
state) and `a11y.page_h1` (visible heading).

## Per-page evidence contract

For every page navigation:

1. HTTP status of the document request must be 2xx (else FAIL).
2. Capture full-page screenshot to `qa-results/screenshots/web-<slug>.png` (slug = path with `/` → `-`, root = `root`).
3. Take an accessibility-tree snapshot for inline embedding in the report.
4. Record a one-line summary of `/api/*` calls (count, statuses).
5. Evaluate the applicable Accessibility assertions (see section above) and append each as a `checks[]` entry; include the relevant before/after a11y-tree node(s) in the evidence.

## Output

Persist a structured result block at `qa-results/results/web.json`:

```json
{
  "app": "web",
  "base_url": "http://localhost:3000",
  "pages": [
    {
      "path": "/dashboard",
      "status": 200,
      "screenshot": "screenshots/web-dashboard.png",
      "console_errors": [],
      "api_calls": [{ "url": "/api/dashboard/stats", "status": 200 }],
      "checks": [{ "id": "cards_visible", "result": "pass" }]
    }
  ],
  "flows": [{ "id": "web.dashboard", "result": "pass", "notes": "" }]
}
```

## Known Failure Modes

1. **Cold backend slow first response.** First `/api/*` call after `pnpm dev:backend` start can take 10-15s while Spring Boot warms up. The orchestrator should poll `/api/health` to 200 before invoking this skill.
2. **Hydration mismatch on root route.** The root `app/page.tsx` redirects to `/dashboard`. A `next.js hydration` warning may appear on first paint; ignore it ONLY for path `/`.
3. **Recharts SVG renders empty briefly.** Wait for `networkidle` (not just `domcontentloaded`) before asserting chart presence. Recharts renders after data loads.
4. **Usage page large payload.** `/api/usage` returns ~220k records seeded; the table may take 5-10s to render. Use a 30s navigation timeout for `/usage`.
5. **Port already in use.** If `pnpm dev:frontend` fails because port 3000 is busy (concurrent test run, dangling process), kill the listener: `lsof -ti:3000 | xargs -r kill`.

## CI notes

- The workflow runs `pnpm install`, `pnpm exec playwright install --with-deps chromium`, `pnpm reset:db`, then starts both servers in the background and polls health before invoking this skill.
- `agent-browser` ships with the droid CLI; no extra install needed.
- All artifacts under `qa-results/` are uploaded as a build artifact at the end of the run.
