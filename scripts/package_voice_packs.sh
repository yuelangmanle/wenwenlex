#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SOURCE_ROOT="$ROOT_DIR/distribution/voice-packs"
PACKAGE_SCRIPT="$ROOT_DIR/scripts/package_native_voice_pack.sh"
OUTPUT_DIR="${1:?usage: package_voice_packs.sh <output_dir>}"
CHECKSUM_FILE="$OUTPUT_DIR/wenwenlex-voice-pack-checksums.txt"
ASSET_NOTES_FILE="$OUTPUT_DIR/voice-pack-assets.md"

mkdir -p "$OUTPUT_DIR"
: > "$CHECKSUM_FILE"

cat > "$ASSET_NOTES_FILE" <<'EOF'
### Native Voice Packs

EOF

while IFS= read -r manifest_file; do
  pack_dir="$(dirname "$manifest_file")"
  pack_id="$(
    python3 - "$manifest_file" <<'PY'
import json
import pathlib
import sys

manifest_path = pathlib.Path(sys.argv[1])
manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
pack_id = str(manifest.get("id", "")).strip()
if not pack_id:
    raise SystemExit("manifest.json 缺少 id。")
print(pack_id)
PY
  )"

  archive_name="wenwenlex-voice-pack-${pack_id}.zip"
  manifest_asset_name="wenwenlex-voice-pack-${pack_id}-manifest.json"
  archive_path="$OUTPUT_DIR/$archive_name"
  manifest_asset_path="$OUTPUT_DIR/$manifest_asset_name"

  archive_checksum="$(bash "$PACKAGE_SCRIPT" "$pack_dir" "$archive_path")"
  cp "$manifest_file" "$manifest_asset_path"

  manifest_checksum="$(
    python3 - "$manifest_asset_path" <<'PY'
import hashlib
import pathlib
import sys

path = pathlib.Path(sys.argv[1])
print(hashlib.sha256(path.read_bytes()).hexdigest())
PY
  )"

  printf '%s  %s\n' "$archive_checksum" "$archive_name" >> "$CHECKSUM_FILE"
  printf '%s  %s\n' "$manifest_checksum" "$manifest_asset_name" >> "$CHECKSUM_FILE"

  {
    echo "- \`$archive_name\`"
    echo "- \`$manifest_asset_name\`"
  } >> "$ASSET_NOTES_FILE"
done < <(find "$SOURCE_ROOT" -mindepth 2 -maxdepth 2 -name manifest.json | sort)

echo "- \`$(basename "$CHECKSUM_FILE")\`" >> "$ASSET_NOTES_FILE"
