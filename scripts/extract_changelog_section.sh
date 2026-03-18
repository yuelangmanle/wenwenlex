#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:?usage: extract_changelog_section.sh <major.minor> [CHANGELOG.md]}"
CHANGELOG_FILE="${2:-CHANGELOG.md}"

if [[ ! -f "$CHANGELOG_FILE" ]]; then
  echo "Changelog file not found: $CHANGELOG_FILE" >&2
  exit 1
fi

SECTION="$(awk -v version="$VERSION" '
  $0 ~ "^## \\[" version "\\]" { printing = 1 }
  printing && $0 ~ "^## \\[" && $0 !~ "^## \\[" version "\\]" { exit }
  printing { print }
' "$CHANGELOG_FILE")"

if [[ -z "$SECTION" ]]; then
  echo "No changelog section found for version $VERSION in $CHANGELOG_FILE" >&2
  exit 1
fi

printf '%s\n' "$SECTION"
