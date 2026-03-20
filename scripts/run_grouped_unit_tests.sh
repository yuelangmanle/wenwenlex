#!/usr/bin/env bash

set -euo pipefail

COMMON_ARGS=("$@")

run_group() {
  local label="$1"
  shift

  echo "== Running ${label} =="
  ./gradlew :app:testDebugUnitTest "${COMMON_ARGS[@]}" "$@"
}

run_group \
  "worker unit tests" \
  --tests "com.yueliangmanle.danci.core.worker.*"

run_group \
  "data unit tests" \
  --tests "com.yueliangmanle.danci.core.data.*"

run_group \
  "pronunciation and database unit tests" \
  --tests "com.yueliangmanle.danci.core.pronunciation.*" \
  --tests "com.yueliangmanle.danci.core.database.*"

run_group \
  "remaining unit tests" \
  --tests "com.yueliangmanle.danci.core.backup.*" \
  --tests "com.yueliangmanle.danci.core.importer.*" \
  --tests "com.yueliangmanle.danci.core.ai.*" \
  --tests "com.yueliangmanle.danci.core.study.*" \
  --tests "com.yueliangmanle.danci.core.analytics.*" \
  --tests "com.yueliangmanle.danci.feature.*"
