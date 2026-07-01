from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "extract_skill_outline.py"


def run_script(source: Path) -> dict:
    completed = subprocess.run(
        [sys.executable, str(SCRIPT), str(source)],
        check=True,
        capture_output=True,
        text=True,
    )
    return json.loads(completed.stdout)


def test_script_outputs_expected_schema_for_sample_chat() -> None:
    source = ROOT / "examples" / "sample-chat.md"
    payload = run_script(source)

    assert payload["source_files"] == ["chat-1.md"]
    assert "decision_points" in payload
    assert "anonymization_notes" in payload
    assert "skill_assets" in payload
    assert payload["triggers"]


def test_script_does_not_leak_original_file_name(tmp_path: Path) -> None:
    source = tmp_path / "真实项目总结.md"
    source.write_text(
        "## User\n请把这些聊天整理成一个独立技能。\n\n## Assistant\n先读取聊天源，再执行严格脱敏。\n",
        encoding="utf-8",
    )

    payload = run_script(source)

    assert payload["source_files"] == ["chat-1.md"]
    dumped = json.dumps(payload, ensure_ascii=False)
    assert "真实项目总结" not in dumped


def test_script_fails_when_no_structured_turns_exist(tmp_path: Path) -> None:
    source = tmp_path / "not-chat.md"
    source.write_text("# 普通文档\n这不是聊天记录。\n", encoding="utf-8")

    completed = subprocess.run(
        [sys.executable, str(SCRIPT), str(source)],
        capture_output=True,
        text=True,
    )

    assert completed.returncode != 0
