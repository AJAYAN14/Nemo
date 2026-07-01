#!/usr/bin/env python3
"""Extract an anonymized outline from one or more chat markdown files.

This script intentionally stops at the outline stage. It does not generate a
final skill package, because that assembly step still benefits from judgment
and manual review.
"""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from typing import Iterable


TURN_HEADER_RE = re.compile(
    r"^\s{0,3}#{2,6}\s+(User|Assistant|用户|助手|Human|TRAE)\s*$",
    re.IGNORECASE,
)
CODE_FENCE_RE = re.compile(r"^\s*```")
TRIGGER_PHRASES = (
    "整理成 skill",
    "整理成技能",
    "创建技能",
    "独立技能",
    "提炼 SKILL",
    "总结成技能",
    "沉淀为技能",
    "skill 包",
)
CHAT_HINT_RE = re.compile(r"(##\s*(User|Assistant|用户|助手|Human|TRAE)\b|聊天|对话|chat)", re.IGNORECASE)
CHAT_INTENT_RE = re.compile(r"(聊天|对话|chat|transcript)", re.IGNORECASE)
SKILL_INTENT_RE = re.compile(r"(技能|skill)", re.IGNORECASE)
ACTION_STEP_RE = re.compile(r"(读取|识别|脱敏|提炼|生成|组装|更新|校验|抽取|拆分|重写|过滤)")
VALIDATION_RE = re.compile(r"(验证|校验|检查|审查|扫描)")
DECISION_RE = re.compile(r"(是否|选择|确认|希望|要求|需要|决定)")
PITFALL_RE = re.compile(r"(不要|避免|不能|绝不|不保留)")
ASSET_RE = re.compile(r"(SKILL\.md|references/|examples/|scripts/|outline|总览)")
FILE_NAME_RE = re.compile(
    r"\b[\w\-\u4e00-\u9fff]+\.(?:md|txt|json|py|ts|tsx|js|jsx|css|scss|html|yaml|yml|png|jpg|jpeg|gif|svg|mp4|wav|mp3|srt)\b"
)
CAMEL_ENTITY_RE = re.compile(r"\b[A-Z][A-Za-z0-9]+(?:Agent|Tool|Service|Manager|Controller|Component|Workflow)\b")
SNAKE_ENTITY_RE = re.compile(r"\b[a-z]+(?:_[a-z0-9]+){1,}\b")
MARKDOWN_LINK_RE = re.compile(r"\[([^\]]+)\]\(([^)]+)\)")
URL_RE = re.compile(r"https?://[^\s)]+")
TITLE_RE = re.compile(r"《[^》]{2,80}》")
WINDOWS_PATH_RE = re.compile(r"[A-Za-z]:\\[^\s`]+")
UNIX_PATH_RE = re.compile(r"(?<!\w)/(?:[^\s`/]+/)+[^\s`/]+")
LOCALHOST_PORT_RE = re.compile(r"\b(localhost|127\.0\.0\.1):\d{2,5}\b", re.IGNORECASE)
PORT_CONTEXT_RE = re.compile(r"\b(port\s*[:=]?\s*|端口\s*)\d{2,5}\b", re.IGNORECASE)
MAX_FILES = 100


def normalize_role(raw_role: str) -> str:
    role = raw_role.lower()
    if role in {"assistant", "TRAE", "助手"}:
        return "assistant"
    return "user"


def is_likely_chat_markdown(path: Path) -> bool:
    try:
        text = path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        text = path.read_text(encoding="utf-8", errors="ignore")
    return bool(CHAT_HINT_RE.search(text[:8000]))


def collect_markdown_files(path: Path) -> list[Path]:
    if path.is_file():
        return [path]
    matches = [p for p in sorted(path.rglob("*.md")) if p.is_file() and is_likely_chat_markdown(p)]
    return matches[:MAX_FILES]


def split_turns(text: str) -> list[dict[str, str]]:
    turns: list[dict[str, str]] = []
    current_role: str | None = None
    buffer: list[str] = []
    in_code = False

    for raw_line in text.splitlines():
        if CODE_FENCE_RE.match(raw_line):
            in_code = not in_code

        if not in_code:
            match = TURN_HEADER_RE.match(raw_line)
            if match:
                if current_role is not None and buffer:
                    turns.append(
                        {
                            "role": current_role.lower(),
                            "content": "\n".join(buffer).strip(),
                        }
                    )
                current_role = normalize_role(match.group(1))
                buffer = []
                continue

        if current_role is not None:
            buffer.append(raw_line)

    if current_role is not None and buffer:
        turns.append({"role": current_role, "content": "\n".join(buffer).strip()})

    return [turn for turn in turns if turn["content"]]


