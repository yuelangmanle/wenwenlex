#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
LIBS_DIR="$ROOT_DIR/app/libs"
LATEST_RELEASE_API="https://api.github.com/repos/k2-fsa/sherpa-onnx/releases/latest"

resolve_version() {
  local requested="${1:-latest}"
  if [[ "$requested" != "latest" ]]; then
    printf '%s\n' "$requested"
    return
  fi

  curl -fsSL "$LATEST_RELEASE_API" |
    sed -n 's/.*"tag_name":[[:space:]]*"\([^"]*\)".*/\1/p' |
    head -n 1
}

VERSION="$(resolve_version "${1:-latest}")"
if [[ -z "$VERSION" ]]; then
  echo "无法解析 sherpa-onnx 最新版本号。" >&2
  exit 1
fi

VERSION_NO_V="${VERSION#v}"
AAR_NAME="sherpa-onnx-${VERSION_NO_V}.aar"
AAR_URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/${VERSION}/${AAR_NAME}"
CHECKSUM_URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/${VERSION}/checksum.txt"

mkdir -p "$LIBS_DIR"
find "$LIBS_DIR" -maxdepth 1 -type f -name 'sherpa-onnx-*.aar' ! -name "$AAR_NAME" -delete

echo "下载 ${AAR_NAME} ..."
curl -fL "$AAR_URL" -o "$LIBS_DIR/$AAR_NAME"

if curl -fsSL "$CHECKSUM_URL" -o "$LIBS_DIR/checksum-${VERSION}.txt"; then
  echo "已保存 checksum 到 app/libs/checksum-${VERSION}.txt"
fi

echo "Sherpa ONNX Android runtime 已下载到: $LIBS_DIR/$AAR_NAME"
