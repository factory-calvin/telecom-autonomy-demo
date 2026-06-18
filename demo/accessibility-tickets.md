# Accessibility Demo: Triage -> Fix -> QA verifies

A two-command demo on the FactoryFone Admin Portal showing an agent **triage** an
accessibility backlog, then **fix** the items it judged safe to automate, with each fix
landing as a PR that the **QA skill verifies** (red on `main`, green on the fix PR).

All issues are real and grounded in the codebase. No invented problems.

## The two commands

| Step      | Script                   | What it does                                                                                                                                                                      |
| --------- | ------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1. Triage | `scripts/a11y-triage.sh` | Reads every `[A11Y]` Backlog ticket, applies `demo/triage-rubric.md`, posts a JSON verdict comment, and moves agent-ready tickets to **Todo** (needs-human stays in **Backlog**). |
| 2. Fix    | `scripts/a11y-fix.sh`    | For each agent-ready ticket, opens **one branch + one PR** with the minimal fix. The PR triggers `.github/workflows/qa.yml`.                                                      |

Both call `droid exec` headlessly. Prerequisites:

- Factory `droid` CLI on PATH (`$HOME/.factory/bin`).
- **Linear MCP** configured in your droid config (the scripts read/update tickets via MCP).
- `gh` authenticated for the fix step (PR creation).

## The backlog (Linear team: Assembly Demos / PRO)

### Agent-ready (5) — the fixer will ship these

| Ticket                                                | Issue                                                            | Fix                                             | QA diff the skill surfaces                                                    |
| ----------------------------------------------------- | ---------------------------------------------------------------- | ----------------------------------------------- | ----------------------------------------------------------------------------- |
| [PRO-543](https://linear.app/factoryai/issue/PRO-543) | Icon-only Edit/Delete buttons have no accessible name (4 tables) | `aria-label` per button + `aria-hidden` on icon | a11y tree: `button` -> `button "Edit customer …"`                             |
| [PRO-544](https://linear.app/factoryai/issue/PRO-544) | Dashboard charts have no accessible name                         | `role="img"` + `aria-label` summary             | a11y tree: chart -> `img "Revenue by Plan: …"`                                |
| [PRO-542](https://linear.app/factoryai/issue/PRO-542) | No skip-to-content link / no `<main>` landmark                   | skip link + `<main id="main-content">`          | a11y tree gains `link "Skip to main content"` + `main`; **visible focus GIF** |
| [PRO-545](https://linear.app/factoryai/issue/PRO-545) | Data tables lack caption + column-header scope                   | sr-only `<caption>` + `scope="col"`             | a11y tree: table gains a name + header associations                           |
| [PRO-546](https://linear.app/factoryai/issue/PRO-546) | Pages have no top-level `<h1>`                                   | one visible `<h1>` per page                     | a11y tree gains `heading … (level 1)`; **visible heading GIF**                |

### Needs-human (2) — triage routes these to a person

| Ticket                                                | Issue                                                                          | Why a human decides                                                      |
| ----------------------------------------------------- | ------------------------------------------------------------------------------ | ------------------------------------------------------------------------ |
| [PRO-548](https://linear.app/factoryai/issue/PRO-548) | Chart palette not colorblind-safe (`--chart-2 #e20074` vs `--chart-8 #c90062`) | new accessible palette / non-color encoding is a brand + design decision |
| [PRO-547](https://linear.app/factoryai/issue/PRO-547) | Keyboard/screen-reader strategy for the ~220k-row usage table                  | pagination vs virtualization + announcement strategy is architectural    |

## How the QA skill shows the diff

The `qa-web` sub-skill captures an **accessibility-tree snapshot** (and screenshots) per
page. It now evaluates explicit **Accessibility assertions** (see
`.factory/skills/qa-web/SKILL.md`) reported as individual `checks[]`:

| Assertion                  | Ticket  | Result on `main` | Result on fix PR |
| -------------------------- | ------- | ---------------- | ---------------- |
| `a11y.action_button_names` | PRO-543 | FAIL             | PASS             |
| `a11y.chart_names`         | PRO-544 | FAIL             | PASS             |
| `a11y.skip_link_and_main`  | PRO-542 | FAIL             | PASS             |
| `a11y.table_semantics`     | PRO-545 | FAIL             | PASS             |
| `a11y.page_h1`             | PRO-546 | FAIL             | PASS             |

Because the QA workflow is **informational/non-blocking**, the failing baseline never
blocks a merge; it just makes the before/after contrast visible in the PR comment.

## Running the demo

```bash
# 1. Triage the backlog (read-only on code; updates Linear)
./scripts/a11y-triage.sh

# 2. Fix every agent-ready ticket (one PR each). Defaults to the 5 agent-ready IDs;
#    override with args or the A11Y_TICKETS env var.
./scripts/a11y-fix.sh
./scripts/a11y-fix.sh PRO-543            # just one ticket, for a focused live demo
```

Optional: to show the red baseline first, run the QA workflow on `main`
(`gh workflow run "QA Pass"`) before opening the fix PRs.
