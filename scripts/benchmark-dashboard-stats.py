#!/usr/bin/env python3
"""Benchmark dashboard stats and report the current-cycle SQLite query plan."""

import argparse
import math
import sqlite3
import statistics
import time
import urllib.request
from datetime import datetime, timezone
from pathlib import Path


def percentile(values: list[float], percentage: float) -> float:
    """Return a nearest-rank percentile from sorted values."""
    rank = max(1, math.ceil(len(values) * percentage))
    return sorted(values)[rank - 1]


def explain_query_plan(database: Path) -> list[str]:
    """Return the query plan used by the current-cycle aggregate."""
    connection = sqlite3.connect(f"file:{database}?mode=ro", uri=True)
    try:
        cycle_start = datetime.now(timezone.utc).replace(
            day=1, hour=0, minute=0, second=0, microsecond=0
        )
        if cycle_start.month == 12:
            cycle_end = cycle_start.replace(year=cycle_start.year + 1, month=1)
        else:
            cycle_end = cycle_start.replace(month=cycle_start.month + 1)
        customer_ids = [row[0] for row in connection.execute("SELECT id FROM customers")]
        placeholders = ",".join("?" for _ in customer_ids)
        query = f"""
            SELECT customer_id, SUM(quantity)
            FROM usage_records
            WHERE customer_id IN ({placeholders})
              AND type = ?
              AND recorded_at >= ?
              AND recorded_at < ?
            GROUP BY customer_id
        """
        parameters = [
            *customer_ids,
            "DATA",
            cycle_start.strftime("%Y-%m-%d %H:%M:%S.000"),
            cycle_end.strftime("%Y-%m-%d %H:%M:%S.000"),
        ]
        return [
            row[3]
            for row in connection.execute(
                "EXPLAIN QUERY PLAN " + query, parameters
            )
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
        default="http://localhost:8080/api/dashboard/stats",
        help="Dashboard stats endpoint",
    )
    parser.add_argument(
        "--db",
        type=Path,
        default=Path("backend/app.db"),
        help="SQLite database used by the backend",
    )
    parser.add_argument("--warmups", type=int, default=5)
    parser.add_argument("--runs", type=int, default=30)
    args = parser.parse_args()

    if args.runs < 1 or args.warmups < 0:
        parser.error("--runs must be positive and --warmups cannot be negative")

    timings = benchmark(args.url, args.warmups, args.runs)
    print(f"Requests: {args.runs} after {args.warmups} warmups")
    print(f"p50: {statistics.median(timings):.2f} ms")
    print(f"p95: {percentile(timings, 0.95):.2f} ms")
    print("EXPLAIN QUERY PLAN:")
    for detail in explain_query_plan(args.db):
        print(f"  {detail}")


if __name__ == "__main__":
    main()
