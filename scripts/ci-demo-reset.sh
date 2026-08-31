#!/usr/bin/env bash
# Reset the CI triage demo to a clean slate.
#
# Closes demo PRs, deletes demo branches locally and on the remote, reverts any fault
# still applied to the working tree, and clears triage output. Safe to run repeatedly.
#
# Only touches branches matching demo/ci-fault-* and fix/ci-triage-*, and only deletes
# files this demo creates. It never touches your other work.
#
# Usage:
#   ./scripts/ci-demo-reset.sh              # show what would change, then do it
#   ./scripts/ci-demo-reset.sh --dry-run    # show only
#   ./scripts/ci-demo-reset.sh --local-only # skip all remote and gh operations
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

DRY_RUN=0
LOCAL_ONLY=0

while [ $# -gt 0 ]; do
  case "$1" in
    --dry-run)
      DRY_RUN=1
      shift
      ;;
    --local-only)
      LOCAL_ONLY=1
      shift
      ;;
    -h | --help)
      sed -n '2,15p' "${BASH_SOURCE[0]}"
      exit 0
      ;;
    *)
      echo "ERROR: unknown argument: $1" >&2
      exit 1
      ;;
  esac
done

run() {
  if [ "$DRY_RUN" = "1" ]; then
    echo "  would run: $*"
  else
    "$@" || echo "  (failed, continuing: $*)"
  fi
}

PATTERN='^(demo/ci-fault-|fix/ci-triage-)'

echo "=== 1. Revert any applied faults"
APPLIED="$(python3 scripts/ci-faults.py status)"
echo "  $APPLIED"
if [[ "$APPLIED" == Applied* ]]; then
  for fault in $(python3 -c "
import sys
line = sys.argv[1]
print(' '.join(f.strip() for f in line.split(':', 1)[1].split(',')))" "$APPLIED"); do
    if [ "$DRY_RUN" = "1" ]; then
      echo "  would revert $fault"
    else
      python3 scripts/ci-faults.py revert "$fault"
    fi
  done
fi

echo
echo "=== 2. Clear triage output"
for path in ci-triage nightly-triage; do
  if [ -e "$path" ]; then
    echo "  removing $path/"
    run rm -rf "$path"
  fi
done

if [ "$LOCAL_ONLY" = "1" ]; then
  echo
  echo "Local reset complete (--local-only: remote branches and PRs untouched)."
  exit 0
fi

echo
echo "=== 3. Close open demo PRs"
if command -v gh > /dev/null && gh auth status > /dev/null 2>&1; then
  GH_REPO="$(gh repo view --json nameWithOwner -q .nameWithOwner)"
  while IFS=$'\t' read -r number branch; do
    [ -n "${number:-}" ] || continue
    if [[ "$branch" =~ $PATTERN ]]; then
      echo "  closing #$number ($branch)"
      run gh pr close "$number" --repo "$GH_REPO" --comment "Closing: CI triage demo reset."
    fi
  done < <(gh pr list --repo "$GH_REPO" --state open --json number,headRefName \
    --jq '.[] | [.number, .headRefName] | @tsv')
else
  echo "  (gh unavailable or unauthenticated; skipping PR cleanup)"
fi

echo
echo "=== 4. Delete demo branches"
CURRENT="$(git rev-parse --abbrev-ref HEAD)"
if [[ "$CURRENT" =~ $PATTERN ]]; then
  DEFAULT_BRANCH="$(git symbolic-ref --quiet refs/remotes/origin/HEAD 2> /dev/null | sed 's|refs/remotes/origin/||')"
  [ -n "$DEFAULT_BRANCH" ] || DEFAULT_BRANCH="main"
  echo "  currently on demo branch $CURRENT; switching to $DEFAULT_BRANCH first"
  run git switch "$DEFAULT_BRANCH"
fi

for branch in $(git branch --format='%(refname:short)' | grep -E "$PATTERN" || true); do
  echo "  deleting local $branch"
  run git branch -D "$branch"
done

git fetch --prune origin --quiet 2> /dev/null || true
for branch in $(git branch -r --format='%(refname:short)' | sed 's|^origin/||' | grep -E "$PATTERN" || true); do
  echo "  deleting remote $branch"
  run git push origin --delete "$branch"
done

echo
if [ "$DRY_RUN" = "1" ]; then
  echo "Dry run only. Nothing was changed."
else
  echo "Reset complete. Working tree:"
  git status --short | sed 's/^/  /'
fi
