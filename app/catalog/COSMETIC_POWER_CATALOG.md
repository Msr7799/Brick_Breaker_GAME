# Brick Breaker Ball — Cosmetic Power Catalog

> Generated from the current game assets and the power catalogs implemented in code.

## الملخص

- **الكرات:** 105 كرة فعلية مرقّمة من 01 إلى 105.
- **مجموعات البادل:** 36 Style Set، وكل Set يحتوي **Normal + Weapon + Sticky**.
- شاشة الفوز تعرض أي Ball/Paddle تم فتحه مباشرةً مع قوته؛ لم يعد هناك انتقال عبر زر **VIEW NEW REWARDS**.
- الضغط على أي كرة أو Paddle Style في شاشة التخصيص يفتح شاشة Power Details كاملة، ومنها يتم التجهيز Equip.
- Ball powers مقسمة إلى 7 archetypes × 5 tiers. Paddle powers مقسمة إلى 4 archetypes × 3 tiers.

## Ball Power Rules

| Power | Gameplay effect |
|---|---|
| IMPACT CORE | Bonus score on every brick destroyed by that ball. |
| INFERNO BURST | Every N direct hits becomes a fire splash hit. |
| ARC CHAIN | Every N direct hits damages one nearby breakable brick. |
| ARMOR BREAKER | Every N direct hits deals +1 damage to the struck brick. |
| VOID PHASE | Every N direct hits instantly breaks one breakable brick. |
| FORTUNE CORE | Every N destroyed bricks grants a flat bonus score. |
| MOMENTUM CORE | Permanent small ball-speed multiplier within the normal game speed cap. |

## 105 Ball Catalog

