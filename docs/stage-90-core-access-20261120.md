# Stage 90 — Final Core Access Fix

Date: 2026-11-20

## Reproduced geometry

- The stage has 177 authored pieces, including 38 blockers (three are destructible passage gates), two moving torch obstacles, 132 ice pieces and five freeze pieces.
- The three selected core guards begin at board Y positions 1950, 2270 and 2670.
- The original final core begins around Y 3310, beyond multiple blocker rows. It cannot even enter the initial 1720-unit viewport until about 20 descents.
- Some blockers cannot be broken. For this encounter, simply requiring the player to break everything in front of the core is not a valid solution.

## Implemented behavior

When **all other clear objectives**, including the three guards, have been destroyed in stage 90:

1. Keep the existing core's ID, current/initial HP, geometry size, damage rules and save identity.
2. If the core is still outside the viewport or lacks a clear firing lane, search the currently active board for a collision-free location.
3. Prefer the central horizontal lane; then look downward in 80-unit steps, staying safely above the danger line.
4. Require at least one unobstructed ball-sized vertical approach and no overlap with any surviving obstacle.
5. Display a short core-exposed message and continue the normal encounter; the core still has to be damaged before the stage can end.

No other stage, level curve, ball power, purchases, guard count, inventory or save schema is changed. If no safe landing slot exists, the core is left at its current position rather than intersecting another collider.

## Verification

- Read-only projection of the stage's existing pieces found collision-free center-lane placements at 0, 8, 12, 16, 20, 22 and 24 descents; at 20 descents the projected candidate was X 330, Y 960.
- Regression tests added for guard prerequisites, unchanged core identity and HP, paused-session restoration, direct ball contact and an arena with 20 descents.
- The tests are **not yet executed** against the Android physics engine. This environment cannot download the existing Gradle 9.4.1 wrapper distribution.
- A Release-device replay is required before merging or claiming the stage is fully beatable.

## Rollback and persistence

The behavior is restricted to stage 90 and is computed from the live brick layout. A v2 paused-session snapshot already stores the core's rectangle and existing guard-layout version, so it requires no schema change. Revert this isolated code change if Release replay or save-restore checks fail; do not alter saved player ownership.
