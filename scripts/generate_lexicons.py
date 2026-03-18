#!/usr/bin/env python3
from __future__ import annotations
import csv
import io
import json
import re
import sys
import urllib.request
from collections import OrderedDict
from pathlib import Path
from typing import Optional

ROOT = Path(__file__).resolve().parents[1]
ASSET_DIR = ROOT / "app" / "src" / "main" / "assets" / "books"
ECDICT_URL = "https://raw.githubusercontent.com/skywind3000/ECDICT/master/ecdict.csv"
LICENSE_URL = "https://raw.githubusercontent.com/skywind3000/ECDICT/master/LICENSE"

CATALOGS = [
    {
        "id": "cet4",
        "title": "四级词库",
        "description": "基于 ECDICT 标签筛出的 CET4 词汇",
        "asset": "cet4.json",
        "tags": {"cet4"},
        "coverage": "CET4",
    },
    {
        "id": "cet6",
        "title": "六级词库",
        "description": "基于 ECDICT 标签筛出的 CET6 词汇",
        "asset": "cet6.json",
        "tags": {"cet6"},
        "coverage": "CET6",
    },
    {
        "id": "kaoyan",
        "title": "考研词库",
        "description": "基于 ECDICT 标签筛出的考研英语词汇",
        "asset": "kaoyan.json",
        "tags": {"ky"},
        "coverage": "考研英语",
    },
    {
        "id": "gaozhong",
        "title": "高中词库",
        "description": "基于 ECDICT 标签筛出的高考英语词汇",
        "asset": "gaozhong.json",
        "tags": {"gk"},
        "coverage": "高考英语",
    },
    {
        "id": "ielts-base",
        "title": "雅思基础词库",
        "description": "基于 ECDICT 标签筛出的 IELTS 基础词汇",
        "asset": "ielts-base.json",
        "tags": {"ielts"},
        "coverage": "IELTS",
    },
]

WORD_RE = re.compile(r"^[A-Za-z][A-Za-z' -]*$")
POS_PREFIX_RE = re.compile(r"^[A-Za-z./ ]+\s+")


def read_csv_text() -> str:
    with urllib.request.urlopen(ECDICT_URL) as response:
        return response.read().decode("utf-8")


def normalize_phonetic(raw: str) -> Optional[str]:
    value = raw.strip()
    if not value:
        return None
    return value if value.startswith("/") else f"/{value}/"


def parse_meanings(translation: str) -> list[str]:
    text = (
        translation.replace("\\r\\n", "\n")
        .replace("\\n", "\n")
        .replace("\r\n", "\n")
        .strip()
    )
    if not text:
        return []
    meanings: list[str] = []
    for line in text.splitlines():
        cleaned = line.strip()
        if not cleaned or cleaned.startswith("["):
            continue
        cleaned = POS_PREFIX_RE.sub("", cleaned).strip(" ;,，；")
        for part in re.split(r"[;,，；]", cleaned):
            item = part.strip()
            if item and item not in meanings:
                meanings.append(item)
            if len(meanings) >= 4:
                return meanings
    return meanings


def parse_word_forms(exchange: str) -> list[str]:
    if not exchange:
        return []
    forms: list[str] = []
    seen: set[str] = set()
    for chunk in exchange.split("/"):
        if ":" not in chunk:
            continue
        _, value = chunk.split(":", 1)
        value = value.strip()
        if value and value not in seen:
            forms.append(value)
            seen.add(value)
    return forms[:6]


def parse_pos(pos: str) -> list[str]:
    items = [part.strip() for part in re.split(r"[/,]", pos or "") if part.strip()]
    return items[:4]


def build_word_entry(row: dict[str, str]) -> dict | None:
    word = (row.get("word") or "").strip()
    if not word or not WORD_RE.match(word):
        return None
    meanings = parse_meanings(row.get("translation") or "")
    if not meanings:
        return None
    phonetic = normalize_phonetic(row.get("phonetic") or "")
    exchange = parse_word_forms(row.get("exchange") or "")
    frequency_rank = None
    frq = (row.get("frq") or "").strip()
    if frq.isdigit() and int(frq) > 0:
        frequency_rank = int(frq)
    entry = OrderedDict()
    entry["word"] = word
    if phonetic:
        entry["phonetic"] = phonetic
        entry["phonetic_uk"] = phonetic
    entry["meaning"] = meanings
    pos_items = parse_pos(row.get("pos") or "")
    if pos_items:
        entry["part_of_speech"] = pos_items
    if exchange:
        entry["word_forms"] = exchange
    tags = [part.strip() for part in (row.get("tag") or "").split() if part.strip()]
    if tags:
        entry["tags"] = tags
    if frequency_rank is not None:
        entry["frequency_rank"] = frequency_rank
    return entry


def main() -> int:
    csv_text = read_csv_text()
    reader = csv.DictReader(io.StringIO(csv_text))
    buckets: dict[str, list[dict]] = {catalog["id"]: [] for catalog in CATALOGS}
    seen_words: dict[str, set[str]] = {catalog["id"]: set() for catalog in CATALOGS}

    for row in reader:
        tags = {item.strip() for item in (row.get("tag") or "").split() if item.strip()}
        if not tags:
            continue
        for catalog in CATALOGS:
            if not (tags & catalog["tags"]):
                continue
            entry = build_word_entry(row)
            if entry is None:
                continue
            word = entry["word"].lower()
            if word in seen_words[catalog["id"]]:
                continue
            buckets[catalog["id"]].append(entry)
            seen_words[catalog["id"]].add(word)

    ASSET_DIR.mkdir(parents=True, exist_ok=True)

    manifest = {"books": []}
    for catalog in CATALOGS:
        words = buckets[catalog["id"]]
        payload = OrderedDict(
            id=catalog["id"],
            title=catalog["title"],
            description=catalog["description"],
            language="en",
            category="exam",
            sourceType="builtin",
            words=words,
        )
        (ASSET_DIR / catalog["asset"]).write_text(
            json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
        manifest["books"].append(
            OrderedDict(
                id=catalog["id"],
                title=catalog["title"],
                description=catalog["description"],
                wordCount=len(words),
                asset=catalog["asset"],
                sourceName="ECDICT 标签词库",
                sourceLicense="MIT",
                licenseUrl="https://github.com/skywind3000/ECDICT",
                sourceVersion="ecdict-master",
                coverage=catalog["coverage"],
                reviewedAt="2026-03-19",
            )
        )

    (ASSET_DIR / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    print("Generated built-in lexicons:")
    for item in manifest["books"]:
        print(f"- {item['id']}: {item['wordCount']} words")
    print(f"- license: {LICENSE_URL}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