def anonymize_text(text: str) -> str:
    text = MARKDOWN_LINK_RE.sub("[sample-link](workspace/path)", text)
    text = URL_RE.sub("sample-url", text)
    text = WINDOWS_PATH_RE.sub("workspace/path", text)
    text = UNIX_PATH_RE.sub("workspace/path", text)
    text = LOCALHOST_PORT_RE.sub(lambda m: f"{m.group(1)}:PORT", text)
    text = PORT_CONTEXT_RE.sub(lambda m: f"{m.group(1)}PORT", text)
    text = TITLE_RE.sub("《示例标题》", text)
    text = re.sub(r"`[^`]*\.[A-Za-z0-9]{1,8}`", "`sample-file`", text)
    text = FILE_NAME_RE.sub("sample-file", text)
    text = CAMEL_ENTITY_RE.sub("SampleComponent", text)
    text = SNAKE_ENTITY_RE.sub("sample_symbol", text)
    return text


def unique_lines(items: Iterable[str]) -> list[str]:
    seen: set[str] = set()
    result: list[str] = []
    for item in items:
        normalized = item.strip()
        if not normalized or normalized in seen:
            continue
        seen.add(normalized)
        result.append(normalized)
    return result


def extract_candidates(turns: list[dict[str, str]]) -> dict[str, list[str]]:
    user_texts = [anonymize_text(t["content"]) for t in turns if t["role"] == "user"]
    assistant_texts = [anonymize_text(t["content"]) for t in turns if t["role"] == "assistant"]

    goals = []
    triggers = []
    steps = []
    validations = []
    decisions = []
    pitfalls = []
    assets = []

    for text in user_texts:
        first_line = next((line.strip() for line in text.splitlines() if line.strip()), "")
        if first_line:
            goals.append(first_line)
        if CHAT_INTENT_RE.search(text) and SKILL_INTENT_RE.search(text):
            if any(phrase in first_line for phrase in TRIGGER_PHRASES):
                triggers.append(first_line)
            else:
                triggers.append("根据聊天记录创建技能")
        for line in text.splitlines():
            cleaned = line.strip("-* \t")
            if any(phrase in cleaned for phrase in TRIGGER_PHRASES):
                triggers.append(cleaned)
            if DECISION_RE.search(cleaned):
                decisions.append(cleaned)
            if PITFALL_RE.search(cleaned):
                pitfalls.append(cleaned)
            if ASSET_RE.search(cleaned):
                assets.append(cleaned)

    for text in assistant_texts:
        for line in text.splitlines():
            cleaned = re.sub(r"^\d+\.\s*", "", line.strip("-* \t"))
            if not cleaned:
                continue
            lowered = cleaned.lower()
            if ACTION_STEP_RE.search(cleaned):
                steps.append(cleaned)
            if VALIDATION_RE.search(cleaned):
                validations.append(cleaned)
            if DECISION_RE.search(cleaned):
                decisions.append(cleaned)
            if PITFALL_RE.search(cleaned):
                pitfalls.append(cleaned)
            if ASSET_RE.search(cleaned):
                assets.append(cleaned)

    return {
        "goal": unique_lines(goals)[:5],
        "triggers": unique_lines(triggers)[:8],
        "stable_steps": unique_lines(steps)[:12],
        "decision_points": unique_lines(decisions)[:10],
        "validation_steps": unique_lines(validations)[:10],
        "pitfalls": unique_lines(pitfalls)[:10],
        "anonymization_notes": [
            "用通用占位符替换真实项目名、路径、文件名和标题",
            "保留方法论和目录角色，不保留可回溯实体",
        ],
        "skill_assets": unique_lines(assets)[:10] or [
            "SKILL.md",
            "references/",
            "examples/",
            "scripts/",
        ],
    }


def render_outline(data: dict[str, list[str]], files: list[Path]) -> str:
    payload = {
        "source_files": [f"chat-{i + 1}.md" for i, _ in enumerate(files)],
        "goal": data["goal"],
        "triggers": data["triggers"],
        "stable_steps": data["stable_steps"],
        "decision_points": data["decision_points"],
        "validation_steps": data["validation_steps"],
        "pitfalls": data["pitfalls"],
        "anonymization_notes": data["anonymization_notes"],
        "skill_assets": data["skill_assets"],
    }
    return json.dumps(payload, ensure_ascii=False, indent=2)


def main() -> int:
    parser = argparse.ArgumentParser(description="Extract an anonymized skill outline from chat markdown.")
    parser.add_argument("source", help="Path to a markdown file or directory containing markdown files.")
    args = parser.parse_args()

    source = Path(args.source)
    if not source.exists():
        raise SystemExit(f"Source does not exist: {source}")

    files = collect_markdown_files(source)
    if not files:
        raise SystemExit(f"No chat-like markdown files found under: {source}")

    all_turns: list[dict[str, str]] = []
    for file_path in files:
        try:
            text = file_path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            text = file_path.read_text(encoding="utf-8", errors="ignore")
        all_turns.extend(split_turns(text))

    user_turns = [turn for turn in all_turns if turn["role"] == "user"]
    assistant_turns = [turn for turn in all_turns if turn["role"] == "assistant"]
    if not user_turns or not assistant_turns:
        raise SystemExit("No structured user/assistant chat turns found")

    extracted = extract_candidates(all_turns)
    print(render_outline(extracted, files))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
