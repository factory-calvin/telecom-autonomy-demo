#!/usr/bin/env bash
# droid exec #1 - Accessibility triage
#
# Reads every [A11Y] ticket in Backlog (Linear team: Assembly Demos / key PRO),
# classifies each as agent-ready or needs-human using demo/triage-rubric.md, posts a
# structured JSON verdict comment, applies the agent-ready/needs-human label, and moves
# agent-ready tickets to Todo.
#
# Read-only on the repo. Writes only to Linear (via MCP).
#
# Prereqs: droid CLI on PATH; Linear MCP configured in your droid config.
# Env:     DROID_AUTO (default: medium) - autonomy level for droid exec.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export PATH="$HOME/.local/bin:$HOME/.factory/bin:$PATH"
cd "$ROOT"

RUBRIC="demo/triage-rubric.md"
[ -f "$RUBRIC" ] || { echo "ERROR: missing rubric at $ROOT/$RUBRIC" >&2; exit 1; }

AUTONOMY="${DROID_AUTO:-medium}"
DRY_RUN="${DRY_RUN:-0}"
for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY_RUN=1 ;;
    *) echo "Unknown argument: $arg (supported: --dry-run)" >&2; exit 2 ;;
  esac
done

if [ "$DRY_RUN" = "1" ]; then
  ACTION='DRY RUN: do NOT modify Linear. Do NOT post comments, do NOT apply labels, and do NOT change any issue status. For each issue, print to stdout the JSON verdict block you WOULD post and the label you WOULD apply (agent-ready or needs-human), per demo/triage-rubric.md.'
else
  ACTION='For each issue, post the JSON verdict block (per demo/triage-rubric.md) as a Linear comment, then apply the matching Linear label (agent-ready issues get the "agent-ready" label; needs-human issues get the "needs-human" label) and set status (agent-ready -> "Todo"; needs-human -> leave "Backlog").'
fi

read -r -d '' PROMPT <<EOF || true
You are running headless and non-interactive. There is NO human available:
never call AskUser, never wait for confirmation, never pause for input.

TASK: Triage accessibility tickets in Linear.

1. Read the rubric at demo/triage-rubric.md and follow it exactly.
2. Using the Linear MCP tools, list issues in team "Assembly Demos" (key PRO) whose
   title starts with "[A11Y]" and whose status is "Backlog".
3. For each issue, read the files it references in this repository to confirm the problem
   is real. Do NOT modify any file. Do NOT run git or gh. The repo is read-only here.
4. Classify each issue agent-ready or needs-human strictly per the rubric.

${ACTION}

Finally, print a summary table to stdout: ticket | verdict | confidence | one-line rationale.
EOF

echo "=== A11Y triage (droid exec --auto ${AUTONOMY}) ==="
[ "$DRY_RUN" = "1" ] && echo "(dry run: no Linear writes)"
droid exec --auto "$AUTONOMY" "$PROMPT"
