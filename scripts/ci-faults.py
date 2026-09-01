#!/usr/bin/env python3
"""Seeded CI failure catalog for the triage demo.

Each fault is a small, reversible, believable change that makes the CI pipeline red in
exactly one way. `scripts/ci-seed-failure.sh` wraps this with the git and gh plumbing;
`demo/ci-failures/README.md` documents the catalog and the expected triage verdict.

Every edit is an exact string replacement with an expected occurrence count, so applying
a fault to a file that has drifted fails loudly instead of silently corrupting it.

Usage:
    ci-faults.py list
    ci-faults.py show F1
    ci-faults.py apply F1
    ci-faults.py revert F1
    ci-faults.py status
    ci-faults.py verify F1        # apply, confirm the intended checks fail, revert
"""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

DASHBOARD = "backend/src/main/java/com/example/demo/controller/DashboardController.java"
PLAN_CONTROLLER = "backend/src/main/java/com/example/demo/controller/PlanController.java"
PLANS_TABLE = "components/plans-table.tsx"
CUSTOMERS_TABLE = "components/customers-table.tsx"
CI_WORKFLOW = ".github/workflows/ci.yml"
FLAKE_TEST = "__tests__/components/plans-table-window.test.tsx"


@dataclass(frozen=True)
class Edit:
    path: str
    find: str
    replace: str
    count: int = 1


@dataclass(frozen=True)
class Fault:
    id: str
    title: str
    story: str
    commit_subject: str
    pr_body: str
    classification: str
    route: str
    expected_failures: list[str]
    verify: list[str]
    edits: list[Edit] = field(default_factory=list)
    creates: dict[str, str] = field(default_factory=dict)
    format_after: bool = True


FLAKE_TEST_BODY = '''import { render, screen } from "@testing-library/react"
import { describe, it, expect, vi } from "vitest"
import { PlansTable } from "@/components/plans-table"
import type { Plan } from "@/hooks/use-plans"

// Built once and shared by every test in this file so the fixture is cheap.
const rows: Plan[] = [
  {
    id: 1,
    name: "Basic",
    monthly_price: 20,
    data_limit_gb: 10,
    minutes_limit: 500,
    sms_limit: 500,
    is_active: true,
  },
  {
    id: 2,
    name: "Standard",
    monthly_price: 40,
    data_limit_gb: 25,
    minutes_limit: 1000,
    sms_limit: 1000,
    is_active: true,
  },
]

describe("PlansTable window", () => {
  it("shows a plan provisioned after the initial load", () => {
    rows.push({
      id: 3,
      name: "Business Pro",
      monthly_price: 120,
      data_limit_gb: 100,
      minutes_limit: null,
      sms_limit: null,
      is_active: true,
    })

    render(<PlansTable plans={rows} onEdit={vi.fn()} onDelete={vi.fn()} />)

    expect(screen.getByText("Business Pro")).toBeInTheDocument()
  })

  it("renders the seeded plan window", () => {
    render(<PlansTable plans={rows} onEdit={vi.fn()} onDelete={vi.fn()} />)

    // One header row plus the two seeded plans.
    expect(screen.getAllByRole("row")).toHaveLength(3)
  })
})
'''


