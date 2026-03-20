#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
LIBS_DIR="$ROOT_DIR/app/libs"
JNI_LIBS_DIR="$ROOT_DIR/app/src/main/jniLibs"
LATEST_RELEASE_API="https://api.github.com/repos/k2-fsa/sherpa-onnx/releases/latest"
DEFAULT_VERSION="v1.12.31"

resolve_version() {
  local requested="${1:-$DEFAULT_VERSION}"
  if [[ "$requested" != "latest" ]]; then
    printf '%s\n' "$requested"
    return
  fi

  curl -fsSL "$LATEST_RELEASE_API" |
    sed -n 's/.*"tag_name":[[:space:]]*"\([^"]*\)".*/\1/p' |
    head -n 1
}

VERSION="$(resolve_version "${1:-$DEFAULT_VERSION}")"
if [[ -z "$VERSION" ]]; then
  echo "无法解析 sherpa-onnx 最新版本号。" >&2
  exit 1
fi

ARCHIVE_NAME="sherpa-onnx-${VERSION}-android.tar.bz2"
ARCHIVE_URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/${VERSION}/${ARCHIVE_NAME}"
VERSION_FILE="$LIBS_DIR/sherpa-onnx-android-version.txt"
SHA256_FILE="$LIBS_DIR/sherpa-onnx-android-${VERSION}.sha256"
TMP_DIR="$(mktemp -d)"
ARCHIVE_PATH="$TMP_DIR/$ARCHIVE_NAME"

mkdir -p "$LIBS_DIR"
mkdir -p "$JNI_LIBS_DIR"
find "$LIBS_DIR" -maxdepth 1 -type f -name 'sherpa-onnx-android-*.sha256' -delete
rm -f "$VERSION_FILE"
rm -rf "$JNI_LIBS_DIR"
mkdir -p "$JNI_LIBS_DIR"

cleanup() {
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT

echo "下载 ${ARCHIVE_NAME} ..."
curl -fL "$ARCHIVE_URL" -o "$ARCHIVE_PATH"

echo "解压官方 Android JNI 库 ..."
tar -xjf "$ARCHIVE_PATH" -C "$TMP_DIR"
cp -R "$TMP_DIR/jniLibs/." "$JNI_LIBS_DIR/"

shasum -a 256 "$ARCHIVE_PATH" | awk '{print $1}' > "$SHA256_FILE"
printf '%s\n' "$VERSION" > "$VERSION_FILE"

echo "Sherpa ONNX Android JNI runtime 已同步到: $JNI_LIBS_DIR"
echo "版本记录: $VERSION_FILE"
echo "归档 SHA-256: $SHA256_FILE"
