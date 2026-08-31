#!/usr/bin/env python3
"""Generate a realistic nightly test-failure corpus with known ground truth.

ServiceNow runs roughly half a million tests a night and has humans read the failures.
Reproducing that volume here would be theatre; reproducing its *shape* is the useful
part. This emits a JUnit corpus where a small number of root causes account for a large
number of failures, with the properties that make real triage hard:

  * the same root cause surfaces in several suites, with different files and line numbers
  * some failures are retries of each other, and one retry passes
  * two jobs die during environment setup and produce no test results at all
  * the message text varies within a root cause, so naive string equality will not cluster

Because the generator knows which root cause produced each failure, the output doubles as
an answer key. `ground-truth.json` is written outside the bundle so triage cannot read it
by accident.

Deterministic: the same --seed always produces the same corpus.

Usage:
    ci-nightly-fixture.py                       # writes nightly-triage/
    ci-nightly-fixture.py --out /tmp/nightly --seed 7 --tests 2000
"""

from __future__ import annotations

import argparse
import json
import random
import re
import shutil
from dataclasses import dataclass, field
from datetime import datetime, timedelta, timezone
from pathlib import Path
from xml.etree import ElementTree as ET


@dataclass(frozen=True)
class RootCause:
    id: str
    classification: str
    route: str
    summary: str
    suites: list[str]
    failures: int
    message_templates: list[str]
    stack_templates: list[str]
    target_files: list[str] = field(default_factory=list)
    # Subjects used to name the failing tests, so a name reads like it belongs to its suite.
    name_nouns: list[str] = field(default_factory=list)
    # A flake surfaces as the same test failing then passing on retry.
    retries: bool = False
    # An infra cause kills the job before any test runs, so it has no testcases at all.
    job_level: bool = False
    log_lines: list[str] = field(default_factory=list)


