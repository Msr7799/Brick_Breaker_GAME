/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/test/java/com/example/brick_breaker_ball/CosmeticCustomizationTest.kt
 * المؤلف: mohamed alromaihi
 * اختبار حاوية الكور: `hiddenBallCardsCannotInterceptPreviewOrSave`
 * الدوال الموجودة: `hiddenPaddleCardsCannotInterceptSaveAndBack`، `ballCatalogContainsAllGroupsSpritesAndValidUniqueBounds`، `paddleCatalogDiscoversEveryNumberedJsonPngPair`، `futurePaddleGroupsNeedNoCodeChange`، `cosmeticSettingsRoundTripWithStablePreferenceKeys`، `startedGameFlagPersistsForContinueVersusStartMenuState`، `corruptValuesUseSafeDefaultsWithoutThrowing`، `sizesDriveTheSameVisualAndCollisionRadius`، `multiballInheritsCosmeticSizeElementAndCollisionMode`، `megaExpiresButShrinkLastsForTheLevelWithinBounds`، `pausedSessionRoundTripsBaseSizeAndCosmeticIdentity`، `weaponAndStickyPaddleSelectionsNeverActivateAbilities`، `groupTwoDefaultsAreSelectedForNormalWeaponAndStickyStates`، `dualPaddlesAreAdjacentAndCollisionMatchesBothVisualSlots`، `parsePaddles`
 */

package com.example.brick_breaker_ball

