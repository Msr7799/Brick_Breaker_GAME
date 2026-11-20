#!/usr/bin/env python3
"""Inspect the shipped 160-stage board geometry without changing gameplay.

This audit mirrors the authored-brick projection and world-finale placement in
VolleySourceStageData.kt. Its results are *structural diagnostics*, not proof
that a player can win: moving bricks, collisions, damage, skills, and ricochets
must also be exercised with the actual game simulation.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
from dataclasses import asdict, dataclass
from pathlib import Path

ASSET_ROOT = Path(__file__).resolve().parents[1] / "app/src/main/assets/volley-source"
BOARD_LEFT = 0.0
BOARD_RIGHT = 900.0
BOARD_TOP = 1720.0
LAUNCH_Y = 434.0
DANGER_Y = 300.0
PITCH = 80.0
LANE_LEFT = 10.0
WAVE_BOTTOM = 1230.0
FINALE_STAGES = (18, 36, 54, 72, 90, 108, 126, 143, 160)
REPLACED_CORES = (40, 80, 120)
NON_BREAKABLE_KINDS = {"TELEPORT", "SPIKE_TRAP", "VORTEX", "TORCH", "ROTATING"}
DANGEROUS_SOLIDS = {"VORTEX", "TORCH", "ROTATING", "SLIDER", "BLOCKER"}
EXCLUDED_OBJECTIVES = {"BONUS_BALLS", "VORTEX", "TORCH", "ROTATING"}
GUARD_KINDS = {"GRASS", "STONE", "SAND", "BLACK", "ICE", "BURN", "ENEMY"}


@dataclass
class Piece:
    index: int
    x: float
    y: float
    width: float
    height: float
    shape: str
    kind: str
    objective: bool
    breakable: bool
    group: int
    source_health: float | None
    suppressed: bool = False
    guard: bool = False


@dataclass(frozen=True)
class StageAudit:
    stage: int
    world: int
    objective_count: int
    obstruction_count: int
    gate_count: int
    initial_visible_objectives: int
    direct_front_lanes: int
    blocked_front_lanes: int
    open_front_lanes: int
    first_unchanged_front_breach_turn: int
    boss_guard_count: int
    initially_visible_guards: int
    boss_core_y: float | None
    boss_first_visible_turn: int | None
    boss_after_unchanged_front_breach: bool
    structural_errors: tuple[str, ...]


def _is_breakable(kind: str, raw: dict, gate: bool) -> bool:
    if kind in NON_BREAKABLE_KINDS:
        return False
    if kind == "SLIDER":
        return "sourceHealth" in raw
    if kind == "BLOCKER":
        return "sourceHealth" in raw or gate
    return True


def _project(raw: dict, index: int, gate: bool) -> Piece:
    scale = PITCH / 64.0
    width = float(raw["width"]) * scale
    height = float(raw["height"]) * scale
    kind = raw["kind"]
    breakable = _is_breakable(kind, raw, gate)
    return Piece(
        index=index,
        x=LANE_LEFT + (float(raw["x"]) - 8.0) * scale - width / 2.0,
        y=BOARD_TOP - (float(raw["y"]) + float(raw.get("collisionOffsetY", 0.0)) + 40.0) * scale - height / 2.0,
        width=width,
        height=height,
        shape=raw["shape"],
        kind=kind,
        objective=breakable and kind not in EXCLUDED_OBJECTIVES,
        breakable=breakable,
        group=int(raw.get("connected_to_destructible_number", 0)),
        source_health=float(raw["sourceHealth"]) if "sourceHealth" in raw else None,
    )


def _place_finale(world: int, pieces: list[Piece]) -> None:
    original = next((p for p in pieces if p.kind == "BOSS" and not p.suppressed), None)
    top = max(p.y + p.height for p in pieces if p.kind != "BOSS" and not p.suppressed)
    core = Piece(
        index=original.index if original else len(pieces),
        x=LANE_LEFT + (11.0 * PITCH - 3.0 * PITCH) / 2.0,
        y=top + PITCH,
        width=3.0 * PITCH,
        height=3.0 * PITCH,
        shape="SQ",
        kind="BOSS",
        objective=True,
        breakable=True,
        group=0,
        source_health=original.source_health if original else None,
    )
    if original:
        pieces[original.index] = core
    else:
        pieces.append(core)

    count = 2 if world <= 3 else (3 if world <= 6 else 4)
    candidates = [
        p for p in pieces
        if p.breakable and p.objective and not p.suppressed and p.shape == "SQ"
        and p.group == 0 and p.kind in GUARD_KINDS and p.source_health is not None
    ]
    if len(candidates) < count:
        raise ValueError(f"World {world} has only {len(candidates)} guard candidates")
    bottom = min(p.y for p in candidates)
    ceiling = max(p.y for p in candidates)
    for guard in range(count):
        target_y = bottom + (ceiling - bottom) * (0.3 + 0.6 * (guard + 0.5) / count)
        column = (2, 8, 5, 1)[(guard + world) % 4]
        target_x = LANE_LEFT + column * PITCH
        chosen = min(
            candidates,
            key=lambda p: abs(p.y - target_y) * 2.0 + abs(p.x - target_x),
        )
        chosen.guard = True
        candidates.remove(chosen)


def _first_visible_turn(y: float) -> int:
    if y < BOARD_TOP:
        return 0
    return math.floor((y - BOARD_TOP) / PITCH) + 1


def _front_lane_counts(pieces: list[Piece]) -> tuple[int, int, int]:
    """Approximate vertical first impacts with AABBs, not actual triangle sweeps."""
    objectives = blocked = empty = 0
    for column in range(11):
        x = LANE_LEFT + (column + 0.5) * PITCH
        collided = [
            p for p in pieces if not p.suppressed and p.y < BOARD_TOP
            and p.y + p.height > LAUNCH_Y and p.x <= x <= p.x + p.width
        ]
        if not collided:
            empty += 1
            continue
        first = min(collided, key=lambda p: max(p.y, LAUNCH_Y))
        if first.breakable:
            objectives += 1
        else:
            blocked += 1
    return objectives, blocked, empty


def audit_stage(entry: dict, gate_indices: set[int]) -> StageAudit:
    stage_id = int(entry["id"])
    world = int(entry["world"])
    raw_bricks = entry["bricks"]
    pieces = [_project(raw, index, index in gate_indices) for index, raw in enumerate(raw_bricks)]
    errors: list[str] = []
    if not pieces:
        errors.append("empty board")
        return StageAudit(stage_id, world, 0, 0, 0, 0, 0, 0, 11, 0, 0, 0, None, None, False, tuple(errors))

    offset = WAVE_BOTTOM - min(p.y for p in pieces)
    for piece in pieces:
        piece.y += offset
    if stage_id in REPLACED_CORES:
        for piece in pieces:
            if piece.kind == "BOSS":
                piece.suppressed = True
    if stage_id in FINALE_STAGES:
        _place_finale(world, pieces)

    objectives = [p for p in pieces if p.objective and not p.suppressed]
    cores = [p for p in pieces if p.kind == "BOSS" and not p.suppressed]
    guards = [p for p in pieces if p.guard]
    if not objectives:
        errors.append("no clear objectives")
    if len(cores) != int(stage_id in FINALE_STAGES):
        errors.append(f"unexpected boss core count: {len(cores)}")
    if stage_id in FINALE_STAGES and len(guards) != (2 if world <= 3 else 3 if world <= 6 else 4):
        errors.append(f"unexpected guard count: {len(guards)}")
    for piece in pieces:
        if piece.suppressed:
            continue
        if piece.width <= 0 or piece.height <= 0:
            errors.append(f"nonpositive collider dimensions at index {piece.index}")
        if piece.x < BOARD_LEFT - 0.01 or piece.x + piece.width > BOARD_RIGHT + 0.01:
            errors.append(f"collider outside horizontal board at index {piece.index}")

    dangerous = [p for p in pieces if not p.suppressed and (
        p.objective or (not p.breakable and p.kind in DANGEROUS_SOLIDS)
    )]
    front_breach = max(0, math.ceil((min(p.y for p in dangerous) - DANGER_Y) / PITCH)) if dangerous else 0
    core_y = cores[0].y if cores else None
    core_turn = _first_visible_turn(core_y) if core_y is not None else None
    direct, blocked, empty = _front_lane_counts(pieces)
    return StageAudit(
        stage=stage_id,
        world=world,
        objective_count=len(objectives),
        obstruction_count=sum(not p.breakable and not p.suppressed for p in pieces),
        gate_count=len(gate_indices),
        initial_visible_objectives=sum(p.y < BOARD_TOP and p.y + p.height > LAUNCH_Y for p in objectives),
        direct_front_lanes=direct,
        blocked_front_lanes=blocked,
        open_front_lanes=empty,
        first_unchanged_front_breach_turn=front_breach,
        boss_guard_count=len(guards),
        initially_visible_guards=sum(p.y < BOARD_TOP and p.y + p.height > LAUNCH_Y for p in guards),
        boss_core_y=round(core_y, 2) if core_y is not None else None,
        boss_first_visible_turn=core_turn,
        boss_after_unchanged_front_breach=bool(core_turn is not None and core_turn > front_breach),
        structural_errors=tuple(errors),
    )


def audit_campaign(asset_root: Path = ASSET_ROOT) -> tuple[str, list[StageAudit]]:
    stage_bytes = (asset_root / "stages.json").read_bytes()
    digest = hashlib.sha256(stage_bytes).hexdigest()
    source = json.loads(stage_bytes)
    gates = json.loads((asset_root / "solvability_gates.json").read_text(encoding="utf-8"))
    specials = json.loads((asset_root / "special-brick-behaviors.json").read_text(encoding="utf-8"))
    for name, companion in (("passage gates", gates), ("special behaviors", specials)):
        if companion.get("sourceSha256") != digest:
            raise ValueError(f"{name} metadata does not match current stage content")
    stages = source["stages"]
    if len(stages) != 160 or [s["id"] for s in stages] != list(range(1, 161)):
        raise ValueError("expected 160 consecutively numbered stages")
    counts = [sum(s["world"] == world for s in stages) for world in range(1, 10)]
    if counts != source["worldCounts"]:
        raise ValueError(f"world counts disagree with stage data: {counts}")
    if set(gates["stages"]) != {str(s["id"]) for s in stages}:
        raise ValueError("passage metadata does not cover all stages")

    result = []
    for entry in stages:
        indices = set(gates["stages"][str(entry["id"])])
        if any(not 0 <= i < len(entry["bricks"]) for i in indices):
            raise ValueError(f"passage index outside stage {entry['id']}")
        for i in indices:
            raw = entry["bricks"][i]
            if raw["kind"] != "BLOCKER" or "sourceHealth" in raw or raw["shape"] not in {"SQ", "NW", "NE", "SW", "SE"}:
                raise ValueError(f"invalid passage at stage {entry['id']} index {i}")
        result.append(audit_stage(entry, indices))
    return digest, result


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--assets", type=Path, default=ASSET_ROOT, help="directory of installed stage JSON assets")
    parser.add_argument("--format", choices=("summary", "json"), default="summary")
    parser.add_argument("--strict", action="store_true", help="exit nonzero for structural errors")
    args = parser.parse_args()
    digest, audits = audit_campaign(args.assets)
    errors = [a for a in audits if a.structural_errors]
    late_bosses = [a for a in audits if a.boss_after_unchanged_front_breach]
    if args.format == "json":
        print(json.dumps({
            "stageSha256": digest,
            "stages": [asdict(a) for a in audits],
            "structuralErrorCount": len(errors),
            "lateBossCount": len(late_bosses),
            "verification": "STATIC_ONLY_NOT_A_WIN_CERTIFICATE",
        }, ensure_ascii=False, indent=2))
    else:
        print(f"Stage SHA-256: {digest}")
        print(f"Stages inspected: {len(audits)}; structural errors: {len(errors)}")
        print(f"Finales requiring frontline clearance before core appearance: {len(late_bosses)}/9")
        print("Stage  Objects  Guards  Visible guards  Core first seen  Front breach if unchanged  Lanes direct/blocked/open")
        for a in audits:
            if a.stage in FINALE_STAGES:
                print(f"{a.stage:5} {a.objective_count:8} {a.boss_guard_count:7} {a.initially_visible_guards:15} "
                      f"{a.boss_first_visible_turn:16} {a.first_unchanged_front_breach_turn:25} "
                      f"{a.direct_front_lanes}/{a.blocked_front_lanes}/{a.open_front_lanes}")
        for a in errors:
            print(f"ERROR stage {a.stage}: {', '.join(a.structural_errors)}")
        print("NOTE: Lane counts use bounding boxes; triangle gaps, bounces, and moving obstacles are not simulated.")
        print("NOTE: No actual shot, win, or saved-session playback was performed.")
    return int(args.strict and bool(errors))


if __name__ == "__main__":
    raise SystemExit(main())