ROOT_CAUSES: list[RootCause] = [
    RootCause(
        id="rc-billing-proration",
        classification="product-code",
        route="agent-fix-now",
        name_nouns=["theProratedAmount", "aMidCycleUpgrade", "theInvoiceTotal", "aPartialMonth"],
        summary=(
            "BillingCycleService prorates with double arithmetic instead of BigDecimal, so "
            "mid-cycle plan changes drift by fractions of a cent and accumulate."
        ),
        suites=[
            "com.example.demo.billing.BillingCycleServiceTest",
            "com.example.demo.billing.ProrationTest",
            "com.example.demo.controller.InvoiceControllerTest",
        ],
        failures=23,
        message_templates=[
            "expected: <{a}> but was: <{b}>",
            "expected <{a}> but got <{b}> (delta {d})",
            "AssertionFailedError: prorated amount expected: <{a}> but was: <{b}>",
        ],
        stack_templates=[
            "at com.example.demo.billing.BillingCycleService.prorate(BillingCycleService.java:{line})",
            "at com.example.demo.billing.BillingCycleService.chargeFor(BillingCycleService.java:{line})",
        ],
        target_files=["backend/src/main/java/com/example/demo/billing/BillingCycleService.java"],
    ),
    RootCause(
        id="rc-usage-timezone",
        classification="product-code",
        route="agent-fix-now",
        name_nouns=["theDailyBucket", "theUsageWindow", "aMidnightRecord", "theRollup"],
        summary=(
            "Usage aggregation buckets records by local date instead of UTC, so the last "
            "hour of each day lands in the wrong bucket."
        ),
        suites=[
            "com.example.demo.usage.UsageAggregatorTest",
            "com.example.demo.controller.UsageRecordControllerTest",
        ],
        failures=14,
        message_templates=[
            "expected {a} records in bucket {bucket} but found {b}",
            "expected: <{a}> but was: <{b}>",
        ],
        stack_templates=[
            "at com.example.demo.usage.UsageAggregator.bucketFor(UsageAggregator.java:{line})",
            "at com.example.demo.usage.UsageAggregator.rollup(UsageAggregator.java:{line})",
        ],
        target_files=["backend/src/main/java/com/example/demo/usage/UsageAggregator.java"],
    ),
    RootCause(
        id="rc-null-plan-npe",
        classification="product-code",
        route="agent-fix-now",
        name_nouns=["aCustomerWithoutAPlan", "theExportRow", "aDeletedPlan", "theCsvHeader"],
        summary=(
            "Customer export dereferences getPlan() without a null check, so any customer "
            "whose plan was deleted throws."
        ),
        suites=[
            "com.example.demo.export.CustomerExportTest",
            "com.example.demo.controller.CustomerControllerTest",
        ],
        failures=16,
        message_templates=[
            'Cannot invoke "com.example.demo.model.Plan.getName()" because "plan" is null',
            "NullPointerException: Cannot invoke Plan.getName() on a null reference",
        ],
        stack_templates=[
            "at com.example.demo.export.CustomerExporter.row(CustomerExporter.java:{line})",
            "at com.example.demo.export.CustomerExporter.export(CustomerExporter.java:{line})",
        ],
        target_files=["backend/src/main/java/com/example/demo/export/CustomerExporter.java"],
    ),
    RootCause(
        id="rc-stale-copy-assertion",
        classification="test-code",
        route="agent-fix-now",
        name_nouns=["thePriorityBadge", "theTicketRow", "theUrgentLabel"],
        summary=(
            'Ticket priority labels were renamed from "Urgent" to "P1" in the source; these '
            "tests still assert the old copy."
        ),
        suites=["tickets-table.test.tsx", "ticket-detail.test.tsx"],
        failures=9,
        message_templates=[
            'Unable to find an element with the text: Urgent',
            'expected "P1" to be "Urgent"',
        ],
        stack_templates=[
            "at __tests__/components/tickets-table.test.tsx:{line}:{col}",
            "at __tests__/components/ticket-detail.test.tsx:{line}:{col}",
        ],
        target_files=["__tests__/components/tickets-table.test.tsx"],
    ),
    RootCause(
        id="rc-device-contract-drift",
        classification="contract-drift",
        route="needs-human",
        name_nouns=["theSimNumber", "theDeviceRow", "theInventoryPayload"],
        summary=(
            "DeviceResponse renamed simNumber to sim_msisdn on the backend; the web client "
            "and two integration suites still read simNumber."
        ),
        suites=[
            "com.example.demo.controller.DeviceControllerTest",
            "devices-table.test.tsx",
            "devices.spec.ts",
        ],
        failures=11,
        message_templates=[
            "expected sim_msisdn to be present, received undefined",
            'no such property "simNumber" in DeviceResponse',
            "expected: <89014103211118510720> but was: <null>",
        ],
        stack_templates=[
            "at com.example.demo.controller.DeviceController$DeviceResponse.from(DeviceController.java:{line})",
            "at components/devices-table.tsx:{line}:{col}",
        ],
        target_files=[
            "backend/src/main/java/com/example/demo/controller/DeviceController.java",
            "components/devices-table.tsx",
        ],
    ),
    RootCause(
        id="rc-artifact-registry-503",
        classification="infra",
        route="platform-team",
        summary="The internal artifact mirror returned 503 for ~4 minutes, so two jobs could not resolve dependencies.",
        suites=[],
        failures=18,
        message_templates=[],
        stack_templates=[],
        job_level=True,
        log_lines=[
            "> Task :compileJava FAILED",
            "FAILURE: Build failed with an exception.",
            "* What went wrong:",
            "Could not resolve all files for configuration ':compileClasspath'.",
            "> Could not resolve org.springframework.boot:spring-boot-starter-web:4.0.0.",
            "  Required by: project :",
            "   > Could not HEAD 'https://artifacts.internal.example.com/maven2/org/springframework/boot/"
            "spring-boot-starter-web/4.0.0/spring-boot-starter-web-4.0.0.pom'.",
            "     > Received status code 503 from server: Service Unavailable",
        ],
    ),
    RootCause(
        id="rc-runner-disk-full",
        classification="infra",
        route="platform-team",
        summary="The e2e runner filled its disk writing traces, so Playwright could not start a browser.",
        suites=[],
        failures=6,
        message_templates=[],
        stack_templates=[],
        job_level=True,
        log_lines=[
            "browserType.launch: Failed to launch chromium because executable doesn't exist",
            "ENOSPC: no space left on device, write '/home/runner/.cache/ms-playwright/traces/trace-0041.zip'",
            "##[error]Process completed with exit code 1.",
            "Filesystem      Size  Used Avail Use% Mounted on",
            "/dev/root        73G   73G     0 100% /",
        ],
    ),
    RootCause(
        id="rc-shared-fixture-order",
        classification="flake",
        route="quarantine-and-retry",
        name_nouns=["theVisibleRows", "thePageWindow", "theRowCount"],
        summary=(
            "A module-scope fixture array is mutated by the first test in the file, so every "
            "later test in that file sees extra rows. Passes in isolation."
        ),
        suites=["usage-table.test.tsx", "customers-table.test.tsx"],
        failures=12,
        message_templates=[
            "expected length 25 to be 24",
            "expected 26 elements, found 27",
        ],
        stack_templates=[
            "at __tests__/components/usage-table.test.tsx:{line}:{col}",
            "at __tests__/components/customers-table.test.tsx:{line}:{col}",
        ],
        retries=True,
    ),
    RootCause(
        id="rc-wall-clock-boundary",
        classification="flake",
        route="quarantine-and-retry",
        name_nouns=["theWindowEnd", "theBillingDay", "theCutoff"],
        summary=(
            "Two suites build an expected date from new Date() and compare against a value "
            "computed a moment later, so they fail when a run straddles midnight UTC."
        ),
        suites=["com.example.demo.usage.UsageWindowTest", "usage.spec.ts"],
        failures=7,
        message_templates=[
            "expected: <2026-03-14> but was: <2026-03-15>",
            "expected window to end 2026-03-15T00:00:00Z, received 2026-03-15T00:00:01Z",
        ],
        stack_templates=[
            "at com.example.demo.usage.UsageWindowTest.windowEndsAtMidnight(UsageWindowTest.java:{line})",
            "at e2e/usage.spec.ts:{line}:{col}",
        ],
        retries=True,
    ),
    RootCause(
        id="rc-spotless-drift",
        classification="hygiene",
        route="agent-fix-now",
        summary="Four Java files were committed without running spotlessApply.",
        suites=[],
        failures=4,
        message_templates=[],
        stack_templates=[],
        job_level=True,
        log_lines=[
            "> Task :spotlessJavaCheck FAILED",
            "The following files had format violations:",
            "    src/main/java/com/example/demo/billing/BillingCycleService.java",
            "    src/main/java/com/example/demo/usage/UsageAggregator.java",
            "    src/main/java/com/example/demo/export/CustomerExporter.java",
            "    src/main/java/com/example/demo/controller/DeviceController.java",
            "Run 'gradlew :spotlessApply' to fix these violations.",
        ],
    ),
]

