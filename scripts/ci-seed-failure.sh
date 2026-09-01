#!/usr/bin/env bash
# Seed one CI failure and open the PR that carries it.
#
# Branches off the default branch, applies a fault from the catalog
# (scripts/ci-faults.py, documented in demo/ci-failures/README.md), commits it with a
# plausible message, pushes, and opens a PR. The PR's CI run goes red, which is what
# .github/workflows/ci-autotriage.yml reacts to.
#
# Usage:
#   ./scripts/ci-seed-failure.sh F1                 # branch + commit + push + PR
#   ./scripts/ci-seed-failure.sh F1 --no-pr         # branch + commit + push, no PR
#   ./scripts/ci-seed-failure.sh F1 --local         # apply in place, no git at all
#   ./scripts/ci-seed-failure.sh --list
#
# Clean up afterwards with ./scripts/ci-demo-reset.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

FAULT=""
OPEN_PR=1
PUSH=1
LOCAL_ONLY=0

die() {
  echo "ERROR: $*" >&2
  exit 1
}

while [ $# -gt 0 ]; do
  case "$1" in
    --list)
      python3 scripts/ci-faults.py list
      exit 0
      ;;
    --no-pr)
      OPEN_PR=0
      shift
      ;;
    --no-push)
      OPEN_PR=0
      PUSH=0
      shift
      ;;
    --local)
      LOCAL_ONLY=1
      OPEN_PR=0
      PUSH=0
      shift
      ;;
    -h | --help)
      sed -n '2,17p' "${BASH_SOURCE[0]}"
      exit 0
      ;;
    F[1-9])
      FAULT="$1"
      shift
      ;;
    *) die "unknown argument: $1 (try --help)" ;;
  esac
done

[ -n "$FAULT" ] || die "which fault? try: $0 --list"

FAULT_JSON="$(python3 scripts/ci-faults.py show "$FAULT")" || die "unknown fault $FAULT"
field() { printf '%s' "$FAULT_JSON" | python3 -c "import json,sys; print(json.load(sys.stdin)[sys.argv[1]])" "$1"; }

TITLE="$(field title)"
SUBJECT="$(field commit_subject)"
BODY="$(field pr_body)"
CLASS="$(field classification)"
ROUTE="$(field route)"

if [ "$LOCAL_ONLY" = "1" ]; then
  python3 scripts/ci-faults.py apply "$FAULT"
  echo
  echo "Applied in place, no branch created. Undo with:"
  echo "  python3 scripts/ci-faults.py revert $FAULT"
  exit 0
fi

# ------------------------------------------------------------------------------- git

command -v git > /dev/null || die "git not found"
[ -z "$(git status --porcelain)" ] || die "working tree is dirty. Commit, stash, or use --local."

DEFAULT_BRANCH="$(git symbolic-ref --quiet refs/remotes/origin/HEAD 2> /dev/null | sed 's|refs/remotes/origin/||')"
[ -n "$DEFAULT_BRANCH" ] || DEFAULT_BRANCH="main"

BRANCH="demo/ci-fault-$(echo "$FAULT" | tr '[:upper:]' '[:lower:]')-$(date -u +%H%M%S)"

echo "=== seeding $FAULT: $TITLE"
echo "    expected triage: $CLASS -> $ROUTE"
echo

git fetch origin "$DEFAULT_BRANCH" --quiet
git switch --create "$BRANCH" "origin/$DEFAULT_BRANCH" --quiet
echo "Branched $BRANCH off origin/$DEFAULT_BRANCH"

python3 scripts/ci-faults.py apply "$FAULT"

git add --all
git commit --quiet --message "$SUBJECT" --message "$BODY"
echo "Committed: $SUBJECT"

if [ "$PUSH" = "0" ]; then
  echo
  echo "Not pushed (--no-push). Push with: git push -u origin $BRANCH"
  exit 0
fi

git push --quiet --set-upstream origin "$BRANCH"
echo "Pushed $BRANCH"

if [ "$OPEN_PR" = "0" ]; then
  echo
  echo "PR not opened (--no-pr). Open one with: gh pr create --fill"
  exit 0
fi

command -v gh > /dev/null || die "gh CLI not found; branch is pushed, open the PR manually"

PR_URL="$(gh pr create \
  --base "$DEFAULT_BRANCH" \
  --head "$BRANCH" \
  --title "$SUBJECT" \
  --body "$BODY")"

echo
echo "PR opened: $PR_URL"
echo
echo "Next: CI runs, goes red, and 'CI Auto-Triage' picks it up from $DEFAULT_BRANCH."
echo "Watch it with:  gh run watch"