import com.badlogic.gdx.utils.JsonReader
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CosmeticCustomizationTest {
    private val root = generateSequence(File(requireNotNull(System.getProperty("user.dir")))) { it.parentFile }
        .first { File(it, "app/src/main/assets/sprites").isDirectory }
    private val ballJson = File(root, "app/src/main/assets/sprites/balls-sprites/balls-sprites-7groups-named.json")
    private val ballPng = File(root, "app/src/main/assets/sprites/balls-sprites/balls-sprites-7.png")
    private val paddleDirectory = File(root, "app/src/main/assets/sprites/paddles-sprites")

    /** ملاحظة صيانة: الدالة `hiddenPaddleCardsCannotInterceptSaveAndBack` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun hiddenPaddleCardsCannotInterceptSaveAndBack() {
        val saveAndBack = com.badlogic.gdx.math.Rectangle(250f, 45f, 400f, 80f)
        val hiddenObsidianRow = com.badlogic.gdx.math.Rectangle(40f, 30f, 390f, 164f)
        assertNull(CustomizationHitTesting.visiblePaddleCard(hiddenObsidianRow))

        val partiallyVisible = com.badlogic.gdx.math.Rectangle(40f, 460f, 390f, 164f)
        val clipped = requireNotNull(CustomizationHitTesting.visiblePaddleCard(partiallyVisible))
        assertEquals(500f, clipped.y)
        assertEquals(124f, clipped.height)
        assertTrue(!clipped.overlaps(saveAndBack))
    }

    /** يضمن أن لمس الكور محصور في حاوية الصفوف الثلاثة القابلة للتمرير. */
    @Test fun hiddenBallCardsCannotInterceptPreviewOrSave() {
        val hiddenBelowCollection = com.badlogic.gdx.math.Rectangle(55f, 300f, 250f, 190f)
        val partiallyVisibleBottom = com.badlogic.gdx.math.Rectangle(55f, 480f, 250f, 190f)
        val partiallyVisibleTop = com.badlogic.gdx.math.Rectangle(55f, 1100f, 250f, 190f)

        assertNull(CustomizationHitTesting.visibleBallCard(hiddenBelowCollection))
        assertEquals(135f, requireNotNull(CustomizationHitTesting.visibleBallCard(partiallyVisibleBottom)).height)
        assertEquals(85f, requireNotNull(CustomizationHitTesting.visibleBallCard(partiallyVisibleTop)).height)
    }

    /** ملاحظة صيانة: الدالة `ballCatalogContainsAllGroupsSpritesAndValidUniqueBounds` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun ballCatalogContainsAllGroupsSpritesAndValidUniqueBounds() {
        val image = ImageIO.read(ballPng)
        val groups = CosmeticSpriteCatalogParser.balls(ballJson.readText(), image.width, image.height)
        val balls = groups.flatMap { it.sprites }
        assertEquals(40, groups.size)
        assertEquals(271, balls.size)
        assertEquals(271, balls.map { it.id }.toSet().size)
        assertTrue(
            balls.all {
                it.x >= 0 && it.y >= 0 && it.width > 0 && it.height > 0 && it.x + it.width <= image.width &&
                    it.y + it.height <= image.height
            }
        )
    }

    /** ملاحظة صيانة: يتحقق الاختبار من اكتشاف مجموعات البادلز الست وتكوين 36 عائلة كاملة وفريدة. */
    @Test fun paddleCatalogDiscoversEveryNumberedJsonPngPair() {
        val assets = CosmeticSpriteCatalogParser.paddleSheets(paddleDirectory.list()?.toList().orEmpty())
        assertEquals((1..6).map { "group$it" }, assets.map { it.group })
        assertEquals("paddle-group1.png", assets.single { it.group == "group1" }.imageFile)
        assertEquals("paddle-group3.png", assets.single { it.group == "group3" }.imageFile)

        val allPaddles = assets.flatMap { parsePaddles(it) }
        assertEquals(108, allPaddles.size)
        assertEquals(108, allPaddles.map { it.id }.toSet().size)
        assertEquals(36, PaddleStyleCatalog.build(allPaddles).size)
        assertTrue(allPaddles.all { it.groupId in setOf("normal", "weapon", "sticky") })
        assertNotNull(allPaddles.singleOrNull { it.id == "group4:paddle_normal_cyan" })
    }

    @Test fun futurePaddleGroupsNeedNoCodeChange() {
        val assets = CosmeticSpriteCatalogParser.paddleSheets(
            listOf(
                "paddles-group6.png",
                "paddles-group5.json",
                "notes.txt",
                "paddles-group6.json",
                "paddles-group5.png",
                "paddles-group7.json"
            )
        )

        assertEquals(listOf("group5", "group6"), assets.map { it.group })
        assertEquals(listOf("paddles-group5.png", "paddles-group6.png"), assets.map { it.imageFile })
    }

    /** ملاحظة صيانة: الدالة `cosmeticSettingsRoundTripWithStablePreferenceKeys` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun cosmeticSettingsRoundTripWithStablePreferenceKeys() {
        val prefs = TestPreferences()
        val first = ProgressStore(prefs)
        first.settings.selectedBallGroupName = "planets"
        first.settings.selectedBallSpriteName = "planets_earth"
        first.settings.selectedBallBaseSize = BallSize.LARGE
        first.settings.selectedPaddleId = "group1:paddle_normal_solar_ceramic"
        first.settings.selectedWeaponPaddleId = "group1:paddle_weapon_photon_lance"
        first.settings.selectedStickyPaddleId = "group2:paddle_sticky_nano_gel"
        first.saveSettings()

        assertEquals("planets_earth", prefs.getString("selected_ball_sprite"))
        assertEquals("planets", prefs.getString("selected_ball_group"))
        assertEquals("DEFAULT", prefs.getString("selected_ball_size"))
        assertEquals("group1:paddle_normal_solar_ceramic", prefs.getString("selected_paddle_id"))
        assertEquals("group1:paddle_weapon_photon_lance", prefs.getString("selected_weapon_paddle_id"))
        assertEquals("group2:paddle_sticky_nano_gel", prefs.getString("selected_sticky_paddle_id"))
        val restored = ProgressStore(prefs).settings
        assertEquals("planets_earth", restored.selectedBallSpriteName)
        assertEquals("planets", restored.selectedBallGroupName)
        assertEquals(BallSize.DEFAULT, restored.selectedBallBaseSize)
        assertEquals("group1:paddle_normal_solar_ceramic", restored.selectedPaddleId)
        assertEquals("group1:paddle_weapon_photon_lance", restored.selectedWeaponPaddleId)
        assertEquals("group2:paddle_sticky_nano_gel", restored.selectedStickyPaddleId)
    }

    /** ملاحظة صيانة: الدالة `startedGameFlagPersistsForContinueVersusStartMenuState` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun startedGameFlagPersistsForContinueVersusStartMenuState() {
        val prefs = TestPreferences()
        val progress = ProgressStore(prefs)
        assertFalse(progress.hasStartedGame)
        progress.markGameStarted()
        assertTrue(ProgressStore(prefs).hasStartedGame)
        assertTrue(prefs.getBoolean("has_started_game"))
    }

    /** ملاحظة صيانة: الدالة `corruptValuesUseSafeDefaultsWithoutThrowing` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun corruptValuesUseSafeDefaultsWithoutThrowing() {
        val prefs = TestPreferences().putString("selected_ball_size", "BROKEN")
        assertEquals(BallSize.DEFAULT, ProgressStore(prefs).settings.selectedBallBaseSize)
        assertTrue(runCatching { CosmeticSpriteCatalogParser.balls("{broken", 1536, 1024) }.isFailure)
        val rootJson = JsonReader().parse(ballJson.readText())
        assertNotNull(rootJson.get("groups"))
    }

    /** ملاحظة صيانة: الدالة `sizesDriveTheSameVisualAndCollisionRadius` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun sizesDriveTheSameVisualAndCollisionRadius() {
        assertEquals(16f, BallSize.SMALL.radius)
        assertEquals(22f, BallSize.DEFAULT.radius)
        assertEquals(30f, BallSize.LARGE.radius)
        BallSize.entries.forEach { size ->
            val ball = Ball(1, size = size, baseSize = size)
            assertEquals(size.radius, ball.radius)
            assertEquals(ball.radius * 2f, ball.visualDiameter)
        }
    }

    /** ملاحظة صيانة: الدالة `multiballInheritsCosmeticSizeElementAndCollisionMode` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun multiballInheritsCosmeticSizeElementAndCollisionMode() {
        val session = GameSession(
            baseBallSize = BallSize.SMALL,
            selectedBallGroupName = "monsters",
            selectedBallSpriteName = "monsters_skull"
        )
        assertTrue(session.activatePowerUp(PowerUpType.MEGA_BALL))
        assertTrue(session.activatePowerUp(PowerUpType.FIRE_BALL))
        assertTrue(session.activatePowerUp(PowerUpType.PIERCING_BALL))
        assertTrue(session.activatePowerUp(PowerUpType.MULTI_BALL))
        assertTrue(session.balls.size > 1)
        session.balls.forEach { ball ->
            assertEquals(BallSize.SMALL, ball.baseSize)
            assertEquals(BallSize.DEFAULT, ball.size)
            assertEquals("monsters", ball.cosmeticGroupName)
            assertEquals("monsters_skull", ball.cosmeticSpriteName)
            assertEquals(BallElement.FIRE, ball.element)
            assertEquals(BallCollisionMode.PIERCING, ball.collisionMode)
        }
    }

    /** ملاحظة صيانة: الدالة `megaExpiresButShrinkLastsForTheLevelWithinBounds` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun megaExpiresButShrinkLastsForTheLevelWithinBounds() {
        val small = GameSession(baseBallSize = BallSize.SMALL)
        assertFalse(small.canActivatePowerUp(PowerUpType.SHRINK_BALL))
        assertTrue(small.activatePowerUp(PowerUpType.MEGA_BALL))
        assertEquals(BallSize.DEFAULT, small.ball.size)
        small.powerUps.timers[PowerUpType.MEGA_BALL] = .001f
        small.update(.01f)
        assertEquals(BallSize.SMALL, small.ball.size)

        val large = GameSession(baseBallSize = BallSize.LARGE)
        assertFalse(large.canActivatePowerUp(PowerUpType.MEGA_BALL))
        assertTrue(large.activatePowerUp(PowerUpType.SHRINK_BALL))
        assertEquals(BallSize.DEFAULT, large.ball.size)
        large.update(30f)
        assertEquals(BallSize.DEFAULT, large.ball.size)
        assertTrue(PowerUpType.SHRINK_BALL in large.powerUps)
        large.activatePowerUp(PowerUpType.KILL_PADDLE)
        assertEquals(BallSize.DEFAULT, large.ball.size)
        assertTrue(PowerUpType.SHRINK_BALL in large.powerUps)
    }

    @Test fun sizePowerUpsKeepSelectedCosmeticWhileElementEffectsUseGameplayArt() {
        val ball = Ball(
            id = 1,
            size = BallSize.LARGE,
            baseSize = BallSize.DEFAULT,
            cosmeticGroupName = "biomes",
            cosmeticSpriteName = "biomes_swamp"
        )
        assertFalse(requiresGameplayBallSprite(ball))

        ball.element = BallElement.FIRE
        assertTrue(requiresGameplayBallSprite(ball))
    }

    /** ملاحظة صيانة: الدالة `pausedSessionRoundTripsBaseSizeAndCosmeticIdentity` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun pausedSessionRoundTripsBaseSizeAndCosmeticIdentity() {
        val prefs = TestPreferences()
        val store = PausedSessionStore(prefs)
        val level = LevelRepository.level(1)
        val session = GameSession(
            level = level,
            baseBallSize = BallSize.LARGE,
            selectedBallGroupName = "monsters",
            selectedBallSpriteName = "monsters_skull"
        )
        session.activatePowerUp(PowerUpType.SHRINK_BALL)
        repeat(3) { session.activatePowerUp(PowerUpType.EXPAND_PADDLE) }
        store.save(level, session)
        val restored = requireNotNull(store.restore()).second
        assertEquals(BallSize.DEFAULT, restored.baseBallSize)
        assertEquals(BallSize.DEFAULT, restored.ball.baseSize)
        assertEquals(BallSize.SMALL, restored.ball.size)
        assertEquals("monsters", restored.ball.cosmeticGroupName)
        assertEquals("monsters_skull", restored.ball.cosmeticSpriteName)
        assertEquals(3, restored.expandPaddleStacks)
        assertEquals(GameSession.BASE_PADDLE_WIDTH + GameSession.PADDLE_EXPAND_STEP * 3f, restored.paddle.targetWidth)
    }

    /** ملاحظة صيانة: الدالة `weaponAndStickyPaddleSelectionsNeverActivateAbilities` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun weaponAndStickyPaddleSelectionsNeverActivateAbilities() {
        val session = GameSession()
        assertFalse(PowerUpType.LASER_PADDLE in session.powerUps)
        assertFalse(PowerUpType.LASER_AUTO_CHARGE in session.powerUps)
        assertFalse(PowerUpType.STICKY_PADDLE in session.powerUps)
        val paddles = parsePaddles("group1") + parsePaddles("group2") + parsePaddles("group3")
        assertTrue(paddles.any { it.groupId == "weapon" })
        assertTrue(paddles.any { it.groupId == "sticky" })
    }

    /** ملاحظة صيانة: يتحقق الاختبار من أن عائلة البداية تأتي من المجموعة الأولى بعد إعادة تنظيم الصور. */
    @Test fun groupOneDefaultsAreSelectedForNormalWeaponAndStickyStates() {
        val paddles = parsePaddles("group1").associateBy { it.id }
        assertEquals("normal", requireNotNull(paddles[CosmeticDefaults.PADDLE_ID]).groupId)
        assertEquals(
            "weapon",
            requireNotNull(paddles[CosmeticDefaults.WEAPON_PADDLE_ID]) {
                paddles.keys.filter { "pulse" in it }.joinToString()
            }.groupId
        )
        assertEquals(
            "sticky",
            requireNotNull(paddles[CosmeticDefaults.STICKY_PADDLE_ID]) {
                paddles.keys.filter { "nano" in it }.joinToString()
            }.groupId
        )
        val normal = "group1:paddle_normal_solar_ceramic"
        val weapon = "group1:paddle_weapon_photon_lance"
        val sticky = "group1:paddle_sticky_liquid_metal_bond"
        assertEquals(normal, CosmeticPaddleSelection.idForState(normal, weapon, sticky, laserActive = false, stickyActive = false))
        assertEquals(weapon, CosmeticPaddleSelection.idForState(normal, weapon, sticky, laserActive = true, stickyActive = false))
        assertEquals(sticky, CosmeticPaddleSelection.idForState(normal, weapon, sticky, laserActive = false, stickyActive = true))
    }

    @Test fun starterPaddleFamilyIsFirstInNormalWeaponAndStickyOrdering() {
        val paddles = parsePaddles("group1") + parsePaddles("group2") + parsePaddles("group3")
        val defaultBall = BallSpriteDefinition(0, CosmeticDefaults.BALL_GROUP, 0, "starter", CosmeticDefaults.BALL_SPRITE, 0, 0, 32, 32)
        val progression = CosmeticProgressionService(
            balls = listOf(defaultBall),
            paddles = PaddleStyleCatalog.build(paddles),
            ownership = CosmeticOwnershipStore(TestPreferences())
        )
        val first = progression.paddleStyles.first()
        assertEquals(CosmeticDefaults.PADDLE_ID, first.normal.id)
        assertEquals(CosmeticDefaults.WEAPON_PADDLE_ID, first.weapon.id)
        assertEquals(CosmeticDefaults.STICKY_PADDLE_ID, first.sticky.id)
    }

    /** ملاحظة صيانة: الدالة `dualPaddlesAreAdjacentAndCollisionMatchesBothVisualSlots` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    @Test fun dualPaddlesAreAdjacentAndCollisionMatchesBothVisualSlots() {
        val session = GameSession()
        session.activatePowerUp(PowerUpType.DUAL_PADDLE)
        val slots = PaddleVisualLayout.slots(session.paddle.x, session.paddle.width, dual = true)
        val bounds = session.paddleCollisionBounds()
        assertEquals(2, slots.size)
        assertEquals(2, bounds.size)
        assertTrue(slots[0].centerX < slots[1].centerX)
        assertEquals(session.paddle.y, bounds[0].y + bounds[0].height / 2f)
        assertEquals(session.paddle.y, bounds[1].y + bounds[1].height / 2f)
        assertTrue(bounds[0].x + bounds[0].width < bounds[1].x)
        assertEquals(slots[0].width, bounds[0].width)
        assertEquals(slots[1].width, bounds[1].width)
        session.paddle.x = GameSession.WIDTH
        session.clampPaddleForActiveLayout()
        assertTrue(session.paddleCollisionBounds().all { it.x >= 0f && it.x + it.width <= GameSession.WIDTH })
    }

    /** ملاحظة صيانة: الدالة `parsePaddles` توثّق حالة اختبار أو تهيئة آلية وتحمي السلوك المتوقع من التراجع؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun parsePaddles(group: String): List<PaddleSpriteDefinition> {
        val assets = CosmeticSpriteCatalogParser.paddleSheets(paddleDirectory.list()?.toList().orEmpty())
        return parsePaddles(requireNotNull(assets.singleOrNull { it.group == group }))
    }

    private fun parsePaddles(asset: PaddleSheetAsset): List<PaddleSpriteDefinition> {
        val image = ImageIO.read(File(paddleDirectory, asset.imageFile))
        return CosmeticSpriteCatalogParser.paddles(
            File(paddleDirectory, asset.jsonFile).readText(),
            asset.group,
            image.width,
            image.height
        )
    }
}
