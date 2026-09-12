#!/usr/bin/env bash
# Risk-based review routing for a pull request.
#
# Two layers decide how much human attention a PR needs:
#   1. Deterministic floors/caps computed from the changed paths (CI config, schema,
#      dependencies, API surface, docs-only). These never depend on the model.
#   2. A read-only `droid exec` pass that scores the diff 0-100 and explains why.
# The final score is max(agent, floor) clamped by any cap, then routed:
#   score <  RISK_THRESHOLD_LOW   -> risk:low    + auto-merge-eligible   (optionally enables auto-merge)
#   score <  RISK_THRESHOLD_HIGH  -> risk:medium + needs-human-review
#   otherwise                     -> risk:high   + needs-human-review, and the step fails so the
#                                    check blocks merging until a human approves.
# The comment also prints the trailing human-review rate so the "trust curve" is visible.
#
# Works the same from GitHub Actions, Jenkins, or a laptop: it only needs `git`, `gh`,
# `jq`, `python3`, and `droid` on PATH.
#
# Env:
#   PR_NUMBER            (required) pull request number
#   BASE_REF             base branch (default: main)
#   FACTORY_API_KEY      required by droid exec
#   GH_TOKEN             token for gh (labels, comments, auto-merge)
#   GH_REPO              owner/repo (default: from gh repo view)
#   RISK_THRESHOLD_LOW   default 30
#   RISK_THRESHOLD_HIGH  default 70
#   RISK_AUTO_MERGE      "true" to enable squash auto-merge on low-risk PRs (default: false)
#   RISK_MODEL           optional model id for droid exec
#   RISK_DRY_RUN         "true" to skip label/comment/merge writes
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

PR_NUMBER="${PR_NUMBER:?PR_NUMBER is required}"
BASE_REF="${BASE_REF:-main}"
THRESHOLD_LOW="${RISK_THRESHOLD_LOW:-30}"
THRESHOLD_HIGH="${RISK_THRESHOLD_HIGH:-70}"
AUTO_MERGE="${RISK_AUTO_MERGE:-false}"
DRY_RUN="${RISK_DRY_RUN:-false}"
REPO="${GH_REPO:-$(gh repo view --json nameWithOwner -q .nameWithOwner)}"
OUT_DIR="$ROOT/risk-router"
mkdir -p "$OUT_DIR"

for bin in git gh jq python3 droid; do
  command -v "$bin" >/dev/null || { echo "missing dependency: $bin" >&2; exit 2; }
done

# ---------------------------------------------------------------------------
# 1. Deterministic layer: what changed, and what that implies regardless of the model
# ---------------------------------------------------------------------------
git fetch --no-tags origin "$BASE_REF" >/dev/null 2>&1 || true
RANGE="origin/${BASE_REF}...HEAD"
CHANGED_FILES="$(git diff --name-only "$RANGE")"
SHORTSTAT="$(git diff --shortstat "$RANGE" | sed 's/^ *//')"
FILE_COUNT="$(printf '%s\n' "$CHANGED_FILES" | sed '/^$/d' | wc -l | tr -d ' ')"

FLOOR=0
CAP=100
FLOOR_REASONS=()
bump_floor() { # bump_floor <score> <reason>
  if (( $1 > FLOOR )); then FLOOR=$1; fi
  FLOOR_REASONS+=("$2 (floor $1)")
}
matches() { printf '%s\n' "$CHANGED_FILES" | grep -qE "$1"; }

if matches '^\.github/workflows/|^Jenkinsfile$|^scripts/risk-router\.sh$|^\.husky/'; then
  bump_floor 70 "CI / review-routing configuration changed"
fi
if matches '^backend/src/main/java/.*/model/|^backend/app\.db$|\.sql$|^scripts/seed-database\.py$'; then
  bump_floor 50 "data model, schema, or seed data changed"
fi
if matches '^package\.json$|^pnpm-lock\.yaml$|^backend/build\.gradle$|^docs/package\.json$'; then
  bump_floor 50 "dependency manifest changed"
fi
if matches '^backend/src/main/java/.*/controller/'; then
  bump_floor 40 "REST API surface changed"
fi
if matches '^AGENTS\.md$|^\.factory/'; then
  bump_floor 40 "agent instructions or skills changed"
