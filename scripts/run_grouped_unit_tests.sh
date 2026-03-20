#!/usr/bin/env bash

set -euo pipefail

COMMON_ARGS=("$@")
GRADLE_ARGS=(--stacktrace --info)
TEST_TIMEOUT_SECONDS="${GRADLE_TEST_TIMEOUT_SECONDS:-300}"

resolve_timeout_command() {
  if command -v timeout >/dev/null 2>&1; then
    printf 'timeout --signal=TERM --kill-after=30s %ss' "$TEST_TIMEOUT_SECONDS"
    return
  fi

  if command -v gtimeout >/dev/null 2>&1; then
    printf 'gtimeout --signal=TERM --kill-after=30s %ss' "$TEST_TIMEOUT_SECONDS"
    return
  fi

  printf ''
}

TIMEOUT_COMMAND="$(resolve_timeout_command)"

run_group() {
  local label="$1"
  shift

  echo "== Running ${label} =="
  if [ -n "$TIMEOUT_COMMAND" ]; then
    read -r -a timeout_parts <<<"$TIMEOUT_COMMAND"
    if "${timeout_parts[@]}" ./gradlew :app:testDebugUnitTest "${COMMON_ARGS[@]}" "${GRADLE_ARGS[@]}" "$@"; then
      return 0
    else
      local status="$?"
      if [ "$status" -eq 124 ]; then
        echo "!! ${label} exceeded ${TEST_TIMEOUT_SECONDS}s"
      fi
      return "$status"
    fi
  fi

  ./gradlew :app:testDebugUnitTest "${COMMON_ARGS[@]}" "${GRADLE_ARGS[@]}" "$@"
}

run_group \
  "worker unit tests: DailyReminderSchedulerTest" \
  --tests "com.yueliangmanle.danci.core.worker.DailyReminderSchedulerTest"

run_group \
  "worker unit tests: VoicePackInstallerTest" \
  --tests "com.yueliangmanle.danci.core.worker.VoicePackInstallerTest"

run_group \
  "data unit tests: VoicePackRepositoryTest" \
  --tests "com.yueliangmanle.danci.core.data.VoicePackRepositoryTest"

run_group \
  "data unit tests: WordAudioRepositoryTest" \
  --tests "com.yueliangmanle.danci.core.data.WordAudioRepositoryTest"

run_group \
  "pronunciation unit tests: service and runtime" \
  --tests "com.yueliangmanle.danci.core.pronunciation.DictionaryAudioServiceTest" \
  --tests "com.yueliangmanle.danci.core.pronunciation.SherpaOnnxRuntimeTest" \
  --tests "com.yueliangmanle.danci.core.pronunciation.WordPronunciationNormalizerTest"

run_group \
  "pronunciation unit tests: OfflineTtsEngineTest" \
  --tests "com.yueliangmanle.danci.core.pronunciation.OfflineTtsEngineTest"

run_group \
  "pronunciation unit tests: PronunciationOrchestratorTest" \
  --tests "com.yueliangmanle.danci.core.pronunciation.PronunciationOrchestratorTest"

run_group \
  "pronunciation unit tests: native runtime path" \
  --tests "com.yueliangmanle.danci.core.pronunciation.NativeOfflineWordTtsEngineTest"

run_group \
  "database unit tests" \
  --tests "com.yueliangmanle.danci.core.database.DanciDatabaseTest"

run_group \
  "remaining unit tests: backup and importer" \
  --tests "com.yueliangmanle.danci.core.backup.*" \
  --tests "com.yueliangmanle.danci.core.importer.*"

run_group \
  "remaining unit tests: ai analytics study" \
  --tests "com.yueliangmanle.danci.core.ai.*" \
  --tests "com.yueliangmanle.danci.core.study.*" \
  --tests "com.yueliangmanle.danci.core.analytics.*"

run_group \
  "remaining unit tests: feature view models" \
  --tests "com.yueliangmanle.danci.feature.pronunciation.PronunciationSettingsViewModelTest" \
  --tests "com.yueliangmanle.danci.feature.study.StudyViewModelTest"