| # | Ball | Sprite ID | Tier | Power | Main stat | Unlock |
|---:|---|---|---:|---|---|---|
| 38 | Chrome Mirror | `38_chrome_mirror` | 2 | ARC CHAIN | CHAIN / 11 HITS | STARTER |
| 39 | Black Gloss | `39_black_gloss` | 2 | ARMOR BREAKER | +1 ARMOR DMG / 9 HITS | Stage 1 |
| 43 | Amber Glass | `43_amber_glass` | 3 | IMPACT CORE | +8% SCORE | Stage 2 |
| 44 | Orange Glass | `44_orange_glass` | 3 | INFERNO BURST | FIRE / 9 HITS | Stage 3 |
| 42 | Smoky Stone | `42_smoky_stone` | 2 | MOMENTUM CORE | +3% SPEED | Stage 4 |
| 41 | Pearl White | `41_pearl_white` | 2 | FORTUNE CORE | +100 / 7 BREAKS | Stage 5 |
| 1 | Midnight Blue Half | `01_midnight_blue_half` | 1 | IMPACT CORE | +4% SCORE | Stage 6 |
| 2 | Silver Smoke | `02_silver_smoke` | 1 | INFERNO BURST | FIRE / 11 HITS | Stage 7 |
| 3 | Violet Smoke | `03_violet_smoke` | 1 | ARC CHAIN | CHAIN / 12 HITS | Stage 8 |
| 4 | Crimson Smoke | `04_crimson_smoke` | 1 | ARMOR BREAKER | +1 ARMOR DMG / 10 HITS | Stage 8 |
| 5 | Cyan Smoke | `05_cyan_smoke` | 1 | VOID PHASE | PHASE / 15 HITS | Stage 9 |
| 6 | Forest Camo | `06_forest_camo` | 1 | FORTUNE CORE | +75 / 8 BREAKS | Stage 10 |
| 7 | Obsidian Spike | `07_obsidian_spike` | 1 | MOMENTUM CORE | +2% SPEED | Stage 11 |
| 8 | Desert Camo | `08_desert_camo` | 1 | IMPACT CORE | +4% SCORE | Stage 12 |
| 9 | Urban Camo | `09_urban_camo` | 1 | INFERNO BURST | FIRE / 11 HITS | Stage 13 |
| 10 | Lava Crack | `10_lava_crack` | 1 | ARC CHAIN | CHAIN / 12 HITS | Stage 14 |
| 11 | Blue Stone Crack | `11_blue_stone_crack` | 1 | ARMOR BREAKER | +1 ARMOR DMG / 10 HITS | Stage 14 |
| 12 | Cyan Stone Crack | `12_cyan_stone_crack` | 1 | VOID PHASE | PHASE / 15 HITS | Stage 15 |
| 13 | Ice Crystal | `13_ice_crystal` | 1 | FORTUNE CORE | +75 / 8 BREAKS | Stage 16 |
| 14 | Frost Fang | `14_frost_fang` | 1 | MOMENTUM CORE | +2% SPEED | Stage 17 |
| 15 | Smooth Blue Panel | `15_smooth_blue_panel` | 1 | IMPACT CORE | +4% SCORE | Stage 18 |
| 16 | Aqua Bubble | `16_aqua_bubble` | 1 | INFERNO BURST | FIRE / 11 HITS | Stage 19 |
| 17 | Storm Lightning | `17_storm_lightning` | 1 | ARC CHAIN | CHAIN / 12 HITS | Stage 20 |
| 18 | Plasma Burst | `18_plasma_burst` | 1 | ARMOR BREAKER | +1 ARMOR DMG / 10 HITS | Stage 21 |
| 19 | Molten Core | `19_molten_core` | 1 | VOID PHASE | PHASE / 15 HITS | Stage 21 |
| 20 | Tech Core | `20_tech_core` | 1 | FORTUNE CORE | +75 / 8 BREAKS | Stage 22 |
| 21 | Blue Gem | `21_blue_gem` | 1 | MOMENTUM CORE | +2% SPEED | Stage 23 |
| 22 | Red Gem | `22_red_gem` | 2 | IMPACT CORE | +6% SCORE | Stage 24 |
| 23 | Violet Gem | `23_violet_gem` | 2 | INFERNO BURST | FIRE / 10 HITS | Stage 25 |
| 24 | Smoke Marble | `24_smoke_marble` | 2 | ARC CHAIN | CHAIN / 11 HITS | Stage 26 |
| 25 | Hydro Mech | `25_hydro_mech` | 2 | ARMOR BREAKER | +1 ARMOR DMG / 9 HITS | Stage 27 |
| 26 | Shadow Gloss | `26_shadow_gloss` | 2 | VOID PHASE | PHASE / 14 HITS | Stage 27 |
| 27 | Midnight Gloss | `27_midnight_gloss` | 2 | FORTUNE CORE | +100 / 7 BREAKS | Stage 28 |
| 28 | Sun Vortex | `28_sun_vortex` | 2 | MOMENTUM CORE | +3% SPEED | Stage 29 |
| 29 | Violet Vortex | `29_violet_vortex` | 2 | IMPACT CORE | +6% SCORE | Stage 30 |
| 30 | Blue Energy Vortex | `30_blue_energy_vortex` | 2 | INFERNO BURST | FIRE / 10 HITS | Stage 31 |
| 31 | Cosmic Field | `31_cosmic_field` | 2 | ARC CHAIN | CHAIN / 11 HITS | Stage 32 |
| 32 | Ice Spiral | `32_ice_spiral` | 2 | ARMOR BREAKER | +1 ARMOR DMG / 9 HITS | Stage 33 |
| 33 | Deep Ocean | `33_deep_ocean` | 2 | VOID PHASE | PHASE / 14 HITS | Stage 33 |
| 34 | Layered Planet | `34_layered_planet` | 2 | FORTUNE CORE | +100 / 7 BREAKS | Stage 34 |
| 35 | Teal Spiral | `35_teal_spiral` | 2 | MOMENTUM CORE | +3% SPEED | Stage 35 |
| 36 | Teal Tunnel | `36_teal_tunnel` | 2 | IMPACT CORE | +6% SCORE | Stage 36 |
| 37 | Neon Purple | `37_neon_purple` | 2 | INFERNO BURST | FIRE / 10 HITS | Stage 37 |
| 40 | Gold Segment | `40_gold_segment` | 2 | VOID PHASE | PHASE / 14 HITS | Stage 38 |
| 45 | Mist White | `45_mist_white` | 3 | ARC CHAIN | CHAIN / 10 HITS | Stage 39 |
| 46 | Gold Segmented | `46_gold_segmented` | 3 | ARMOR BREAKER | +1 ARMOR DMG / 8 HITS | Stage 40 |
| 47 | Fire Flame | `47_fire_flame` | 3 | VOID PHASE | PHASE / 13 HITS | Stage 40 |
| 48 | Stone Spiral | `48_stone_spiral` | 3 | FORTUNE CORE | +125 / 6 BREAKS | Stage 41 |
| 49 | Magma Stripe | `49_magma_stripe` | 3 | MOMENTUM CORE | +4% SPEED | Stage 42 |
| 50 | Red Vortex | `50_red_vortex` | 3 | IMPACT CORE | +8% SCORE | Stage 43 |
| 51 | Solar Honeycomb | `51_solar_honeycomb` | 3 | INFERNO BURST | FIRE / 9 HITS | Stage 44 |
| 52 | Purple Swirl | `52_purple_swirl` | 3 | ARC CHAIN | CHAIN / 10 HITS | Stage 45 |
| 53 | Silver Whirl | `53_silver_whirl` | 3 | ARMOR BREAKER | +1 ARMOR DMG / 8 HITS | Stage 46 |
| 54 | Steel Claw | `54_steel_claw` | 3 | VOID PHASE | PHASE / 13 HITS | Stage 46 |
| 55 | Blue Cracked Ice | `55_blue_cracked_ice` | 3 | FORTUNE CORE | +125 / 6 BREAKS | Stage 47 |
| 56 | Skull Core | `56_skull_core` | 3 | MOMENTUM CORE | +4% SPEED | Stage 48 |
| 57 | Dark Grid | `57_dark_grid` | 3 | IMPACT CORE | +8% SCORE | Stage 49 |
| 58 | Black Crack | `58_black_crack` | 3 | INFERNO BURST | FIRE / 9 HITS | Stage 50 |
| 59 | Red Target | `59_red_target` | 3 | ARC CHAIN | CHAIN / 10 HITS | Stage 51 |
| 60 | Gray Concentric | `60_gray_concentric` | 3 | ARMOR BREAKER | +1 ARMOR DMG / 8 HITS | Stage 52 |
| 61 | Inferno Cross | `61_inferno_cross` | 3 | VOID PHASE | PHASE / 13 HITS | Stage 52 |
| 62 | Golden Spiral | `62_golden_spiral` | 3 | FORTUNE CORE | +125 / 6 BREAKS | Stage 53 |
| 63 | Blue Starburst | `63_blue_starburst` | 3 | MOMENTUM CORE | +4% SPEED | Stage 54 |
| 64 | Red Core Eye | `64_red_core_eye` | 4 | IMPACT CORE | +10% SCORE | Stage 55 |
| 65 | Blue Hex Crack | `65_blue_hex_crack` | 4 | INFERNO BURST | FIRE / 8 HITS | Stage 56 |
| 66 | Purple Spark | `66_purple_spark` | 4 | ARC CHAIN | CHAIN / 9 HITS | Stage 57 |
| 67 | Void Portal | `67_void_portal` | 4 | ARMOR BREAKER | +1 ARMOR DMG / 7 HITS | Stage 58 |
| 68 | Silver Cross | `68_silver_cross` | 4 | VOID PHASE | PHASE / 12 HITS | Stage 59 |
| 69 | Steel Emblem | `69_steel_emblem` | 4 | FORTUNE CORE | +150 / 5 BREAKS | Stage 59 |
| 70 | Shadow Crack | `70_shadow_crack` | 4 | MOMENTUM CORE | +5% SPEED | Stage 60 |
| 71 | Magenta Lightning | `71_magenta_lightning` | 4 | IMPACT CORE | +10% SCORE | Stage 61 |
| 72 | Blue Smoke | `72_blue_smoke` | 4 | INFERNO BURST | FIRE / 8 HITS | Stage 62 |
| 73 | Bronze Band | `73_bronze_band` | 4 | ARC CHAIN | CHAIN / 9 HITS | Stage 63 |
| 74 | Teal Lightning | `74_teal_lightning` | 4 | ARMOR BREAKER | +1 ARMOR DMG / 7 HITS | Stage 64 |
| 75 | Green Lightning | `75_green_lightning` | 4 | VOID PHASE | PHASE / 12 HITS | Stage 65 |
| 76 | Blue Lightning | `76_blue_lightning` | 4 | FORTUNE CORE | +150 / 5 BREAKS | Stage 65 |
| 77 | Slate Panel | `77_slate_panel` | 4 | MOMENTUM CORE | +5% SPEED | Stage 66 |
| 78 | Blue Diamond Grid | `78_blue_diamond_grid` | 4 | IMPACT CORE | +10% SCORE | Stage 67 |
| 79 | Ink Splash | `79_ink_splash` | 4 | INFERNO BURST | FIRE / 8 HITS | Stage 68 |
| 80 | Blue Gloss | `80_blue_gloss` | 4 | ARC CHAIN | CHAIN / 9 HITS | Stage 69 |
| 81 | Purple Cell | `81_purple_cell` | 4 | ARMOR BREAKER | +1 ARMOR DMG / 7 HITS | Stage 70 |
| 82 | Black Mesh | `82_black_mesh` | 4 | VOID PHASE | PHASE / 12 HITS | Stage 71 |
| 83 | Purple Mesh | `83_purple_mesh` | 4 | FORTUNE CORE | +150 / 5 BREAKS | Stage 71 |
| 84 | Violet Arc | `84_violet_arc` | 4 | MOMENTUM CORE | +5% SPEED | Stage 72 |
| 85 | Blue Ribbed | `85_blue_ribbed` | 5 | IMPACT CORE | +12% SCORE | Stage 73 |
| 86 | Purple Storm | `86_purple_storm` | 5 | INFERNO BURST | FIRE / 7 HITS | Stage 74 |
| 87 | Teal Vortex | `87_teal_vortex` | 5 | ARC CHAIN | CHAIN / 8 HITS | Stage 75 |
| 88 | Green Starbolt | `88_green_starbolt` | 5 | ARMOR BREAKER | +1 ARMOR DMG / 6 HITS | Stage 76 |
| 89 | Purple Wave | `89_purple_wave` | 5 | VOID PHASE | PHASE / 11 HITS | Stage 77 |
| 90 | Navy Panel | `90_navy_panel` | 5 | FORTUNE CORE | +175 / 4 BREAKS | Stage 78 |
| 91 | Silver Mesh | `91_silver_mesh` | 5 | MOMENTUM CORE | +6% SPEED | Stage 78 |
| 92 | Blue Energy Core | `92_blue_energy_core` | 5 | IMPACT CORE | +12% SCORE | Stage 79 |
| 93 | Ice Portal | `93_ice_portal` | 5 | INFERNO BURST | FIRE / 7 HITS | Stage 80 |
| 94 | Purple Bolt | `94_purple_bolt` | 5 | ARC CHAIN | CHAIN / 8 HITS | Stage 81 |
| 95 | Blue Bolt | `95_blue_bolt` | 5 | ARMOR BREAKER | +1 ARMOR DMG / 6 HITS | Stage 82 |
| 96 | Maroon Crest | `96_maroon_crest` | 5 | VOID PHASE | PHASE / 11 HITS | Stage 83 |
| 97 | Cyan Bolt | `97_cyan_bolt` | 5 | FORTUNE CORE | +175 / 4 BREAKS | Stage 84 |
| 98 | Woven Steel | `98_woven_steel` | 5 | MOMENTUM CORE | +6% SPEED | Stage 84 |
| 99 | Ice Star | `99_ice_star` | 5 | IMPACT CORE | +12% SCORE | Stage 85 |
| 100 | Pink Bolt | `100_pink_bolt` | 5 | INFERNO BURST | FIRE / 7 HITS | Stage 86 |
| 101 | Engraved Swirl | `101_engraved_swirl` | 5 | ARC CHAIN | CHAIN / 8 HITS | Stage 87 |
| 102 | Gold Mech | `102_gold_mech` | 5 | ARMOR BREAKER | +1 ARMOR DMG / 6 HITS | Stage 88 |
| 103 | Purple Shard | `103_purple_shard` | 5 | VOID PHASE | PHASE / 11 HITS | Stage 89 |
| 104 | Bronze Core | `104_bronze_core` | 5 | FORTUNE CORE | +175 / 4 BREAKS | Stage 90 |
| 105 | Gray Segmented | `105_gray_segmented` | 5 | MOMENTUM CORE | +6% SPEED | Stage 90 |

