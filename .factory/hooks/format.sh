#!/bin/bash
# Auto-format files after edits
input=$(cat)
file_path=$(echo "$input" | jq -r '.tool_input.file_path // empty')

[[ -z "$file_path" ]] && exit 0
[[ ! -f "$file_path" ]] && exit 0

case "$file_path" in
  *.ts|*.tsx|*.js|*.jsx)
    npx prettier --write "$file_path" 2>/dev/null
    ;;
  *.py)
    "$FACTORY_PROJECT_DIR/.venv/bin/ruff" format "$file_path" 2>/dev/null
    ;;
esac
