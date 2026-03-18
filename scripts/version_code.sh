#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:?usage: version_code.sh <major.minor>}"

if [[ ! "$VERSION" =~ ^([0-9]+)\.([0-9]+)$ ]]; then
  echo "Version must match <major>.<minor>, for example 1.0" >&2
  exit 1
fi

MAJOR="${BASH_REMATCH[1]}"
MINOR="${BASH_REMATCH[2]}"

if ((10#$MINOR > 9)); then
  echo "Minor version must stay between 0 and 9 so versions roll like 1.0 -> 1.9 -> 2.0" >&2
  exit 1
fi

printf '%d\n' "$((10#$MAJOR * 100 + 10#$MINOR * 10))"
