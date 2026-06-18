# Accessibility Triage Rubric

Single source of truth for the **triage** step (`scripts/a11y-triage.sh`, droid exec #1).
It decides whether an accessibility ticket is `agent-ready` (a coding agent can ship the
fix unattended) or `needs-human` (a person must make a judgment call first).

The rubric is deliberately conservative: when in doubt, route to a human.

## Classify as `agent-ready` only if ALL of these hold

1. **Objective & verifiable.** Success can be confirmed by an automated signal:
   `pnpm lint` (eslint-plugin-jsx-a11y), a `qa-web` accessibility assertion, or an axe rule.
2. **Single correct implementation.** There is one well-established HTML/ARIA pattern for
   the fix. No taste, copywriting, or product judgment is required.
3. **Localized to code.** Changes are confined to the component/page files named in the
   ticket. No new dependencies, no infra, no API/schema/data changes.
4. **No brand / design / UX / architecture decision.** Does not require choosing colors,
   visual style, navigation model, pagination vs virtualization, or performance tradeoffs.
5. **Low blast radius.** Mechanical, reversible, and small (typically under ~50 lines
   across a handful of files).

## Classify as `needs-human` if ANY of these are true

- Requires a **brand or design** choice (color palette, visual treatment).
- Requires a **product or content** decision (what a text alternative should say, which
  data to expose, how much detail).
- Requires a **UX or architecture** decision (focus-management strategy, live-region
  patterns at scale, pagination vs virtualization).
- The "correct" answer is **ambiguous or context-dependent**.
- Needs a **new dependency, infrastructure, or cross-cutting refactor**.
- Involves **performance tradeoffs** or large-data handling.

## Tie-breakers

- If a ticket mixes a mechanical part and a judgment part, classify it `needs-human` and
  suggest splitting the mechanical sub-task into a new `agent-ready` ticket.
- If you are unsure whether an automated success signal exists, classify `needs-human`.

## Required action per ticket

1. Post a Linear comment containing a fenced ` ```json ` verdict block:

   ```json
   {
     "ticket": "PRO-543",
     "verdict": "agent-ready",
     "confidence": 0.92,
     "rationale": "Objective (jsx-a11y + a11y-tree), single ARIA pattern, localized to 4 table files, no design judgment.",
     "target_files": [
       "components/customers-table.tsx",
       "components/plans-table.tsx",
       "components/devices-table.tsx",
       "components/tickets-table.tsx"
     ],
     "suggested_fix": "Add aria-label to each icon button; aria-hidden on the icon.",
     "verification": "pnpm lint + qa-web a11y.action_button_names"
   }
   ```

2. Then apply the matching Linear label (the primary hand-off signal):
   - `agent-ready` issues get the **`agent-ready`** label
   - `needs-human` issues get the **`needs-human`** label
3. Move the ticket as a secondary signal:
   - `agent-ready` -> **Todo**
   - `needs-human` -> leave in **Backlog**

## Expected outcome for the demo set (oracle)

| Ticket  | Title                                                 | Expected verdict |
| ------- | ----------------------------------------------------- | ---------------- |
| PRO-543 | Icon-only Edit/Delete buttons have no accessible name | agent-ready      |
| PRO-544 | Dashboard charts have no accessible name              | agent-ready      |
| PRO-542 | No skip-to-content link or main landmark              | agent-ready      |
| PRO-545 | Data tables lack caption and column-header scope      | agent-ready      |
| PRO-546 | Pages have no top-level h1 heading                    | agent-ready      |
| PRO-548 | Chart color palette is not colorblind-safe            | needs-human      |
| PRO-547 | Keyboard/screen-reader strategy for large usage table | needs-human      |
