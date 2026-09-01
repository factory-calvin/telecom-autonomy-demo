#!/usr/bin/env bash
# CI failure triage - local / offline runner.
#
# Builds a "run bundle" (meta.json + junit/ + logs/) and hands it to the ci-triage
# skill (.factory/skills/ci-triage/SKILL.md) via headless `droid exec`. This is the
# same skill .github/workflows/ci-autotriage.yml runs, so a laptop and a GitHub
# runner produce the same verdicts from the same evidence.
#
# Sources for the bundle, pick one:
#   --run <id>       download logs + JUnit artifacts for a GitHub Actions run (needs gh)
#   --local <dir>    use a bundle that already exists on disk
#   --collect        build a bundle from local test output (offline; run your tests first)
#
# Options:
#   --fix            also ship fixes for clusters routed agent-fix-now (dirty tree, no commit)
#   --mode <m>       single (default) or batch
#   --auto <level>   droid autonomy level (default: high)
#   --dry-run        print the prompt and exit without calling droid
#
# Examples:
#   ./scripts/ci-triage.sh --run 1234567890
#   ./scripts/ci-triage.sh --run 1234567890 --fix
#   pnpm test:frontend; ./scripts/ci-triage.sh --collect
#   ./scripts/ci-triage.sh --local demo/ci-failures/saved-run
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export PATH="$HOME/.local/bin:$HOME/.factory/bin:$PATH"
cd "$ROOT"

SOURCE=""
RUN_ID=""
LOCAL_DIR=""
MODE="single"
AUTONOMY="${DROID_AUTO:-high}"
FIX=0
DRY_RUN=0
BUNDLE="ci-triage/bundle"

die() {
  echo "ERROR: $*" >&2
  exit 1
}

while [ $# -gt 0 ]; do
  case "$1" in
    --run)
      SOURCE="run"
      RUN_ID="${2:-}"
      [ -n "$RUN_ID" ] || die "--run needs a run id"
      shift 2
      ;;
    --local)
      SOURCE="local"
      LOCAL_DIR="${2:-}"
      [ -n "$LOCAL_DIR" ] || die "--local needs a directory"
      shift 2
      ;;
    --collect)
      SOURCE="collect"
      shift
      ;;
    --fix)
      FIX=1
      shift
      ;;
    --mode)
      MODE="${2:-}"
      case "$MODE" in
        single | batch) ;;
        *) die "--mode must be single or batch" ;;
      esac
      shift 2
      ;;
    --auto)
      AUTONOMY="${2:-}"
      shift 2
      ;;
    --dry-run)
      DRY_RUN=1
      shift
      ;;
    -h | --help)
      sed -n '2,30p' "${BASH_SOURCE[0]}"
      exit 0
      ;;
    *) die "unknown argument: $1" ;;
  esac
done

[ -n "$SOURCE" ] || die "pick a source: --run <id>, --local <dir>, or --collect"
[ -f "demo/ci-triage-rubric.md" ] || die "missing rubric at $ROOT/demo/ci-triage-rubric.md"
[ -f ".factory/skills/ci-triage/SKILL.md" ] || die "missing ci-triage skill"

# ----------------------------------------------------------------------------- bundle

json_string() { python3 -c 'import json,sys; print(json.dumps(sys.argv[1]))' "$1"; }

write_meta() {
  # write_meta <pr_number|null> <head_sha> <head_ref> <base_ref> <run_id> <run_url> <failed_jobs_json>
  mkdir -p "$BUNDLE"
  cat > "$BUNDLE/meta.json" <<EOF
{
  "mode": "$MODE",
  "repo": $(json_string "${GH_REPO:-unknown}"),
  "workflow": "CI",
  "run_id": $(json_string "$5"),
  "run_url": $(json_string "$6"),
  "pr_number": $1,
  "head_sha": $(json_string "$2"),
  "head_ref": $(json_string "$3"),
  "base_ref": $(json_string "$4"),
  "failed_jobs": $7,
  "ground_truth": null
}
EOF
}

