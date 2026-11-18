#!/usr/bin/env python3
"""Conservative, reproducible geometry audit of all imported Volley stages.

This is NOT a replacement for the LibGDX VolleySession collision/turn simulation.
It flags suspect boards for gameplay testing; it never certifies a stage winnable.
Run: python scripts/audit_volley_reachability.py --strict
"""
import argparse
import collections
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "app/src/main/assets/volley-source/stages.json"
GATES = ROOT / "app/src/main/assets/volley-source/solvability_gates.json"
OUT = ROOT / "build/reports/volley_reachability.json"
BREAKABLE = {"GRASS", "STONE", "SAND", "BLACK", "ICE", "BURN", "ENEMY",
             "DYNAMITE", "SUICIDE", "BONUS_BALLS", "CHEST", "ICE_FREEZE",
             "OSB", "BOSS", "BONUS_DAMAGE"}
NON_SOLID = {"VORTEX", "TELEPORT", "TORCH"}
# Imported board coordinates have 64 source units per brick; this is a
# conservative cell-space approximation, not pixel-accurate physics.
PITCH = 64.0


def bounds(b):
    x, y = float(b["x"]), float(b["y"])
    w, h = float(b.get("width", PITCH)), float(b.get("height", PITCH))
    return (x - w / 2, y - h / 2, x + w / 2, y + h / 2)


def overlap(a, b):
    return a[0] < b[2] and b[0] < a[2] and a[1] < b[3] and b[1] < a[3]


def audit(stage, gate_ids):
    bricks = stage.get("bricks", [])
    if not bricks:
        return {"id": stage.get("id"), "status": "FAIL", "issues": ["empty stage"]}
    rects = [bounds(b) for b in bricks]
    minx = min(r[0] for r in rects)
    maxx = max(r[2] for r in rects)
    miny = min(r[1] for r in rects)
    maxy = max(r[3] for r in rects)
    # Sampled collision occupancy, retaining blockers until an explicitly
    # breakable tile is reached. A wall-to-wall barrier is a warning, not
    # automatically a failure if it contains a playable gate.
    nx = 110
    ny = max(20, min(300, math.ceil((maxy-miny) / (maxx-minx) * nx)))
    dx, dy = (maxx-minx)/nx, (maxy-miny)/ny
    occupancy = {}
    for i, (b, r) in enumerate(zip(bricks, rects)):
        if b.get("kind") in NON_SOLID:
            continue
        lo_x = max(0, int((r[0]-minx)/dx))
        hi_x = min(nx-1, int((r[2]-minx)/dx))
        lo_y = max(0, int((r[1]-miny)/dy))
        hi_y = min(ny-1, int((r[3]-miny)/dy))
        for y in range(lo_y, hi_y+1):
            for x in range(lo_x, hi_x+1):
                # Avoid falsely treating triangle empty corners as solid.
                fx = ((minx+(x+.5)*dx)-r[0]) / max(1e-6, r[2]-r[0])
                fy = ((miny+(y+.5)*dy)-r[1]) / max(1e-6, r[3]-r[1])
                shape = b.get("shape", "SQ")
                inside = {"NW": fx+fy <= 1, "SE": fx+fy >= 1,
                          "NE": fx >= fy, "SW": fx <= fy}.get(shape, True)
                if inside:
                    occupancy.setdefault((x,y), set()).add(i)
    # Starting from below the formation; explore free space in four
    # directions. Once a breakable surface is exposed, remove it and repeat.
    # This is a permissive topological test: ricochet angles may still fail.
    removed = set()
    exposed = set()
    passes = 0
    while passes <= len(bricks):
        passes += 1
        start = [(x,ny-1) for x in range(nx)]
        seen = set(start)
        queue = collections.deque(start)
        touched = set()
        while queue:
            x,y = queue.popleft()
            for p in ((x-1,y),(x+1,y),(x,y-1),(x,y+1)):
                xx,yy = p
                if not (0 <= xx < nx and 0 <= yy < ny) or p in seen:
                    continue
                hit = occupancy.get(p, set()) - removed
                if hit:
                    touched.update(hit)
                else:
                    seen.add(p)
                    queue.append(p)
        new = {i for i in touched if i not in removed and
               (bricks[i].get("kind") in BREAKABLE or
                (bricks[i].get("kind") == "BLOCKER" and
                 (i in gate_ids or "sourceHealth" in bricks[i])))}
        exposed.update(touched)
        if not new:
            break
        removed.update(new)
    required = {i for i,b in enumerate(bricks) if b.get("kind") in BREAKABLE or
                (b.get("kind") == "BLOCKER" and (i in gate_ids or "sourceHealth" in b))}
    unreachable = sorted(required-removed)
    boss = [i for i,b in enumerate(bricks) if b.get("kind") == "BOSS"]
    rows = collections.defaultdict(set)
    for (x,y), ids in occupancy.items():
        rows[y].add(x)
    full_rows = [y for y,xs in rows.items() if len(xs) == nx]
    issues = []
    if unreachable:
        issues.append(f"{len(unreachable)} required tiles topologically unreachable")
    if full_rows:
        issues.append(f"{len(full_rows)} sampled full-width occupied rows (inspect breakability)")
    if boss and any(i in unreachable for i in boss):
        issues.append("boss source core unreachable")
    return {"id":stage.get("id"), "world":stage.get("world"),
            "brick_count":len(bricks), "required_count":len(required),
            "topologically_reachable_count":len(required)-len(unreachable),
            "unreachable_indices":unreachable, "boss_indices":boss,
            "full_width_rows":len(full_rows), "issues":issues,
            "status":"REVIEW" if issues else "GEOMETRY_PASS_ONLY"}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--strict", action="store_true",
                        help="exit nonzero if any geometry warnings or missing stages")
    parser.add_argument("--output", type=Path, default=OUT)
    args = parser.parse_args()
    data = json.loads(DATA.read_text(encoding="utf-8"))
    gates = json.loads(GATES.read_text(encoding="utf-8"))
    stages = data["stages"]
    results = [audit(s, set(gates.get("stages", {}).get(str(s["id"]), [])))
               for s in stages]
    ids = [s.get("id") for s in stages]
    errors = []
    if len(stages) != 160 or sorted(ids) != list(range(1,161)):
        errors.append("Expected exactly 160 distinct numbered stages")
    summary = dict(total=len(results),
                   geometry_pass=sum(r["status"]=="GEOMETRY_PASS_ONLY" for r in results),
                   review=sum(r["status"]=="REVIEW" for r in results),
                   errors=errors,
                   warning="Geometry-only approximation; run LibGDX physics simulation before claiming playability.")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps({"summary":summary,"stages":results},
                                     indent=2,ensure_ascii=False),encoding="utf-8")
    print(json.dumps(summary,indent=2))
    print("Report:",args.output)
    if args.strict and (errors or summary["review"] or summary["geometry_pass"] != 160):
        raise SystemExit(1)


if __name__ == "__main__":
    main()
