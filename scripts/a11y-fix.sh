#!/usr/bin/env bash
# droid exec #2 - Accessibility fixer
#
# For each agent-ready [A11Y] ticket, create ONE branch + ONE PR implementing the
# minimal fix. Each PR triggers .github/workflows/qa.yml, whose qa skill reports the
# accessibility assertions red (on main) -> green (on the fix branch).
#
# Prereqs: droid CLI on PATH; Linear MCP configured; gh authenticated.
# Usage:
#   ./scripts/a11y-fix.sh                 # discovers tickets labeled agent-ready in Linear
#   ./scripts/a11y-fix.sh PRO-543         # a single ticket (focused live demo)
#   A11Y_TICKETS="PRO-543 PRO-544" ./scripts/a11y-fix.sh
# Env: DROID_AUTO (default: high) - autonomy level for droid exec.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export PATH="$HOME/.local/bin:$HOME/.factory/bin:$PATH"
cd "$ROOT"

AUTONOMY="${DROID_AUTO:-high}"
DRY_RUN="${DRY_RUN:-0}"

ARGS=()
for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY_RUN=1 ;;
    -*) echo "Unknown option: $arg (supported: --dry-run)" >&2; exit 2 ;;
    *) ARGS+=("$arg") ;;
  esac
done

DEFAULT_TICKETS="PRO-543 PRO-544 PRO-545 PRO-546 PRO-542"

if [ "${#ARGS[@]}" -gt 0 ]; then
  TICKETS=("${ARGS[@]}")
elif [ -n "${A11Y_TICKETS:-}" ]; then
  # shellcheck disable=SC2206
  TICKETS=(${A11Y_TICKETS})
elif [ "$DRY_RUN" = "1" ]; then
  # shellcheck disable=SC2206
  TICKETS=(${DEFAULT_TICKETS})
else
  echo ">>> Discovering [A11Y] tickets labeled 'agent-ready' in Linear (team PRO)..."
  TICKETS=()
  while IFS= read -r _line; do
    [ -n "$_line" ] && TICKETS+=("$_line")
  done < <(
    droid exec --auto low 'Using the Linear MCP tools, list issues in team "Assembly Demos" (key PRO) that have the label "agent-ready" and whose title starts with "[A11Y]". Output ONLY their identifiers, one per line (for example PRO-543), between a line "===IDS START===" and a line "===IDS END===". No other text.' 2>/dev/null \
      | awk '/===IDS START===/{f=1;next} /===IDS END===/{f=0} f' \
      | grep -Eo 'PRO-[0-9]+'
  )
  if [ "${#TICKETS[@]}" -eq 0 ]; then
    echo ">>> No agent-ready tickets discovered; falling back to the default set."
    # shellcheck disable=SC2206
    TICKETS=(${DEFAULT_TICKETS})
  fi
fi

if [ "$DRY_RUN" = "1" ]; then
  VERB="Plan"
  ACTION='DRY RUN: change nothing. Do NOT edit files, do NOT run git or gh, do NOT modify Linear. Instead print: the branch name you would create, a concise per-file list of the exact edits you would make, and the verification command you would run.'
else
  VERB="Implement and ship"
  ACTION='EXECUTE now: (a) create a branch off origin/main with "git fetch origin && git checkout -b <branch> origin/main" (prefer the gitBranchName Linear suggests); (b) implement ONLY the change, confined to the listed files, matching existing style, with no new dependencies; (c) run "pnpm lint" (and "pnpm exec prettier --write" on changed files if needed) until clean; (d) commit as "[<ticket-id>] <short description>" and push with "git push -u origin <branch>"; (e) open a PR against main with gh (body: summary, changed files, and a note that the QA workflow verifies the a11y assertions red->green); (f) move the ticket to "In Progress" in Linear and comment the PR URL; (g) leave the repo on the feature branch and do NOT merge.'
fi

echo "=== A11Y fixer: ${#TICKETS[@]} ticket(s): ${TICKETS[*]} (droid exec --auto ${AUTONOMY}) ==="
[ "$DRY_RUN" = "1" ] && echo "(dry run: no file, git, gh, or Linear writes)"

for ID in "${TICKETS[@]}"; do
  echo ""
  echo "------------------------------------------------------------"
  echo ">>> ${VERB}: ${ID}"
  echo "------------------------------------------------------------"
  [ "$DRY_RUN" = "1" ] || git fetch origin --quiet || true

  PROMPT="You are running headless and non-interactive. There is NO human available:
never call AskUser, never wait for confirmation, never pause for input.

TASK: ${VERB} the fix for Linear ticket ${ID}.

1. Fetch ${ID} from Linear via MCP. Read its description and acceptance criteria.
2. Determine the minimal change, confined to the files listed in ${ID}.

${ACTION}

Constraints: exactly one ticket (${ID}); do not touch unrelated files or other [A11Y] tickets."

  droid exec --auto "$AUTONOMY" "$PROMPT" || echo "!! ${ID}: run exited non-zero; continuing to next ticket"
done

[ "$DRY_RUN" = "1" ] || git checkout main --quiet 2>/dev/null || true
echo ""
if [ "$DRY_RUN" = "1" ]; then
  echo "=== Dry run complete. No changes made. Re-run without --dry-run to ship PRs. ==="
else
  echo "=== Done. One PR per agent-ready ticket. Check GitHub PRs and the 'QA Pass' workflow. ==="
fi
