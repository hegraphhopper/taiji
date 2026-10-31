"""Validate the task's final check report before any archive mutation."""
from __future__ import annotations
import re
from pathlib import Path

REPORT_LIMIT = 256 * 1024
FINAL_RESULTS = {"passed", "failed", "blocked", "skipped"}


def validate_check_report(task_dir: Path, task_id: str) -> str | None:
    try:
        report = task_dir / "check.md"
        resolved = report.resolve(strict=True)
        if not resolved.is_relative_to(task_dir.resolve()) or not resolved.is_file():
            return "check.md 路径无效"
        with resolved.open("rb") as stream:
            raw = stream.read(REPORT_LIMIT + 1)
        if len(raw) > REPORT_LIMIT:
            return "check.md 超过 256 KiB"
        text = raw.decode("utf-8").replace("\r\n", "\n")
        match = re.fullmatch(r"---\n(.*?)\n---\n(.*)", text, re.S)
        if not match:
            return "check.md 缺少元数据头或正文"
        fields = {}
        for line in match[1].splitlines():
            key, sep, value = line.partition(":")
            if not sep or key.strip() not in {"task_id", "result"} or key.strip() in fields:
                return "check.md 元数据无效"
            fields[key.strip()] = value.strip().strip('\"\'')
        if not task_id or fields.get("task_id") != task_id:
            return "check.md 的 task_id 与当前任务不匹配"
        if fields.get("result") not in FINAL_RESULTS:
            return "check.md 尚未结束或 result 无效"
        body = match[2].strip()
        if not body:
            return "check.md 正文为空"
        if fields["result"] == "skipped" and not re.search(r"^跳过原因：\s*\S.+", body, re.M):
            return "skipped 报告必须填写 跳过原因：具体原因"
        return None
    except (OSError, UnicodeError, RuntimeError):
        return "check.md 缺失或不可读取"