FAULTS: dict[str, Fault] = {
    "F1": Fault(
        id="F1",
        title="Dashboard revenue counts every customer, not just active ones",
        story=(
            "Looks like a harmless simplification: two repository calls collapse into "
            "findAll(). It silently bills suspended and cancelled subscribers, and it "
            "fails three backend tests plus the e2e cross-check from one root cause."
        ),
        commit_subject="refactor(dashboard): reuse the customer list for both aggregates",
        pr_body=(
            "Small cleanup on the dashboard controller. `getStats` and `getRevenueByPlan` "
            "were each issuing their own status-filtered query; both now read the customer "
            "list once. No behavior change intended."
        ),
        classification="product-code",
        route="agent-fix-now",
        expected_failures=[
            "DashboardControllerTest.statsReturnsActiveCustomerCountAndMonthlyRevenue",
            "DashboardControllerTest.statsRevenueExcludesNonActiveCustomers",
            "DashboardControllerTest.revenueByPlanOnlyIncludesPlansWithActiveCustomers",
            "dashboard-revenue.spec.ts:monthly revenue counts only active subscribers",
        ],
        verify=["cd backend && ./gradlew test --tests '*DashboardControllerTest*'"],
        edits=[
            Edit(
                path=DASHBOARD,
                find="customerRepository.findByStatus(Customer.Status.ACTIVE).stream()",
                replace="customerRepository.findAll().stream()",
                count=2,
            )
        ],
    ),
    "F2": Fault(
        id="F2",
        title="Plan API renames monthly_price to price_monthly",
        story=(
            "A naming-consistency change on one side of an interface. Backend tests and "
            "the e2e cross-check both fail, and the frontend silently renders NaN. Which "
            "side is correct is a product decision, so triage must stop."
        ),
        commit_subject="refactor(api): align plan price field with the pricing service",
        pr_body=(
            "Renames `monthly_price` to `price_monthly` in the plan response so the field "
            "matches what the pricing service emits. Frontend follow-up tracked separately."
        ),
        classification="contract-drift",
        route="needs-human",
        expected_failures=[
            "PlanControllerTest.getAllPlansReturnsSnakeCasePayload",
            "PlanControllerTest.createPlanPersistsRequestFields",
            "PlanControllerTest.updatePlanLeavesActiveFlagAloneWhenOmitted",
            "dashboard-revenue.spec.ts:monthly revenue counts only active subscribers",
        ],
        verify=["cd backend && ./gradlew test --tests '*PlanControllerTest*'"],
        edits=[
            Edit(
                path=PLAN_CONTROLLER,
                find="public record PlanResponse(Long id, String name, BigDecimal monthly_price, Integer data_limit_gb,",
                replace="public record PlanResponse(Long id, String name, BigDecimal price_monthly, Integer data_limit_gb,",
            )
        ],
    ),
    "F3": Fault(
        id="F3",
        title="Plans table column renamed, test still asserts the old header",
        story=(
            "The source change is intentional and correct. The test encodes the previous "
            "copy. The right fix is the test, not the component: a classification humans "
            "get wrong under time pressure."
        ),
        commit_subject="feat(plans): spell out the monthly price column header",
        pr_body=(
            'Renames the plans table column from "Price/mo" to "Monthly Price" so it reads '
            "the same as the billing screen. Copy approved by design."
        ),
        classification="test-code",
        route="agent-fix-now",
        expected_failures=["plans-table.test.tsx:renders the expected column headers"],
        verify=["pnpm test:frontend -- __tests__/components/plans-table.test.tsx"],
        edits=[
            Edit(
                path=PLANS_TABLE,
                find='<TableHead className="text-right">Price/mo</TableHead>',
                replace='<TableHead className="text-right">Monthly Price</TableHead>',
            )
        ],
    ),
    "F4": Fault(
        id="F4",
        title="Order-dependent test mutates a shared fixture",
        story=(
            "Fails in the suite, passes in isolation. There is no product bug anywhere "
            "near it. The only correct agent behavior is to name it a flake and change "
            "no production source."
        ),
        commit_subject="test(plans): cover the plan window after provisioning",
        pr_body=(
            "Adds coverage for the plans table window, including a plan provisioned after "
            "the initial load."
        ),
        classification="flake",
        route="quarantine-and-retry",
        expected_failures=[
            "plans-table-window.test.tsx:renders the seeded plan window",
        ],
        verify=["pnpm test:frontend -- __tests__/components/plans-table-window.test.tsx"],
        creates={FLAKE_TEST: FLAKE_TEST_BODY},
    ),
    "F5": Fault(
        id="F5",
        title="E2E job pins a faker version that does not exist",
        story=(
            "The job dies during environment setup. No test ever runs, so there is no "
            "JUnit XML at all: the evidence lives only in the log. Nothing in the diff "
            "is a code defect."
        ),
        commit_subject="ci: pin faker for reproducible seeding",
        pr_body=(
            "Pins the faker version used by the seeding step so e2e runs are reproducible "
            "across runners."
        ),
        classification="infra",
        route="platform-team",
        expected_failures=["E2E Tests job: Setup database step"],
        # Only reproduces on a runner: the failure is dependency resolution during
        # environment setup, so there is nothing meaningful to run locally.
        verify=[],
        edits=[
            Edit(
                path=CI_WORKFLOW,
                find="          pip install faker\n",
                replace="          pip install faker==99.99.99\n",
            )
        ],
        format_after=False,
    ),
    "F6": Fault(
        id="F6",
        title="Unused import and a formatting violation",
        story=(
            "The cheapest possible cluster. Worth showing so the cost table has a floor "
            "to compare the expensive clusters against."
        ),
        commit_subject="chore(customers): tidy the status badge map",
        pr_body="Minor cleanup on the customers table.",
        classification="hygiene",
        route="agent-fix-now",
        expected_failures=[
            "Frontend job: Check formatting step",
        ],
        verify=["pnpm format:check"],
        edits=[
            Edit(
                path=CUSTOMERS_TABLE,
                find='import { Pencil, Trash2 } from "lucide-react"',
                replace='import { Pencil, Trash2, Search } from "lucide-react"',
            ),
            Edit(
                path=CUSTOMERS_TABLE,
                find='  ACTIVE: "bg-green-100 text-green-800",',
                replace='  ACTIVE:     "bg-green-100 text-green-800",',
            ),
        ],
        format_after=False,
    ),
}


