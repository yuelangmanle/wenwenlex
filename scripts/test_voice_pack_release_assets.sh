#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PACKAGE_SCRIPT="$ROOT_DIR/scripts/package_voice_packs.sh"
CATALOG_PATH="$ROOT_DIR/app/src/main/assets/pronunciation/voice-pack-manifest.json"
TMP_DIR="$(mktemp -d)"

cleanup() {
  rm -rf "$TMP_DIR"
}

trap cleanup EXIT

bash "$PACKAGE_SCRIPT" "$TMP_DIR"

python3 - "$CATALOG_PATH" "$TMP_DIR" <<'PY'
import json
import pathlib
import sys
import urllib.parse
import zipfile

catalog_path = pathlib.Path(sys.argv[1])
output_dir = pathlib.Path(sys.argv[2])
catalog = json.loads(catalog_path.read_text(encoding="utf-8"))
checksum_path = output_dir / "wenwenlex-voice-pack-checksums.txt"

assert checksum_path.exists(), "缺少语音包 checksum 产物。"
checksum_text = checksum_path.read_text(encoding="utf-8")

native_packs = [
    item
    for item in catalog.get("voicePacks", [])
    if item.get("engineType") == "sherpa_onnx"
]
assert native_packs, "内置语音包 catalog 中缺少 native release 条目。"

for pack in native_packs:
    download_url = pack.get("downloadUrl", "")
    manifest_url = pack.get("manifestUrl", "")
    assert "/releases/latest/download/" in download_url, (
        f"{pack['id']} downloadUrl 必须使用 latest release 下载地址。"
    )
    assert "/releases/latest/download/" in manifest_url, (
        f"{pack['id']} manifestUrl 必须使用 latest release 下载地址。"
    )

    archive_name = pathlib.PurePosixPath(urllib.parse.urlparse(download_url).path).name
    manifest_name = pathlib.PurePosixPath(urllib.parse.urlparse(manifest_url).path).name
    archive_path = output_dir / archive_name
    manifest_path = output_dir / manifest_name

    assert archive_path.exists(), f"缺少打包产物: {archive_name}"
    assert manifest_path.exists(), f"缺少 manifest 产物: {manifest_name}"
    assert archive_name in checksum_text, f"checksum 文件缺少 {archive_name}"
    assert manifest_name in checksum_text, f"checksum 文件缺少 {manifest_name}"

    packaged_manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    assert packaged_manifest.get("id") == pack["id"], (
        f"{manifest_name} 的 id 与 catalog 不一致。"
    )

    with zipfile.ZipFile(archive_path) as archive:
        names = archive.namelist()
        assert "manifest.json" in names, f"{archive_name} 缺少 manifest.json"
        assert all("/manifest.json" not in name for name in names if name != "manifest.json"), (
            f"{archive_name} 不应把 manifest.json 打到二级目录。"
        )

        manifest_from_zip = json.loads(archive.read("manifest.json"))
        assert manifest_from_zip.get("id") == pack["id"], (
            f"{archive_name} 内 manifest id 与 catalog 不一致。"
        )

        entry_files = manifest_from_zip.get("entryFiles") or []
        assert entry_files, f"{archive_name} manifest 缺少 entryFiles。"
        for relative_path in entry_files:
            assert relative_path in names, (
                f"{archive_name} 缺少 entryFiles 中声明的文件: {relative_path}"
            )
            assert not relative_path.startswith(f"{pack['id']}/"), (
                f"{archive_name} 不应把 pack 根目录名打进压缩包。"
            )

print("voice pack release assets OK")
PY