PASSING_SUITES = [
    "com.example.demo.controller.CustomerControllerTest",
    "com.example.demo.controller.PlanControllerTest",
    "com.example.demo.controller.DashboardControllerTest",
    "com.example.demo.controller.SupportTicketControllerTest",
    "com.example.demo.repository.CustomerRepositoryTest",
    "com.example.demo.repository.UsageRecordRepositoryTest",
    "com.example.demo.billing.InvoiceRenderTest",
    "plans-table.test.tsx",
    "customers-table.test.tsx",
    "use-plans.test.tsx",
    "use-dashboard-stats.test.tsx",
    "dashboard.spec.ts",
    "navigation.spec.ts",
    "api-health.spec.ts",
]

TEST_NAME_VERBS = [
    "returns",
    "rejects",
    "persists",
    "aggregates",
    "renders",
    "filters",
    "paginates",
    "validates",
    "excludes",
    "normalizes",
]
TEST_NAME_NOUNS = [
    "theActiveSubscribers",
    "anUnknownPlan",
    "theProratedAmount",
    "theUsageWindow",
    "theDeviceInventory",
    "theTicketQueue",
    "theInvoiceTotal",
    "theBillingCycle",
    "theCustomerExport",
    "thePlanCatalogue",
]


def test_name(rng: random.Random, suite: str, index: int, nouns: list[str] | None = None) -> str:
    pool = nouns or TEST_NAME_NOUNS
    verb, noun = rng.choice(TEST_NAME_VERBS), rng.choice(pool)
    if suite.endswith((".tsx", ".ts")):
        # Vitest and Playwright titles read as prose.
        spaced = re.sub(r"(?<!^)(?=[A-Z])", " ", noun).lower()
        return f"{verb} {spaced} #{index}"
    return f"{verb}{noun[0].upper()}{noun[1:]}{index}"


