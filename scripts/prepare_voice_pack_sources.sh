#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUTPUT_ROOT="${1:?usage: prepare_voice_pack_sources.sh <output_root> [template_root] [cache_dir]}"
TEMPLATE_ROOT="${2:-$ROOT_DIR/distribution/voice-packs}"
CACHE_DIR="${3:-${VOICE_PACK_DOWNLOAD_CACHE_DIR:-${RUNNER_TEMP:-$ROOT_DIR/.tmp}/voice-pack-downloads}}"

mkdir -p "$OUTPUT_ROOT" "$CACHE_DIR"

python3 - "$TEMPLATE_ROOT" "$OUTPUT_ROOT" "$CACHE_DIR" <<'PY'
import hashlib
import json
import pathlib
import shutil
import tarfile
import tempfile
import urllib.request
import sys

template_root = pathlib.Path(sys.argv[1]).resolve()
output_root = pathlib.Path(sys.argv[2]).resolve()
cache_dir = pathlib.Path(sys.argv[3]).resolve()


def read_json(path: pathlib.Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def sha256_file(path: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def normalize_relative_path(value: str, label: str) -> pathlib.PurePosixPath:
    if not isinstance(value, str):
        raise SystemExit(f"{label} 必须是字符串。")
    text = value.strip()
    if not text:
        raise SystemExit(f"{label} 不能为空。")
    relative = pathlib.PurePosixPath(text)
    if relative.is_absolute() or ".." in relative.parts:
        raise SystemExit(f"{label} 必须是相对路径: {text}")
    return relative


def download_file(url: str, target: pathlib.Path) -> None:
    target.parent.mkdir(parents=True, exist_ok=True)
    if target.exists():
        return
    request = urllib.request.Request(
        url,
        headers={"User-Agent": "wenwenlex-voice-pack-builder/1.0"},
    )
    with urllib.request.urlopen(request) as response, target.open("wb") as output:
        shutil.copyfileobj(response, output)


def safe_extract(archive_path: pathlib.Path, target_dir: pathlib.Path) -> None:
    with tarfile.open(archive_path, "r:*") as archive:
        for member in archive.getmembers():
            member_path = target_dir / member.name
            resolved_parent = member_path.parent.resolve()
            target_root = target_dir.resolve()
            if target_root not in [resolved_parent, *resolved_parent.parents]:
                raise SystemExit(f"检测到非法压缩包路径: {member.name}")
        archive.extractall(target_dir)


def copy_file(source: pathlib.Path, target: pathlib.Path) -> None:
    if not source.is_file():
        raise SystemExit(f"缺少源文件: {source}")
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, target)


def copy_directory(source: pathlib.Path, target: pathlib.Path) -> None:
    if not source.is_dir():
        raise SystemExit(f"缺少源目录: {source}")
    if target.exists():
        shutil.rmtree(target)
    shutil.copytree(source, target)


def iter_files(root: pathlib.Path):
    return sorted(
        [path for path in root.rglob("*") if path.is_file()],
        key=lambda item: item.relative_to(root).as_posix(),
    )


for source_json_path in sorted(template_root.glob("*/source.json")):
    pack_template_dir = source_json_path.parent
    manifest_template_path = pack_template_dir / "manifest.template.json"
    if not manifest_template_path.is_file():
        raise SystemExit(f"缺少 manifest.template.json: {manifest_template_path}")

    source_config = read_json(source_json_path)
    manifest = read_json(manifest_template_path)
    pack_id = str(manifest.get("id", "")).strip()
    if not pack_id:
        raise SystemExit(f"{manifest_template_path} 缺少 id。")

    upstream = source_config.get("upstream") or {}
    asset_url = str(upstream.get("assetUrl", "")).strip()
    asset_sha256 = str(upstream.get("sha256", "")).strip().removeprefix("sha256:")
    asset_name = str(upstream.get("assetName", "")).strip() or pathlib.PurePosixPath(asset_url).name
    if not asset_url or not asset_sha256 or not asset_name:
        raise SystemExit(f"{source_json_path} upstream 配置不完整。")

    archive_path = cache_dir / asset_name
    download_file(asset_url, archive_path)
    actual_archive_sha256 = sha256_file(archive_path)
    if actual_archive_sha256 != asset_sha256:
        raise SystemExit(
            f"{asset_name} SHA256 不匹配。expected={asset_sha256} actual={actual_archive_sha256}"
        )

    output_pack_dir = output_root / pack_template_dir.name
    if output_pack_dir.exists():
        shutil.rmtree(output_pack_dir)
    output_pack_dir.mkdir(parents=True, exist_ok=True)

    with tempfile.TemporaryDirectory(prefix=f"{pack_id}-extract-") as extract_tmp:
        extract_root = pathlib.Path(extract_tmp)
        safe_extract(archive_path, extract_root)

        payload_checksums = {}
        entry_files = []

        for item in source_config.get("payloadFiles", []):
            source_path = extract_root / normalize_relative_path(item.get("source", ""), "payloadFiles.source")
            target_relative = normalize_relative_path(item.get("target", ""), "payloadFiles.target")
            target_path = output_pack_dir / target_relative
            copy_file(source_path, target_path)
            entry_files.append(target_relative.as_posix())
            payload_checksums[target_relative.as_posix()] = sha256_file(target_path)

        for item in source_config.get("payloadDirectories", []):
            source_path = extract_root / normalize_relative_path(item.get("source", ""), "payloadDirectories.source")
            target_relative = normalize_relative_path(item.get("target", ""), "payloadDirectories.target")
            target_path = output_pack_dir / target_relative
            copy_directory(source_path, target_path)
            for file_path in iter_files(target_path):
                relative_path = file_path.relative_to(output_pack_dir).as_posix()
                payload_checksums[relative_path] = sha256_file(file_path)

        for item in source_config.get("licenseFiles", []):
            source_path = extract_root / normalize_relative_path(item.get("source", ""), "licenseFiles.source")
            target_relative = normalize_relative_path(item.get("target", ""), "licenseFiles.target")
            copy_file(source_path, output_pack_dir / target_relative)

        for item in source_config.get("localFiles", []):
            source_path = pack_template_dir / normalize_relative_path(item.get("source", ""), "localFiles.source")
            target_relative = normalize_relative_path(item.get("target", ""), "localFiles.target")
            if source_path.is_dir():
                copy_directory(source_path, output_pack_dir / target_relative)
            else:
                copy_file(source_path, output_pack_dir / target_relative)

    if not entry_files:
        raise SystemExit(f"{source_json_path} 缺少 payloadFiles。")
    if not payload_checksums:
        raise SystemExit(f"{source_json_path} 未生成 payloadChecksums。")

    manifest["entryFiles"] = entry_files
    manifest["payloadChecksums"] = dict(sorted(payload_checksums.items()))

    native_block = manifest.get("native")
    runtime_block = manifest.get("runtime")
    metadata_block = native_block if isinstance(native_block, dict) else runtime_block if isinstance(runtime_block, dict) else manifest
    metadata_block["estimatedStorageBytes"] = sum(path.stat().st_size for path in iter_files(output_pack_dir))

    manifest_path = output_pack_dir / "manifest.json"
    manifest_path.write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2, sort_keys=False) + "\n",
        encoding="utf-8",
    )

print(output_root)
PY
