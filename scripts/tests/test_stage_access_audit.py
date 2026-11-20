"""Regression tests for the shipped stage-layout and boss-access audit."""

from __future__ import annotations

import json
import shutil
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from stage_access_audit import (  # noqa: E402
    ASSET_ROOT,
    FINALE_STAGES,
    _first_visible_turn,
    audit_campaign,
    audit_stage,
)


class StageAccessAuditTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.digest, cls.audits = audit_campaign()
        cls.by_stage = {audit.stage: audit for audit in cls.audits}

    def test_all_stages_have_structurally_valid_objectives(self):
        self.assertEqual(160, len(self.audits))
        self.assertEqual(list(range(1, 161)), sorted(self.by_stage))
        for entry in self.audits:
            with self.subTest(stage=entry.stage):
                self.assertFalse(entry.structural_errors)
                self.assertGreater(entry.objective_count, 0)
                self.assertGreater(entry.initial_visible_objectives, 0)
                self.assertEqual(
                    11,
                    entry.direct_front_lanes + entry.blocked_front_lanes + entry.open_front_lanes,
                )

    def test_nine_world_finales_have_one_core_and_expected_guards(self):
        actual = tuple(a.stage for a in self.audits if a.boss_core_y is not None)
        self.assertEqual(FINALE_STAGES, actual)
        for index, stage_id in enumerate(FINALE_STAGES, start=1):
            stage = self.by_stage[stage_id]
            self.assertEqual(index, stage.world)
            self.assertEqual(2 if index <= 3 else 3 if index <= 6 else 4, stage.boss_guard_count)
            self.assertIsNotNone(stage.boss_first_visible_turn)
            self.assertLess(stage.initially_visible_guards, stage.boss_guard_count)

    def test_timing_flags_are_diagnostic_not_win_claims(self):
        self.assertEqual(
            {18, 36, 72, 90},
            {a.stage for a in self.audits if a.boss_after_unchanged_front_breach},
        )
        self.assertEqual(15, self.by_stage[18].boss_first_visible_turn)
        self.assertEqual(20, self.by_stage[90].boss_first_visible_turn)
        self.assertEqual(0, self.by_stage[143].direct_front_lanes)
        self.assertEqual(0, _first_visible_turn(1719.9))
        self.assertEqual(1, _first_visible_turn(1720.0))

    def test_companion_files_must_match_source_digest(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for name in ("stages.json", "solvability_gates.json", "special-brick-behaviors.json"):
                shutil.copyfile(ASSET_ROOT / name, root / name)
            gates = json.loads((root / "solvability_gates.json").read_text())
            gates["sourceSha256"] = "0" * 64
            (root / "solvability_gates.json").write_text(json.dumps(gates))
            with self.assertRaisesRegex(ValueError, "metadata does not match"):
                audit_campaign(root)

    def test_missing_critical_goal_is_reported(self):
        source = json.loads((ASSET_ROOT / "stages.json").read_text())
        entry = dict(source["stages"][0])
        entry["bricks"] = [
            {"x": 40, "y": 40, "width": 64, "height": 64, "kind": "VORTEX", "shape": "SQ"}
        ]
        outcome = audit_stage(entry, set())
        self.assertIn("no clear objectives", outcome.structural_errors)


if __name__ == "__main__":
    unittest.main()