fi
DOCS_ONLY=true
while IFS= read -r f; do
  [[ -z "$f" ]] && continue
  case "$f" in
    docs/*|*.md|*.mdx) ;;
    *) DOCS_ONLY=false ;;
  esac
done <<< "$CHANGED_FILES"
if [[ "$DOCS_ONLY" == true && "$FILE_COUNT" -gt 0 ]]; then
  CAP=20
  FLOOR_REASONS+=("docs-only change (cap 20)")
fi

# ---------------------------------------------------------------------------
# 2. Agent layer: read-only droid exec scores the diff and explains itself
# ---------------------------------------------------------------------------
PROMPT_FILE="$OUT_DIR/prompt.md"
{
  cat "$ROOT/demo/autonomy-demo/risk-router-prompt.md"
  printf '\n\n## Inputs for this run\n'
  printf -- '- Repository: %s\n- Pull request: #%s\n- Diff range: %s\n- Shortstat: %s\n' "$REPO" "$PR_NUMBER" "$RANGE" "$SHORTSTAT"
  printf -- '- Deterministic floor already applied by the pipeline: %s\n' "$FLOOR"
  printf -- '- Changed files:\n'
  printf '%s\n' "$CHANGED_FILES" | sed 's/^/  - /'
} > "$PROMPT_FILE"

DROID_ARGS=(exec --output-format json -f "$PROMPT_FILE")
[[ -n "${RISK_MODEL:-}" ]] && DROID_ARGS+=(--model "$RISK_MODEL")
echo "▶ droid ${DROID_ARGS[*]}"
set +e
droid "${DROID_ARGS[@]}" > "$OUT_DIR/droid-output.json"
DROID_EXIT=$?
set -e
if (( DROID_EXIT != 0 )) || [[ "$(jq -r '.is_error // false' "$OUT_DIR/droid-output.json")" == "true" ]]; then
  echo "droid exec failed (exit $DROID_EXIT); routing to human review by default" >&2
  cat "$OUT_DIR/droid-output.json" >&2 || true
  AGENT_JSON='{"risk_score":100,"summary":"droid exec failed; defaulting to human review","reasons":["scorer unavailable"],"areas":[],"checks":{}}'
else
  # The final assistant message should be a JSON object; tolerate prose or fences around it.
  AGENT_JSON="$(jq -r '.result' "$OUT_DIR/droid-output.json" | python3 -c '
import json, re, sys
text = sys.stdin.read()
m = re.search(r"\{.*\}", text, re.S)
obj = json.loads(m.group(0)) if m else {}
obj.setdefault("risk_score", 100)
obj.setdefault("summary", "scorer returned no summary")
obj.setdefault("reasons", [])
obj.setdefault("areas", [])
obj.setdefault("checks", {})
print(json.dumps(obj))
')"
fi
printf '%s\n' "$AGENT_JSON" > "$OUT_DIR/agent-score.json"
SESSION_ID="$(jq -r '.session_id // "n/a"' "$OUT_DIR/droid-output.json" 2>/dev/null || echo n/a)"

AGENT_SCORE="$(jq -r '.risk_score' <<< "$AGENT_JSON")"
FINAL=$(( AGENT_SCORE > FLOOR ? AGENT_SCORE : FLOOR ))
(( FINAL > CAP )) && FINAL=$CAP

if (( FINAL < THRESHOLD_LOW )); then
  LEVEL=low;    ROUTE=auto-merge-eligible;  REMOVE=needs-human-review
elif (( FINAL < THRESHOLD_HIGH )); then
  LEVEL=medium; ROUTE=needs-human-review;   REMOVE=auto-merge-eligible
else
  LEVEL=high;   ROUTE=needs-human-review;   REMOVE=auto-merge-eligible
fi

jq -n --argjson agent "$AGENT_JSON" \
  --arg pr "$PR_NUMBER" --argjson floor "$FLOOR" --argjson cap "$CAP" --argjson final "$FINAL" \
  --arg level "$LEVEL" --arg route "$ROUTE" --arg session "$SESSION_ID" \
  --argjson floor_reasons "$(printf '%s\n' "${FLOOR_REASONS[@]:-}" | sed '/^$/d' | jq -R . | jq -s .)" \
  '{pr:$pr, agent:$agent, floor:$floor, cap:$cap, floor_reasons:$floor_reasons, final_score:$final, level:$level, route:$route, session_id:$session}' \
  > "$OUT_DIR/decision.json"
echo "▶ decision: score=$FINAL (agent=$AGENT_SCORE floor=$FLOOR cap=$CAP) level=$LEVEL route=$ROUTE"

# ---------------------------------------------------------------------------
# 3. Route: labels, comment, optional auto-merge, and a fail on high risk
# ---------------------------------------------------------------------------
if [[ "$DRY_RUN" == "true" ]]; then
  echo "RISK_DRY_RUN=true; not writing to GitHub"; cat "$OUT_DIR/decision.json"; exit 0
fi

ensure_label() { gh label create "$1" -R "$REPO" --color "$2" --description "$3" --force >/dev/null; }
ensure_label risk:low            2da44e "Risk router: low risk"
ensure_label risk:medium         bf8700 "Risk router: medium risk"
ensure_label risk:high           d1242f "Risk router: high risk"
ensure_label auto-merge-eligible 0e8a16 "Risk router: no human review required"
ensure_label needs-human-review  b60205 "Risk router: a human must review before merge"

gh pr edit "$PR_NUMBER" -R "$REPO" \
  --remove-label "risk:low,risk:medium,risk:high,$REMOVE" >/dev/null 2>&1 || true
gh pr edit "$PR_NUMBER" -R "$REPO" --add-label "risk:$LEVEL,$ROUTE" >/dev/null

# Trust curve: share of routed PRs in the last 30 days that needed a human.
SINCE="$(date -u -d '30 days ago' +%F 2>/dev/null || date -u -v-30d +%F)"
TOTAL=0
for l in risk:low risk:medium risk:high; do
  n=$(gh pr list -R "$REPO" --state all --label "$l" --search "updated:>=$SINCE" --limit 200 --json number -q 'length')
  TOTAL=$(( TOTAL + n ))
done
HUMAN=$(gh pr list -R "$REPO" --state all --label needs-human-review --search "updated:>=$SINCE" --limit 200 --json number -q 'length')
if (( TOTAL > 0 )); then RATE=$(( 100 * HUMAN / TOTAL )); else RATE=100; fi

REASONS_MD="$(jq -r '.reasons[]? | "- " + .' <<< "$AGENT_JSON")"
CHECKS_MD="$(jq -r '.checks | to_entries[]? | "| " + .key + " | " + (.value|tostring) + " |"' <<< "$AGENT_JSON")"
FLOOR_MD="$(printf '%s\n' "${FLOOR_REASONS[@]:-}" | sed '/^$/d; s/^/- /')"
[[ -z "$FLOOR_MD" ]] && FLOOR_MD="- none triggered"

COMMENT_FILE="$OUT_DIR/comment.md"
cat > "$COMMENT_FILE" <<EOF
<!-- droid-risk-router -->
## Droid risk router: **$LEVEL** (score $FINAL / 100) → \`$ROUTE\`

$(jq -r '.summary' <<< "$AGENT_JSON")

| Layer | Value |
| --- | --- |
| Agent score (read-only \`droid exec\`) | $AGENT_SCORE |
| Deterministic floor / cap | $FLOOR / $CAP |
| Thresholds (low < $THRESHOLD_LOW ≤ medium < $THRESHOLD_HIGH ≤ high) | final **$FINAL** |
| Files changed | $FILE_COUNT ($SHORTSTAT) |
| Human-review rate, trailing 30 days | **$RATE%** ($HUMAN of $TOTAL routed PRs) |

**Deterministic rules triggered**
$FLOOR_MD

**Why the agent scored it this way**
${REASONS_MD:-- (no reasons returned)}

| Check | Result |
| --- | --- |
${CHECKS_MD:-| (none) | |}

<sub>Session \`$SESSION_ID\` · thresholds are repo variables \`RISK_THRESHOLD_LOW\`/\`RISK_THRESHOLD_HIGH\`; lower them as trust grows.</sub>
EOF

# Replace our previous comment instead of stacking one per push.
EXISTING_ID=$(gh api "repos/$REPO/issues/$PR_NUMBER/comments" --paginate \
  -q '[.[] | select(.body | startswith("<!-- droid-risk-router -->"))] | last | .id // empty')
if [[ -n "$EXISTING_ID" ]]; then
  gh api -X PATCH "repos/$REPO/issues/comments/$EXISTING_ID" -F body=@"$COMMENT_FILE" >/dev/null
else
  gh pr comment "$PR_NUMBER" -R "$REPO" --body-file "$COMMENT_FILE" >/dev/null
fi

if [[ "$LEVEL" == "low" && "$AUTO_MERGE" == "true" ]]; then
  gh pr merge "$PR_NUMBER" -R "$REPO" --auto --squash && echo "▶ auto-merge enabled" || echo "auto-merge not enabled (repo setting or protection)" >&2
fi

if [[ "$LEVEL" == "high" ]]; then
  echo "::error::Risk router: high risk ($FINAL). A human must review before merge." >&2
  exit 1
fi
