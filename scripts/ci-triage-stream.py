#!/usr/bin/env python3
"""Render a `droid exec -o stream-json` session and record what the run actually cost.

Two modes:

    droid exec -o stream-json ... | tee session.jsonl | ci-triage-stream.py
        Prints a readable trace while the agent works.

    ci-triage-stream.py --finalize session.jsonl verdicts.json
        Reads the completion event and writes the real token counts into
        verdicts.json's `economics` block.

The economics come from the CLI, not from the agent describing its own usage. An agent
cannot see its own token accounting, so anything it reports there is a guess.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

TOOL_PREVIEW = 110


def summarize_tool(event: dict) -> str | None:
    name = event.get("name") or event.get("tool") or event.get("toolName")
    if not name:
        return None
    args = event.get("input") or event.get("args") or {}
    if isinstance(args, dict):
        for key in ("file_path", "path", "command", "pattern", "query"):
            if key in args:
                detail = str(args[key]).replace("\n", " ")
                if len(detail) > TOOL_PREVIEW:
                    detail = detail[: TOOL_PREVIEW - 3] + "..."
                return f"{name}: {detail}"
    return name


def render(stream) -> int:
    """Print a readable trace. Returns a process exit code."""
    exit_code = 0
    for raw in stream:
        raw = raw.strip()
        if not raw:
            continue
        try:
            event = json.loads(raw)
        except json.JSONDecodeError:
            # Not our stream (a wrapper or an installer wrote to stdout); pass it through.
            print(raw)
            continue

        kind = event.get("type")

        if kind == "system" and event.get("subtype") == "init":
            print(f"  model: {event.get('model', 'unknown')}"
                  f"  reasoning: {event.get('reasoning_effort', 'default')}"
                  f"  session: {event.get('session_id', '?')[:8]}")

        elif kind == "message" and event.get("role") == "assistant":
            text = (event.get("text") or "").strip()
            if text:
                for line in text.splitlines():
                    print(f"  {line}")

        elif kind in ("tool_use", "tool_call"):
            detail = summarize_tool(event)
            if detail:
                print(f"  . {detail}")

        elif kind == "error":
            print(f"  ! error: {event.get('message', event)}", file=sys.stderr)
            exit_code = 1

        elif kind in ("completion", "result"):
            usage = event.get("usage") or {}
            duration = event.get("durationMs") or event.get("duration_ms") or 0
            print()
            print(f"  done in {duration / 1000:.1f}s"
                  f"  turns: {event.get('numTurns') or event.get('num_turns') or '?'}"
                  f"  tokens: {usage.get('input_tokens', 0)} in / {usage.get('output_tokens', 0)} out"
                  f"  credits: {usage.get('factory_credits', 0)}")
            if event.get("is_error"):
                exit_code = 1

    return exit_code


def last_completion(path: Path) -> dict | None:
    completion = None
    for raw in path.read_text().splitlines():
        raw = raw.strip()
        if not raw:
            continue
        try:
            event = json.loads(raw)
        except json.JSONDecodeError:
            continue
        if event.get("type") in ("completion", "result"):
            completion = event
    return completion


def finalize(session_path: Path, verdicts_path: Path) -> int:
    if not session_path.exists():
        print(f"no session log at {session_path}; leaving economics untouched", file=sys.stderr)
        return 0
    if not verdicts_path.exists():
        print(f"no verdicts at {verdicts_path}; nothing to annotate", file=sys.stderr)
        return 0

    completion = last_completion(session_path)
    if completion is None:
        print("no completion event found; leaving economics untouched", file=sys.stderr)
        return 0

    try:
        verdicts = json.loads(verdicts_path.read_text())
    except json.JSONDecodeError as exc:
        print(f"verdicts.json is not valid JSON ({exc}); leaving it alone", file=sys.stderr)
        return 1

    usage = completion.get("usage") or {}
    duration_ms = completion.get("durationMs") or completion.get("duration_ms") or 0
    verdicts["economics"] = {
        "model": verdicts.get("economics", {}).get("model") or "unknown",
        "input_tokens": usage.get("input_tokens", 0),
        "output_tokens": usage.get("output_tokens", 0),
        "cache_read_input_tokens": usage.get("cache_read_input_tokens", 0),
        "cache_creation_input_tokens": usage.get("cache_creation_input_tokens", 0),
        "factory_credits": usage.get("factory_credits", 0),
        "wall_clock_seconds": round(duration_ms / 1000, 1),
        "turns": completion.get("numTurns") or completion.get("num_turns") or 0,
        "session_id": completion.get("session_id", ""),
    }

    # The init event carries the model actually used, which beats whatever the agent wrote.
    for raw in session_path.read_text().splitlines():
        try:
            event = json.loads(raw)
        except json.JSONDecodeError:
            continue
        if event.get("type") == "system" and event.get("subtype") == "init" and event.get("model"):
            verdicts["economics"]["model"] = event["model"]
            break

    verdicts_path.write_text(json.dumps(verdicts, indent=2) + "\n")
    econ = verdicts["economics"]
    print(
        f"economics recorded: {econ['model']}, "
        f"{econ['input_tokens']} in / {econ['output_tokens']} out, "
        f"{econ['factory_credits']} credits, {econ['wall_clock_seconds']}s"
    )
    return 0


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--finalize", nargs=2, metavar=("SESSION_JSONL", "VERDICTS_JSON"))
    args = parser.parse_args()

    if args.finalize:
        raise SystemExit(finalize(Path(args.finalize[0]), Path(args.finalize[1])))
    raise SystemExit(render(sys.stdin))


if __name__ == "__main__":
    main()