collect_local_junit() {
  local found=0
  if [ -f "test-results/junit.xml" ]; then
    cp "test-results/junit.xml" "$BUNDLE/junit/frontend-junit.xml"
    found=1
  fi
  if [ -f "test-results/e2e-junit.xml" ]; then
    cp "test-results/e2e-junit.xml" "$BUNDLE/junit/e2e-junit.xml"
    found=1
  fi
  if [ -d "backend/build/test-results/test" ]; then
    local n=0
    for f in backend/build/test-results/test/*.xml; do
      [ -f "$f" ] || continue
      cp "$f" "$BUNDLE/junit/backend-$(basename "$f")"
      n=$((n + 1))
    done
    [ "$n" -gt 0 ] && found=1
  fi
  return $((1 - found))
}

case "$SOURCE" in
  local)
    [ -d "$LOCAL_DIR" ] || die "no such directory: $LOCAL_DIR"
    [ -f "$LOCAL_DIR/meta.json" ] || die "$LOCAL_DIR is not a bundle (no meta.json)"
    BUNDLE="$LOCAL_DIR"
    echo "Using existing bundle: $BUNDLE"
    ;;

  collect)
    rm -rf "$BUNDLE"
    mkdir -p "$BUNDLE/junit" "$BUNDLE/logs"
    if ! collect_local_junit; then
      die "no local test output found. Run 'pnpm test:frontend' and/or 'cd backend && ./gradlew test' first."
    fi
    HEAD_SHA="$(git rev-parse HEAD)"
    HEAD_REF="$(git rev-parse --abbrev-ref HEAD)"
    write_meta "null" "$HEAD_SHA" "$HEAD_REF" "main" "local" "local" '["local"]'
    echo "Collected local test output into $BUNDLE"
    ;;

  run)
    command -v gh > /dev/null || die "gh CLI not found (needed for --run)"
    gh auth status > /dev/null 2>&1 || die "gh is not authenticated (run: gh auth login)"
    rm -rf "$BUNDLE"
    mkdir -p "$BUNDLE/junit" "$BUNDLE/logs"

    GH_REPO="$(gh repo view --json nameWithOwner -q .nameWithOwner)"
    export GH_REPO

    RUN_JSON="$(gh run view "$RUN_ID" --json headSha,headBranch,url,jobs,conclusion 2> /dev/null)" \
      || die "could not read run $RUN_ID"

    HEAD_SHA="$(printf '%s' "$RUN_JSON" | python3 -c 'import json,sys; print(json.load(sys.stdin)["headSha"])')"
    HEAD_REF="$(printf '%s' "$RUN_JSON" | python3 -c 'import json,sys; print(json.load(sys.stdin)["headBranch"])')"
    RUN_URL="$(printf '%s' "$RUN_JSON" | python3 -c 'import json,sys; print(json.load(sys.stdin)["url"])')"
    FAILED_JOBS="$(printf '%s' "$RUN_JSON" | python3 -c '
import json, sys
jobs = json.load(sys.stdin).get("jobs", [])
print(json.dumps([j["name"] for j in jobs if j.get("conclusion") == "failure"]))')"

    PR_NUMBER="$(gh pr list --state all --head "$HEAD_REF" --json number -q '.[0].number' 2> /dev/null || true)"
    [ -n "$PR_NUMBER" ] || PR_NUMBER="null"

    echo "Downloading failed-step logs for run $RUN_ID ..."
    gh run view "$RUN_ID" --log-failed > "$BUNDLE/logs/failed-steps.txt" 2> /dev/null \
      || echo "(no failed-step log available)" > "$BUNDLE/logs/failed-steps.txt"

    echo "Downloading test-result artifacts ..."
    ARTIFACT_DIR="$BUNDLE/artifacts"
    mkdir -p "$ARTIFACT_DIR"
    for name in frontend-test-results backend-test-results e2e-test-results playwright-report; do
      gh run download "$RUN_ID" -n "$name" -D "$ARTIFACT_DIR/$name" > /dev/null 2>&1 \
        || echo "(artifact $name not present)"
    done
    # Flatten every XML we found into junit/, keeping a unique name per file.
    find "$ARTIFACT_DIR" -name "*.xml" -print0 2> /dev/null | while IFS= read -r -d '' f; do
      rel="${f#"$ARTIFACT_DIR"/}"
      cp "$f" "$BUNDLE/junit/${rel//\//-}"
    done

    write_meta "$PR_NUMBER" "$HEAD_SHA" "$HEAD_REF" "main" "$RUN_ID" "$RUN_URL" "$FAILED_JOBS"
    ;;
esac

XML_COUNT="$(find "$BUNDLE/junit" -name '*.xml' 2> /dev/null | wc -l | tr -d ' ')"
LOG_COUNT="$(find "$BUNDLE/logs" -type f 2> /dev/null | wc -l | tr -d ' ')"
echo "Bundle ready: $BUNDLE ($XML_COUNT JUnit files, $LOG_COUNT logs)"

# ----------------------------------------------------------------------------- triage

mkdir -p ci-triage

if [ "$FIX" = "1" ]; then
  FIX_CLAUSE='FIX=1. After writing verdicts.json, apply fixes for clusters routed agent-fix-now following section 5 of the skill. Do NOT commit, do NOT push, do NOT open a PR: leave the working tree dirty and record what you changed in ci-triage/fixes.json.'
else
  FIX_CLAUSE='FIX=0. Triage only. Do NOT modify any file outside ci-triage/.'
fi

read -r -d '' PROMPT <<EOF || true
You are running headless and non-interactive in mode "$MODE". There is NO human
available: never call AskUser, never wait for confirmation, never pause for input.

TASK: Triage the failing CI run in the bundle at $BUNDLE.

1. Read .factory/skills/ci-triage/SKILL.md and follow it exactly.
2. Read demo/ci-triage-rubric.md. It is authoritative for classification and routing.
3. BUNDLE=$BUNDLE. Read $BUNDLE/meta.json first, then every file under
   $BUNDLE/junit/ and $BUNDLE/logs/.
4. Cluster the failures by root cause, classify and route each cluster, and read the
   actual source files before concluding anything. Do not classify from a stack trace
   alone.
5. Write ci-triage/verdicts.json (validate it against
   .factory/skills/ci-triage/verdict.schema.json), ci-triage/report.md (from
   .factory/skills/ci-triage/REPORT-TEMPLATE.md), and ci-triage/labels.txt.

$FIX_CLAUSE

Finally print a one-line summary to stdout: "<failures> failures -> <clusters> clusters:
<n> product-code, <n> test-code, <n> contract-drift, <n> infra, <n> flake, <n> hygiene".
EOF

if [ "$DRY_RUN" = "1" ]; then
  echo "=== dry run: prompt only ==="
  printf '%s\n' "$PROMPT"
  exit 0
fi

command -v droid > /dev/null || die "droid CLI not found on PATH"

echo "=== ci-triage (droid exec --auto $AUTONOMY, mode=$MODE, fix=$FIX) ==="

# stream-json gives a live trace and a final completion event carrying the real token
# counts. The agent cannot see its own usage, so economics are recorded from the CLI
# afterwards rather than self-reported.
SESSION_LOG="ci-triage/session.jsonl"
set +e
(
  set -o pipefail
  droid exec --output-format stream-json --auto "$AUTONOMY" "$PROMPT" \
    | tee "$SESSION_LOG" \
    | python3 scripts/ci-triage-stream.py
)
DROID_STATUS=$?
set -e

python3 scripts/ci-triage-stream.py --finalize "$SESSION_LOG" ci-triage/verdicts.json || true

echo
echo "Wrote:"
for f in ci-triage/verdicts.json ci-triage/report.md ci-triage/labels.txt ci-triage/fixes.json ci-triage/score.md; do
  [ -f "$f" ] && echo "  $f"
done

if [ "$DROID_STATUS" -ne 0 ]; then
  echo "WARNING: droid exec exited $DROID_STATUS; inspect $SESSION_LOG" >&2
fi
exit "$DROID_STATUS"
