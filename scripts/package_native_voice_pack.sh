#!/usr/bin/env bash

set -euo pipefail

SOURCE_DIR="${1:?usage: package_native_voice_pack.sh <source_dir> <output_zip>}"
OUTPUT_ZIP="${2:?usage: package_native_voice_pack.sh <source_dir> <output_zip>}"

python3 - "$SOURCE_DIR" "$OUTPUT_ZIP" <<'PY'
import hashlib
import json
import pathlib
import sys
import zipfile

source_dir = pathlib.Path(sys.argv[1]).resolve()
output_zip = pathlib.Path(sys.argv[2]).resolve()

if not source_dir.is_dir():
    raise SystemExit(f"语音包源目录不存在: {source_dir}")

manifest_path = source_dir / "manifest.json"
if not manifest_path.is_file():
    raise SystemExit(f"语音包源目录缺少 manifest.json: {manifest_path}")

try:
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
except json.JSONDecodeError as exc:
    raise SystemExit(f"manifest.json 不是合法 JSON: {exc}") from exc

pack_id = str(manifest.get("id", "")).strip()
if not pack_id:
    raise SystemExit("manifest.json 缺少有效的 id。")

entry_files = manifest.get("entryFiles")
if not isinstance(entry_files, list) or not entry_files:
    raise SystemExit("manifest.json 缺少非空 entryFiles。")

native_block = manifest.get("native") or manifest.get("runtime") or manifest
licenses = native_block.get("licenses")
if not isinstance(licenses, list) or not licenses:
    raise SystemExit("manifest.json 缺少 native licenses 声明。")

def normalize_relative_path(value: object, label: str) -> pathlib.PurePosixPath:
    if not isinstance(value, str):
        raise SystemExit(f"{label} 必须是字符串。")
    path_text = value.strip()
    if not path_text:
        raise SystemExit(f"{label} 不能为空。")
    path = pathlib.PurePosixPath(path_text)
    if path.is_absolute() or ".." in path.parts:
        raise SystemExit(f"{label} 必须是 pack 内的相对路径: {path_text}")
    return path

for entry_file in entry_files:
    relative_path = normalize_relative_path(entry_file, "entryFiles")
    file_path = source_dir / relative_path.as_posix()
    if not file_path.is_file():
        raise SystemExit(f"entryFiles 中声明的文件不存在: {relative_path.as_posix()}")

for license_item in licenses:
    if isinstance(license_item, dict) and license_item.get("file"):
        relative_path = normalize_relative_path(license_item.get("file"), "license.file")
        file_path = source_dir / relative_path.as_posix()
        if not file_path.is_file():
            raise SystemExit(f"licenses 中声明的文件不存在: {relative_path.as_posix()}")

all_files = sorted(
    [path for path in source_dir.rglob("*") if path.is_file()],
    key=lambda path: path.relative_to(source_dir).as_posix(),
)

output_zip.parent.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(output_zip, "w", compression=zipfile.ZIP_DEFLATED) as archive:
    for file_path in all_files:
        relative_name = file_path.relative_to(source_dir).as_posix()
        zip_info = zipfile.ZipInfo(relative_name)
        zip_info.date_time = (2026, 1, 1, 0, 0, 0)
        zip_info.compress_type = zipfile.ZIP_DEFLATED
        zip_info.external_attr = 0o100644 << 16
        archive.writestr(zip_info, file_path.read_bytes())

checksum = hashlib.sha256(output_zip.read_bytes()).hexdigest()
print(checksum)
PY