# --------------------------------------------------------------------------- helpers


def fail(msg: str) -> None:
    print(f"ERROR: {msg}", file=sys.stderr)
    raise SystemExit(1)


def read(path: str) -> str:
    target = ROOT / path
    if not target.exists():
        fail(f"missing file: {path}")
    return target.read_text()


def write(path: str, text: str) -> None:
    (ROOT / path).write_text(text)


def substitute(path: str, find: str, replace: str, count: int) -> None:
    text = read(path)
    found = text.count(find)
    if found != count:
        fail(
            f"{path}: expected {count} occurrence(s) of the target text, found {found}.\n"
            f"  The file has drifted from the fault definition. Target was:\n  {find[:120]}"
        )
    write(path, text.replace(find, replace))


def is_applied(fault: Fault) -> bool:
    for edit in fault.edits:
        if read(edit.path).count(edit.replace) != edit.count:
            return False
    for path in fault.creates:
        if not (ROOT / path).exists():
            return False
    return bool(fault.edits or fault.creates)


def run_formatters(fault: Fault) -> None:
    """Keep the seeded commit formatter-clean so CI only fails the intended way."""
    if not fault.format_after:
        return
    frontend = [e.path for e in fault.edits if e.path.endswith((".ts", ".tsx"))]
    frontend += [p for p in fault.creates if p.endswith((".ts", ".tsx"))]
    if frontend:
        subprocess.run(
            ["pnpm", "exec", "prettier", "--write", *frontend],
            cwd=ROOT,
            check=False,
            stdout=subprocess.DEVNULL,
        )
    if any(e.path.endswith(".java") for e in fault.edits):
        subprocess.run(
            ["./gradlew", "spotlessApply", "-q"],
            cwd=ROOT / "backend",
            check=False,
            stdout=subprocess.DEVNULL,
        )


# -------------------------------------------------------------------------- commands


def cmd_list(_: argparse.Namespace) -> None:
    width = max(len(f.title) for f in FAULTS.values())
    print(f"{'ID':<4} {'TITLE':<{width}}  {'CLASS':<15} ROUTE")
    for fault in FAULTS.values():
        mark = "*" if is_applied(fault) else " "
        print(
            f"{fault.id}{mark:<3} {fault.title:<{width}}  "
            f"{fault.classification:<15} {fault.route}"
        )
    print("\n* = currently applied to the working tree")