def render(template: str, rng: random.Random) -> str:
    a = round(rng.uniform(10, 900), 2)
    return template.format(
        a=a,
        b=round(a + rng.choice([0.01, 0.02, -0.01, 1.0, 0.5]), 2),
        d=round(rng.uniform(0.001, 0.4), 3),
        bucket=f"2026-03-{rng.randint(1, 28):02d}",
        line=rng.randint(24, 480),
        col=rng.randint(3, 60),
    )


def build_corpus(seed: int, total_tests: int) -> tuple[dict, list[dict]]:
    """Return (suite -> testcases) plus the answer key."""
    rng = random.Random(seed)
    suites: dict[str, list[dict]] = {}
    answer_key: list[dict] = []

    def add(suite: str, case: dict) -> None:
        suites.setdefault(suite, []).append(case)

    counter = 0
    for cause in ROOT_CAUSES:
        entry = {
            "root_cause_id": cause.id,
            "classification": cause.classification,
            "route": cause.route,
            "summary": cause.summary,
            "target_files": cause.target_files,
            "failure_count": cause.failures,
            "job_level": cause.job_level,
            "failing_tests": [],
        }

        if cause.job_level:
            answer_key.append(entry)
            continue

        for i in range(cause.failures):
            counter += 1
            suite = cause.suites[i % len(cause.suites)]
            name = test_name(rng, suite, counter, cause.name_nouns)
            case = {
                "name": name,
                "time": round(rng.uniform(0.01, 3.5), 3),
                "failure": {
                    "message": render(rng.choice(cause.message_templates), rng),
                    "type": "AssertionFailedError"
                    if not suite.endswith((".tsx", ".ts"))
                    else "AssertionError",
                    "stack": "\n".join(
                        render(t, rng) for t in rng.sample(cause.stack_templates, k=len(cause.stack_templates))
                    ),
                },
                "retries": 0,
            }
            add(suite, case)
            entry["failing_tests"].append(f"{suite}.{name}" if "." not in suite else f"{suite}:{name}")

            # Flakes get a retry. One in four passes on the retry, which is the single
            # strongest signal that the failure is not a real defect.
            if cause.retries:
                passed_on_retry = rng.random() < 0.25
                retry = dict(case)
                retry["retries"] = 1
                if passed_on_retry:
                    retry = {"name": name, "time": case["time"], "failure": None, "retries": 1}
                add(suite, retry)

        answer_key.append(entry)

    # Pad with passing tests until the corpus reaches the requested size.
    failures_emitted = sum(1 for cases in suites.values() for c in cases if c["failure"])
    while sum(len(c) for c in suites.values()) < total_tests:
        counter += 1
        suite = rng.choice(PASSING_SUITES)
        add(
            suite,
            {
                "name": test_name(rng, suite, counter),
                "time": round(rng.uniform(0.005, 2.0), 3),
                "failure": None,
                "retries": 0,
            },
        )

    assert failures_emitted > 0
    return suites, answer_key


