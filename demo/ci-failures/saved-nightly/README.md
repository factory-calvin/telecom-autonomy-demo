# Banked nightly batch run

A real batch triage run, not a mock-up. Generated with the default seed, so
`python3 scripts/ci-nightly-fixture.py --out nightly-triage` reproduces the same corpus.

| File                | What it is                                                     |
| ------------------- | -------------------------------------------------------------- |
| `report.md`         | The triage report: funnel, per-cluster findings, routing table |
| `score.md`          | The scorecard against the answer key                           |
| `verdicts.json`     | Schema-valid machine output, including per-cluster confidence  |
| `labels.txt`        | Labels the workflow would apply                                |
| `ground-truth.json` | The answer key the run was graded against                      |

## Results

| Metric                   | Result  |
| ------------------------ | ------- |
| Classification accuracy  | 10 / 10 |
| Root causes recovered    | 10 / 10 |
| Clusters split or merged | 0       |
| **False auto-fixes**     | **0**   |
| Routing accuracy         | 6 / 10  |

Cost: `claude-opus-5`, 53,163 in / 47,687 out, 1,497,571 credits, 57.6 minutes, 31 turns.

## Read this before you present it

**Show this rather than running it live.** The run took 57 minutes on 2,000 tests and 106
failures. That is a sensible cost for work that replaces a morning of human triage, but it
is not a thing to wait for on a call. For a live run use `--tests 300`, which cuts the
corpus to roughly a fifth.

**All four routing misses are in the safe direction**, `needs-human` where the key says
`agent-fix-now`. Three of the four have one cause: the corpus names source files that do
not exist in this repository, so no verification command could be run, and the skill's
guardrail sends anything it cannot verify to a human. That is the guardrail working on a
mismatched bundle rather than a judgment error, and the run's own scorecard says so.

The honest framing is stronger than a perfect score would be: the number that matters for
whether you would leave this running unattended is the false-autofix count, and it is zero.
The agent also knew where it was unsure. The four misrouted clusters carry the four lowest
confidences in the run, and every correct route sits at 0.78 or above.

**One genuine judgment miss**, cluster c5: a renamed status label where the source had
already moved and the test had not. The rubric's own oracle routes that shape to
`agent-fix-now`. The run treated it as an open copy decision, which was too cautious.

## Reproducing it

```bash
python3 scripts/ci-nightly-fixture.py --out nightly-triage
./scripts/ci-triage.sh --local nightly-triage/bundle --mode batch
```

Cluster wording varies between runs. The classifications, the routes, and the false-autofix
count are what should hold steady.