## Paddle Style Catalog

| Set | Normal | Weapon | Sticky | Tier | Power | Effect | Unlock |
|---:|---|---|---|---:|---|---|---|
| 1 | Titanium Edge | Dual Pulse Cannon | Nano Gel | 1 | PRECISION CORE | +5% paddle response for steady control. | STARTER |
| 2 | Neon Wing | Rotary Blaster | Magnetic Resin | 1 | HYPER GLIDE | +18% paddle response for faster movement. | Stage 1 |
| 3 | Quantum Slate | Ion Bolt | Crystal Membrane | 1 | IMPACT BOOST | Tap just before paddle contact to boost ball speed by 12%. | Stage 4 |
| 4 | Prism Guard | Arc Cannon | Smart Foam | 1 | INFERNO RHYTHM | Every 4th normal paddle return charges one fire hit. | Stage 7 |
| 5 | Obsidian Rail | Micro Rocket Pods | Vacuum Pad | 1 | PRECISION CORE | +5% paddle response for steady control. | Stage 9 |
| 6 | Carbon Specter | Plasma Gatling | Electrostatic Mesh | 1 | HYPER GLIDE | +18% paddle response for faster movement. | Stage 12 |
| 7 | Ivory Normal | Ivory Weapon | Ivory Sticky | 1 | IMPACT BOOST | Tap just before paddle contact to boost ball speed by 12%. | Stage 15 |
| 8 | Frost Normal | Frost Weapon | Frost Sticky | 1 | INFERNO RHYTHM | Every 4th normal paddle return charges one fire hit. | Stage 17 |
| 9 | Navy Normal | Navy Weapon | Navy Sticky | 1 | PRECISION CORE | +5% paddle response for steady control. | Stage 20 |
| 10 | Copper Normal | Copper Weapon | Copper Sticky | 1 | HYPER GLIDE | +18% paddle response for faster movement. | Stage 22 |
| 11 | Gold Normal | Gold Weapon | Gold Sticky | 1 | IMPACT BOOST | Tap just before paddle contact to boost ball speed by 12%. | Stage 25 |
| 12 | Violet Normal | Violet Weapon | Violet Sticky | 1 | INFERNO RHYTHM | Every 4th normal paddle return charges one fire hit. | Stage 28 |
| 13 | Shadow Normal | Shadow Weapon | Shadow Sticky | 2 | PRECISION CORE | +7% paddle response for steady control. | Stage 30 |
| 14 | Ember Normal | Ember Weapon | Ember Sticky | 2 | HYPER GLIDE | +22% paddle response for faster movement. | Stage 33 |
| 15 | Sapphire Normal | Sapphire Weapon | Sapphire Sticky | 2 | IMPACT BOOST | Tap just before paddle contact to boost ball speed by 15%. | Stage 36 |
| 16 | Circuit Normal | Circuit Weapon | Circuit Sticky | 2 | INFERNO RHYTHM | Every 4th normal paddle return charges one fire hit. | Stage 38 |
| 17 | Bronze Normal | Bronze Weapon | Bronze Sticky | 2 | PRECISION CORE | +7% paddle response for steady control. | Stage 41 |
| 18 | Crimson Normal | Crimson Weapon | Crimson Sticky | 2 | HYPER GLIDE | +22% paddle response for faster movement. | Stage 43 |
| 19 | cyan | cyan | cyan | 2 | IMPACT BOOST | Tap just before paddle contact to boost ball speed by 15%. | Stage 46 |
| 20 | amber | amber | amber | 2 | INFERNO RHYTHM | Every 4th normal paddle return charges one fire hit. | Stage 49 |
| 21 | green | green | green | 2 | PRECISION CORE | +7% paddle response for steady control. | Stage 51 |
| 22 | blue | blue | blue | 2 | HYPER GLIDE | +22% paddle response for faster movement. | Stage 54 |
| 23 | crimson | crimson | crimson | 2 | IMPACT BOOST | Tap just before paddle contact to boost ball speed by 15%. | Stage 56 |
| 24 | lunar | lunar | lunar | 2 | INFERNO RHYTHM | Every 4th normal paddle return charges one fire hit. | Stage 59 |
| 25 | Gold White Alloy | Gold White Blaster | Gold White Field | 3 | PRECISION CORE | +9% paddle response for steady control. | Stage 62 |
| 26 | Blue Alloy | Blue Blaster | Blue Field | 3 | HYPER GLIDE | +26% paddle response for faster movement. | Stage 64 |
| 27 | Aurora Alloy | Aurora Blaster | Aurora Field | 3 | IMPACT BOOST | Tap just before paddle contact to boost ball speed by 18%. | Stage 67 |
| 28 | Magenta Alloy | Magenta Blaster | Magenta Field | 3 | INFERNO RHYTHM | Every 4th normal paddle return charges one fire hit. | Stage 70 |
| 29 | Cyan Alloy | Cyan Blaster | Cyan Field | 3 | PRECISION CORE | +9% paddle response for steady control. | Stage 72 |
| 30 | Solar Alloy | Solar Blaster | Solar Field | 3 | HYPER GLIDE | +26% paddle response for faster movement. | Stage 75 |
| 31 | Blue Alloy | Blue Blaster | Blue Field | 3 | IMPACT BOOST | Tap just before paddle contact to boost ball speed by 18%. | Stage 77 |
| 32 | Orange Alloy | Orange Blaster | Orange Field | 3 | INFERNO RHYTHM | Every 4th normal paddle return charges one fire hit. | Stage 80 |
| 33 | Green Alloy | Green Blaster | Green Field | 3 | PRECISION CORE | +9% paddle response for steady control. | Stage 83 |
| 34 | Violet Alloy | Violet Blaster | Violet Field | 3 | HYPER GLIDE | +26% paddle response for faster movement. | Stage 85 |
| 35 | Red Alloy | Red Blaster | Red Field | 3 | IMPACT BOOST | Tap just before paddle contact to boost ball speed by 18%. | Stage 88 |
| 36 | Silver Alloy | Silver Blaster | Silver Field | 3 | INFERNO RHYTHM | Every 4th normal paddle return charges one fire hit. | Stage 90 |

