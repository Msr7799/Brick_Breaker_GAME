# Sprite-sheet coverage

The immutable source is `tools/sprite_extraction/source/bricks-breaker_sprites-no-bg.png` (1254×1254 RGBA). Extraction is driven by explicit reviewed coordinates in `sprite_manifest.json`; no fixed grid is used.

| category | regions | status | runtime usage |
|---|---:|---|---|
| Full paddles | 3 | RESERVED_FOR_FUTURE | Skin previews/unlockable skins |
| Paddle three-slice parts | 9 | USED_IN_RUNTIME | Fixed caps, stretch-only center |
| Balls | 9 | USED_IN_RUNTIME | Normal, phase, micro/titan/future skins |
| Power-up icons | 20 | USED_IN_RUNTIME | Falling capsules and ball/paddle modes |
| Bricks | 28 | USED_IN_RUNTIME | World/material variation and editor |
| Effects | 16 | RESERVED_FOR_FUTURE | Score stars, trails and charge meter |

The authoritative per-item table—including id, name, category, exact x/y/width/height, padding, status, destination and runtime usage—is [sprite_inventory.json](../tools/sprite_extraction/sprite_inventory.json). Total: 85 regions. Every crop is non-empty and receives transparent padding before packing.

Generated visual QA sheets are stored in `tools/sprite_extraction/previews/`. The final runtime atlas is `app/src/main/assets/atlases/gameplay.atlas` plus its packed PNG page. The original sheet is never loaded by the game.