def cmd_show(args: argparse.Namespace) -> None:
    fault = FAULTS[args.fault]
    print(json.dumps(
        {
            "id": fault.id,
            "title": fault.title,
            "story": fault.story,
            "commit_subject": fault.commit_subject,
            "pr_body": fault.pr_body,
            "classification": fault.classification,
            "route": fault.route,
            "expected_failures": fault.expected_failures,
            "verify": fault.verify,
            "files": [e.path for e in fault.edits] + list(fault.creates),
            "applied": is_applied(fault),
        },
        indent=2,
    ))


def cmd_apply(args: argparse.Namespace) -> None:
    fault = FAULTS[args.fault]
    if is_applied(fault):
        print(f"{fault.id} is already applied; nothing to do.")
        return
    for edit in fault.edits:
        substitute(edit.path, edit.find, edit.replace, edit.count)
    for path, body in fault.creates.items():
        target = ROOT / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(body)
    run_formatters(fault)
    print(f"Applied {fault.id}: {fault.title}")
    for path in [e.path for e in fault.edits] + list(fault.creates):
        print(f"  touched {path}")


def cmd_revert(args: argparse.Namespace) -> None:
    fault = FAULTS[args.fault]
    for edit in fault.edits:
        text = read(edit.path)
        if text.count(edit.replace) == edit.count:
            write(edit.path, text.replace(edit.replace, edit.find))
        elif text.count(edit.find) != edit.count:
            print(f"WARNING: {edit.path} matches neither state; leaving it alone.", file=sys.stderr)
    for path in fault.creates:
        target = ROOT / path
        if target.exists():
            target.unlink()
    run_formatters(fault)
    print(f"Reverted {fault.id}")


def cmd_status(_: argparse.Namespace) -> None:
    applied = [f.id for f in FAULTS.values() if is_applied(f)]
    if applied:
        print("Applied faults: " + ", ".join(applied))
    else:
        print("No faults applied. Working tree is clean of seeded failures.")


def cmd_verify(args: argparse.Namespace) -> None:
    """Apply the fault, confirm the intended check actually fails, then revert."""
    fault = FAULTS[args.fault]
    if is_applied(fault):
        fail(f"{fault.id} is already applied; revert it before verifying.")

    print(f"--- verifying {fault.id}: {fault.title}")
    cmd_apply(args)
    ok = True
    if not fault.verify:
        applied = is_applied(fault)
        cmd_revert(args)
        if not applied:
            fail(f"{fault.id} did not apply cleanly")
        print(f"--- {fault.id} applied and reverted cleanly (no local check: CI-only failure)")
        return
    try:
        for command in fault.verify:
            print(f"    $ {command}")
            result = subprocess.run(command, cwd=ROOT, shell=True, capture_output=True, text=True)
            if result.returncode == 0:
                print(f"    UNEXPECTED PASS: the fault did not break `{command}`")
                ok = False
            else:
                print(f"    fails as designed (exit {result.returncode})")
    finally:
        cmd_revert(args)

    if not ok:
        fail(f"{fault.id} did not produce the expected failure")
    print(f"--- {fault.id} OK")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="command", required=True)

    sub.add_parser("list", help="list the fault catalog").set_defaults(func=cmd_list)
    sub.add_parser("status", help="show which faults are applied").set_defaults(func=cmd_status)

    for name, func, helptext in [
        ("show", cmd_show, "print a fault's definition as JSON"),
        ("apply", cmd_apply, "apply a fault to the working tree"),
        ("revert", cmd_revert, "undo a fault"),
        ("verify", cmd_verify, "apply, confirm the intended check fails, revert"),
    ]:
        p = sub.add_parser(name, help=helptext)
        p.add_argument("fault", choices=sorted(FAULTS))
        p.set_defaults(func=func)

    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