def write_junit(out: Path, suites: dict[str, list[dict]], seed: int) -> None:
    junit = out / "bundle" / "junit"
    junit.mkdir(parents=True, exist_ok=True)
    started = datetime(2026, 3, 15, 2, 0, tzinfo=timezone.utc)

    for index, (suite, cases) in enumerate(sorted(suites.items())):
        root = ET.Element(
            "testsuite",
            {
                "name": suite,
                "tests": str(len(cases)),
                "failures": str(sum(1 for c in cases if c["failure"])),
                "errors": "0",
                "skipped": "0",
                "time": f"{sum(c['time'] for c in cases):.3f}",
                "timestamp": (started + timedelta(seconds=index * 37)).isoformat(),
            },
        )
        for case in cases:
            attrs = {
                "classname": suite,
                "name": case["name"],
                "time": f"{case['time']:.3f}",
            }
            if case["retries"]:
                attrs["retries"] = str(case["retries"])
            node = ET.SubElement(root, "testcase", attrs)
            if case["failure"]:
                failure = ET.SubElement(
                    node,
                    "failure",
                    {"message": case["failure"]["message"], "type": case["failure"]["type"]},
                )
                failure.text = case["failure"]["stack"]

        slug = suite.replace(".", "-").replace("/", "-")
        ET.ElementTree(root).write(junit / f"TEST-{slug}.xml", encoding="utf-8", xml_declaration=True)


def write_logs(out: Path) -> None:
    logs = out / "bundle" / "logs"
    logs.mkdir(parents=True, exist_ok=True)
    for cause in ROOT_CAUSES:
        if not cause.job_level:
            continue
        body = [
            f"##[group]Run nightly job for {cause.id}",
            "Runner: ubuntu-22.04, 4 vCPU, self-hosted pool 'nightly'",
            "",
            *cause.log_lines,
            "",
            "##[error]Process completed with exit code 1.",
        ]
        (logs / f"{cause.id}.txt").write_text("\n".join(body) + "\n")


def main() -> None:
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument("--out", default="nightly-triage", help="output directory")
    parser.add_argument("--seed", type=int, default=20260315, help="deterministic seed")
    parser.add_argument("--tests", type=int, default=2000, help="approximate corpus size")
    args = parser.parse_args()

    out = Path(args.out)
    if out.exists():
        shutil.rmtree(out)
    (out / "bundle").mkdir(parents=True)

    suites, answer_key = build_corpus(args.seed, args.tests)
    write_junit(out, suites, args.seed)
    write_logs(out)

    total = sum(len(c) for c in suites.values())
    failures = sum(1 for cases in suites.values() for c in cases if c["failure"])
    retries = sum(1 for cases in suites.values() for c in cases if c["retries"])
    job_level = sum(1 for c in ROOT_CAUSES if c.job_level)

    (out / "ground-truth.json").write_text(
        json.dumps(
            {
                "seed": args.seed,
                "generated_by": "scripts/ci-nightly-fixture.py",
                "totals": {
                    "tests": total,
                    "test_failures": failures,
                    "retry_entries": retries,
                    "job_level_failures": job_level,
                    "root_causes": len(ROOT_CAUSES),
                },
                "root_causes": answer_key,
            },
            indent=2,
        )
        + "\n"
    )

    (out / "bundle" / "meta.json").write_text(
        json.dumps(
            {
                "mode": "batch",
                "repo": "Factory-Academy/Telecom-Demo-NextJS-Springboot",
                "workflow": "Nightly Test Suite",
                "run_id": f"nightly-{args.seed}",
                "run_url": "",
                "pr_number": None,
                "head_sha": "",
                "head_ref": "main",
                "base_ref": "main",
                "failed_jobs": [c.id for c in ROOT_CAUSES if c.job_level],
                "ground_truth": str(out / "ground-truth.json"),
            },
            indent=2,
        )
        + "\n"
    )

    print(f"Wrote {out}/")
    print(f"  {total} tests, {failures} test failures ({retries} retry entries)")
    print(f"  {job_level} jobs died before running tests")
    print(f"  {len(ROOT_CAUSES)} root causes in {out}/ground-truth.json")
    print(f"  bundle: {out}/bundle (meta.json + junit/ + logs/)")


if __name__ == "__main__":
    main()
