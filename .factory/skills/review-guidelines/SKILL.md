---
name: review-guidelines
description: Repository-specific checks injected into every automated Droid review of a FactoryFone pull request.
---

Additional checks for this codebase, on top of the standard bug-focused review:

- **Money and usage math.** Any change touching `monthlyPrice`, `balance`, `cost`, `quantity`, data/minutes/SMS limits, or dashboard revenue aggregation must have a test that pins the arithmetic. Flag rounding, unit (MB vs GB), and currency assumptions.
- **API contract drift.** New or changed `@RestController` routes must be reflected in `docs/docs/backend/api.md` and in the AGENTS.md API table. Response shapes consumed by `hooks/use-*.ts` must stay compatible or the hook must change in the same PR.
- **Entity changes.** Changes under `model/` must consider `scripts/seed-database.py` (the Python seeder is the source of truth) and `DataSeeder.java`; call out if only one was updated.
- **Frontend conventions.** `"use client"` where hooks are used, `@/` imports, shadcn/ui primitives from `components/ui/`, tables named `components/<entity>-table.tsx`. Every new user-visible behaviour needs a Vitest test in `__tests__/`.
- **Regulatory context.** If the PR references an RFC in `docs/docs/rfcs/`, verify each acceptance criterion in the RFC has a corresponding test or an explicit note that it is out of scope.
- **Agent-authored PRs.** When the PR body has a **Risk self-assessment** section, verify the claims against the diff and say explicitly whether you agree with the stated risk level.
