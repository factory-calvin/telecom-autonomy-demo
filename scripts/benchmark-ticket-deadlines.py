#!/usr/bin/env python3
"""Benchmark deadline-filtered tickets and report the SQLite query plan."""

import argparse
import math
import sqlite3
import statistics
import time
import urllib.parse
import urllib.request
from pathlib import Path

TICKET_DEADLINE_INDEX = "idx_support_tickets_deadline_filter"


def percentile(values: list[float], percentage: float) -> float:
    """Return a nearest-rank percentile from sorted values."""
    rank = max(1, math.ceil(len(values) * percentage))
    return sorted(values)[rank - 1]


def explain_query_plan(database: Path) -> list[str]:
    """Return the plan for the indexed deadline-filter input scan."""
    connection = sqlite3.connect(f"file:{database}?mode=ro", uri=True)
    try:
        query = """
            SELECT id, created_at, status, acknowledged_at, resolved_at,
                   priority, customer_id
            FROM support_tickets
            ORDER BY created_at DESC
        """
        return [
            row[3]
            for row in connection.execute("EXPLAIN QUERY PLAN " + query)
        ]
    finally:
        connection.close()


def benchmark(url: str, warmups: int, runs: int) -> list[float]:
    """Measure repeated successful requests in milliseconds."""
    durations = []
    for index in range(warmups + runs):
        started = time.perf_counter()
        with urllib.request.urlopen(url, timeout=10) as response:
            if response.status != 200:
                raise RuntimeError(f"Unexpected HTTP status: {response.status}")
            response.read()
        duration = (time.perf_counter() - started) * 1000
        if index >= warmups:
            durations.append(duration)
    return durations


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--url",
        default="http://localhost:8080/api/tickets",
        help="Tickets endpoint",
    )
    parser.add_argument(
        "--db",
        type=Path,
        default=Path("backend/app.db"),
        help="SQLite database used by the backend",
    )
    parser.add_argument("--as-of", default="2026-08-18T10:00:01Z")
    parser.add_argument("--warmups", type=int, default=5)
    parser.add_argument("--runs", type=int, default=30)
    args = parser.parse_args()

    if args.runs < 1 or args.warmups < 0:
        parser.error("--runs must be positive and --warmups cannot be negative")

    query = urllib.parse.urlencode(
        {
            "deadlineState": "RESOLUTION_OVERDUE",
            "page": 0,
            "size": 50,
            "asOf": args.as_of,
        }
    )
    timings = benchmark(f"{args.url}?{query}", args.warmups, args.runs)
    p95 = percentile(timings, 0.95)
    print(f"Requests: {args.runs} after {args.warmups} warmups")
    print(f"p50: {statistics.median(timings):.2f} ms")
    print(f"p95: {p95:.2f} ms")
    print("EXPLAIN QUERY PLAN:")
    plan = explain_query_plan(args.db)
    for detail in plan:
        print(f"  {detail}")

    if p95 >= 500:
        raise SystemExit(f"p95 target missed: {p95:.2f} ms >= 500 ms")
    if not any(TICKET_DEADLINE_INDEX in detail for detail in plan):
        raise SystemExit(f"deadline index missing from query plan: {plan}")


if __name__ == "__main__":
    main()