## UI / Reward Flow

1. عند إكمال المرحلة: `LEVEL COMPLETE` ثم النجوم ثم السكور ثم عناصر الـCollection الجديدة تدخل بأنيميشن متتابع.
2. Ball reward يعرض: صورة الكرة، الاسم، Power، Tier، Main Stat، ووصف القوة.
3. Paddle reward يعرض الثلاث حالات Normal / Weapon / Sticky مع اسم الـPower وTier ووصفها.
4. لا يوجد زر `VIEW NEW REWARDS`; زر المتابعة يذهب مباشرة إلى المرحلة التالية إن كانت متاحة، أو يرجع للعالم الحالي إذا كان العالم التالي ما زال مقفولاً.
5. شاشة Customize أصبحت بوابة إلى Power Detail Screen لكل Ball ولكل Paddle Style.

## ملاحظات التوازن

- Ball abilities مصممة لتكون واضحة ومحدودة حتى لا تلغي قيمة الـPower-ups الموجودة في اللعبة.
- Momentum يظل خاضعًا لـ `MAX_SPEED`.
- Arc Chain يضرب هدفًا إضافيًا واحدًا فقط.
- Armor Breaker يضيف نقطة ضرر واحدة عند التفعيل.
- Void Phase لا يؤثر على الطوب غير القابل للكسر أو المقفول أو Spiked Hazard.
- جميع الأرقام موجودة في `BallAbilities.kt` و `PaddleAbilities.kt` ويمكن تعديلها من مكان واحد.