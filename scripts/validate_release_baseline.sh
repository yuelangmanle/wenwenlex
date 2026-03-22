#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:?usage: validate_release_baseline.sh <major.minor>}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$ROOT_DIR"

bash scripts/version_code.sh "$VERSION" >/dev/null

required_files=(
  "CHANGELOG.md"
  "README.md"
  "docs/development-progress.md"
  "docs/collaboration-handbook.md"
)

for file in "${required_files[@]}"; do
  if [[ ! -f "$file" ]]; then
    echo "Missing required baseline file: $file" >&2
    exit 1
  fi
done

shopt -s nullglob
smoke_files=(docs/release-v"${VERSION}"-*.md)
shopt -u nullglob

if (( ${#smoke_files[@]} == 0 )); then
  echo "Missing smoke document for version $VERSION under docs/release-v${VERSION}-*.md" >&2
  exit 1
fi

grep -q "^## \\[$VERSION\\]" CHANGELOG.md || {
  echo "CHANGELOG.md is missing section [$VERSION]" >&2
  exit 1
}

grep -q "v$VERSION" README.md || {
  echo "README.md does not mention v$VERSION" >&2
  exit 1
}

grep -q "v$VERSION" docs/development-progress.md || {
  echo "docs/development-progress.md does not mention v$VERSION" >&2
  exit 1
}

grep -q "v$VERSION" docs/collaboration-handbook.md || {
  echo "docs/collaboration-handbook.md does not mention v$VERSION" >&2
  exit 1
}

primary_smoke="${smoke_files[0]}"

grep -q "v$VERSION" "$primary_smoke" || {
  echo "$primary_smoke does not mention v$VERSION" >&2
  exit 1
}

grep -q "Android CI" "$primary_smoke" || {
  echo "$primary_smoke is missing Android CI section" >&2
  exit 1
}

grep -q "Android Release" "$primary_smoke" || {
  echo "$primary_smoke is missing Android Release section" >&2
  exit 1
}

grep -q "Release 页面" "$primary_smoke" || {
  echo "$primary_smoke is missing Release 页面 section" >&2
  exit 1
}

grep -q "本机归档" "$primary_smoke" || {
  echo "$primary_smoke is missing 本机归档 section" >&2
  exit 1
}

printf 'Release baseline check passed for %s\n' "$VERSION"
