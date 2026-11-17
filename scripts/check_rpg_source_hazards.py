#!/usr/bin/env python3
"""Read-only integrity check of authored RPG campaign effects (no Android SDK needed).

We do not add or move bricks. Every source-specific effect is keyed by its
original Godot sourceScene and applied to existing stage-authored coordinates.
"""
import collections
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/assets/volley-source/stages.json"
all_stages = json.loads(SOURCE.read_text(encoding="utf-8"))["stages"]
assert len(all_stages) == 160, "Campaign must contain all 160 stages"

counts = collections.Counter()
seen_positions = set()
for index, stage in enumerate(all_stages, 1):
    bricks = stage["bricks"]
    assert len(bricks) > 0, index
    for item_index, brick in enumerate(bricks, 1):
        scene = brick["sourceScene"]
        kind = brick["kind"]
        coords = (brick["x"], brick["y"])
        assert all(isinstance(v, (int, float)) for v in coords), (index, item_index)
        assert brick["shape"] in ("SQ", "NW", "NE", "SW", "SE"), (index, item_index)
        if "/Spike_Traps/" in scene:
            assert kind == "SPIKE_TRAP", (index, item_index, kind)
            counts["spike"] += 1
        if kind == "VORTEX":
            assert "/Vortex/" in scene, (index, item_index, scene)
            if "/Rock1_Vortex/" in scene:
                counts["rock"] += 1
            elif "/Ice1_Vortex/" in scene:
                counts["ice"] += 1
            elif "/Black1_vortex/" in scene:
                counts["black"] += 1
            else:
                raise AssertionError(("Unknown source vortex", index, scene))
        if "/Blocker/Alt_Blocker/" in scene:
            assert kind == "BLOCKER", (index, item_index, kind)
            if "wooden" in scene:
                counts["wood"] += 1
            elif "rust" in scene:
                counts["rust"] += 1
            else:
                raise AssertionError(("Unknown alt blocker", index, scene))
        seen_positions.add((index, item_index, coords))
assert counts == {"spike": 67, "rock": 60, "ice": 11, "black": 18, "wood": 178, "rust": 80}, counts
print("RPG_NATIVE_SOURCE_SCENE_MAP_OK:", len(all_stages), "stages;",
      dict(counts), "unchanged scene mappings")
