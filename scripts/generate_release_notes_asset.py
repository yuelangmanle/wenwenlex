#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
DEFAULT_CHANGELOG = ROOT / "CHANGELOG.md"
DEFAULT_OUTPUT = ROOT / "app" / "src" / "main" / "assets" / "release-notes" / "release-notes.json"
VERSION_RE = re.compile(r"^## \[(?P<version>[^\]]+)\](?: - (?P<date>\d{4}-\d{2}-\d{2}))?$")
SECTION_RE = re.compile(r"^### (?P<title>.+)$")
BULLET_RE = re.compile(r"^- (?P<item>.+)$")


def parse_changelog(changelog_path: Path) -> dict:
    releases: list[dict] = []
    current_release: dict | None = None
    current_section: dict | None = None

    for raw_line in changelog_path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line:
            continue

        version_match = VERSION_RE.match(line)
        if version_match:
            version = version_match.group("version")
            if version == "Unreleased":
                current_release = None
                current_section = None
                continue
            current_release = {
                "version": version,
                "date": version_match.group("date") or "",
                "sections": [],
            }
            releases.append(current_release)
            current_section = None
            continue

        section_match = SECTION_RE.match(line)
        if section_match and current_release is not None:
            current_section = {
                "title": section_match.group("title"),
                "items": [],
            }
            current_release["sections"].append(current_section)
            continue

        bullet_match = BULLET_RE.match(line)
        if bullet_match and current_section is not None:
            current_section["items"].append(bullet_match.group("item"))

    return {"releases": releases}


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate in-app release notes asset from CHANGELOG.md")
    parser.add_argument("--changelog", type=Path, default=DEFAULT_CHANGELOG)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--check", action="store_true", help="Only verify output is up to date")
    args = parser.parse_args()

    payload = parse_changelog(args.changelog)
    rendered = json.dumps(payload, ensure_ascii=False, indent=2) + "\n"

    if args.check:
        existing = args.output.read_text(encoding="utf-8") if args.output.exists() else ""
        if existing != rendered:
            raise SystemExit(f"{args.output} is out of date. Run scripts/generate_release_notes_asset.py")
        return 0

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(rendered, encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
