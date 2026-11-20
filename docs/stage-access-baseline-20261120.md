# Brick Breaker Ball — Stage Access and Completion Baseline

Date: 2026-11-20  
Branch: `test/ball-boss-stage-access-20261120`  
Base commit: `12aba0753c6aacf4bee4377486fec47b9e725d48`  
Stage content SHA-256: `0a1f3968b97935fe8753ed3f4d1ee948e03e38eb1d0d4db81fd4cb42c726dce7`

## Implemented in this checkpoint

- Independent inspection of all 160 shipped stage layouts and their linked passage/special-behavior metadata.
- Verification of numbered stage IDs, world counts, clear targets, horizontal collider bounds, nine boss cores, and guard counts.
- Approximate line-of-sight inspection of the initial 11 vertical firing lanes.
- Conservative timeline comparison: first possible visible boss frame vs. earliest danger-line breach **if no low object is destroyed**.
- Deterministic, bounded shot-search harness using `VolleySession` physics and snapshot restoration. Its output includes per-shot objective health, boss contact, guard counts, chosen angles, and terminal state.
- Regression tests for structural integrity and deterministic replays, with one JVM-based replay test requiring an available Android Gradle toolchain.

## Boss access warnings

| Stage | Boss first visible after descents | Front breach if objects remain | Guards initially visible | Vertical firing lane summary | Assessment |
| ---: | ---: | ---: | ---: | --- | --- |
| 18 | 15 | 12 | 0/2 | 11 targets / 0 solids / 0 empty | Long-distance boss; clear lower rows first |
| 36 | 14 | 12 | 0/2 | 5 targets / 6 solids / 0 empty | Long-distance boss and narrow entrances |
| 54 | 9 | 12 | 1/2 | 8 targets / 1 solid / 2 empty | Source geometry passes; replay needed |
| 72 | 13 | 12 | 1/3 | 11 targets / 0 solids / 0 empty | Long-distance boss; clear lower rows first |
| 90 | 20 | 12 | 0/3 | 9 targets / 0 solids / 2 empty | Highest priority: longest approach |
| 108 | 9 | 12 | 1/3 | 2 targets / 8 solids / 1 empty | Heavy blocker coverage |
| 126 | 11 | 12 | 2/4 | 10 targets / 0 solids / 1 empty | Source geometry passes; replay needed |
| 143 | 7 | 14 | 2/4 | 0 targets / 11 solids / 0 empty | No direct vertical lane; angled shots/teleport untested |
| 160 | 11 | 12 | 2/4 | 9 targets / 0 solids / 2 empty | Source geometry passes; replay needed |

**Interpretation:** Stage coordinates are in board units. Every stage has at least one initially visible clear target, but that does **not** guarantee the target is hittable by the ball. The front-breach estimate assumes no lower objective or obstacle is removed. Lane estimates use axis-aligned bounds: triangle openings, reflections, portals, sliding obstacles, and direct ball physics are not captured.

**No final win certificate has been produced.** We have not proven that a free player can hit all nine bosses or clear all 160 stages. These are open release gates, not production-ready claims.

## How to reproduce checks

From the existing Android project root:

```bash
python3 scripts/stage_access_audit.py --strict
python3 -m unittest discover -s scripts/tests -p 'test_stage_access_audit.py' -v
```

When the existing Gradle wrapper and dependencies are available, execute the project JVM tests, then drive `VolleyStageAccessProbe.search(...)` against the nine boss stages and a deterministic sample of normal stages. Record the chosen angles, seed, player level, remaining objectives, boss contact, and `WIN` / `LOSS` / `TURN_LIMIT` / `SHOT_TIMEOUT` status. Re-run with the actual installed Release build and ADB traces before declaring any fix complete.

## Remaining work from the production plan

1. **P0:** Release/ADB baseline, golden-save fixtures, purchase recovery and duplicate-grant checks, audio timing checks.
2. **P1:** Run physics search on all 160 stages, find free wins, reproduce boss obstructions and stage softlocks, and adjust only independently verified defects.
3. **P1:** Per-ball and per-talisman effect triggers, damage and save UID tests; preserve current production feature flags until verified.
4. **P1:** Test all nine distinct boss skills behind flags, with warnings, saved-game recovery and free victories.
5. **P2:** Portrait layout readability, accessibility, truthful inventory/ability states, concise tutorials.
6. **P1:** Stage Lab and Android result parity, backend reward idempotency, then a separately approved staged Play release.

## Safety and review boundaries

- No changes were made to source stages, boss coordinates, guard placement, paid items, saves, feature defaults, or release signing in this checkpoint.
- Do not use this geometric audit as a pass/fail measure for stage solvability. The actual ball and enemy simulation must supply a successful replay.
- Keep stage geometry and live monetization unchanged until baseline replay and recovery tests have passed.
- Never publish, merge, or roll out to players based solely on these static checks.
