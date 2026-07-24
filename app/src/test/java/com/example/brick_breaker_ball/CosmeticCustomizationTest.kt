package com.example.brick_breaker_ball

import com.badlogic.gdx.utils.JsonReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class CosmeticCustomizationTest {
    private val root = generateSequence(File(requireNotNull(System.getProperty("user.dir")))) { it.parentFile }
        .first { File(it, "app/src/main/assets/sprites").isDirectory }
    private val ballJson = File(root, "app/src/main/assets/sprites/balls-sprites/balls-sprites-7groups-named.json")
    private val ballPng = File(root, "app/src/main/assets/sprites/balls-sprites/balls-sprites-7.png")
    private val paddleDirectory = File(root, "app/src/main/assets/sprites/paddles-sprites")

    @Test fun ballCatalogContainsAllGroupsSpritesAndValidUniqueBounds() {
        val image = ImageIO.read(ballPng)
        val groups = CosmeticSpriteCatalogParser.balls(ballJson.readText(), image.width, image.height)
        val balls = groups.flatMap { it.sprites }
        assertEquals(40, groups.size)
        assertEquals(271, balls.size)
        assertEquals(271, balls.map { it.id }.toSet().size)
        assertTrue(balls.all { it.x >= 0 && it.y >= 0 && it.width > 0 && it.height > 0 && it.x + it.width <= image.width && it.y + it.height <= image.height })
    }

    @Test fun paddleCatalogContainsAllThreeGroupsAndUniqueCompositeIds() {
        val group1 = parsePaddles("group1")
        val group2 = parsePaddles("group2")
        val group3 = parsePaddles("group3")
        assertEquals(18, group1.size)
        assertEquals(18, group2.size)
        assertEquals(18, group3.size)
        val allPaddles = group1 + group2 + group3
        assertEquals(54, allPaddles.size)
        assertEquals(54, allPaddles.map { it.id }.toSet().size)
        assertTrue(allPaddles.all { it.groupId in setOf("normal", "weapon", "sticky") })
    }

    @Test fun cosmeticSettingsRoundTripWithStablePreferenceKeys() {
        val prefs = TestPreferences()
        val first = ProgressStore(prefs)
        first.settings.selectedBallGroupName = "planets"
        first.settings.selectedBallSpriteName = "planets_earth"
        first.settings.selectedBallBaseSize = BallSize.LARGE
        first.settings.selectedPaddleId = "group2:paddle_sticky_nano_gel"
        first.saveSettings()

        assertEquals("planets_earth", prefs.getString("selected_ball_sprite"))
        assertEquals("planets", prefs.getString("selected_ball_group"))
        assertEquals("LARGE", prefs.getString("selected_ball_size"))
        assertEquals("group2:paddle_sticky_nano_gel", prefs.getString("selected_paddle_id"))
        val restored = ProgressStore(prefs).settings
        assertEquals("planets_earth", restored.selectedBallSpriteName)
        assertEquals("planets", restored.selectedBallGroupName)
        assertEquals(BallSize.LARGE, restored.selectedBallBaseSize)
        assertEquals("group2:paddle_sticky_nano_gel", restored.selectedPaddleId)
    }

    @Test fun startedGameFlagPersistsForContinueVersusStartMenuState() {
        val prefs = TestPreferences()
        val progress = ProgressStore(prefs)
        assertFalse(progress.hasStartedGame)
        progress.markGameStarted()
        assertTrue(ProgressStore(prefs).hasStartedGame)
        assertTrue(prefs.getBoolean("has_started_game"))
    }

    @Test fun corruptValuesUseSafeDefaultsWithoutThrowing() {
        val prefs = TestPreferences().putString("selected_ball_size", "BROKEN")
        assertEquals(BallSize.DEFAULT, ProgressStore(prefs).settings.selectedBallBaseSize)
        assertTrue(runCatching { CosmeticSpriteCatalogParser.balls("{broken", 1536, 1024) }.isFailure)
        val rootJson = JsonReader().parse(ballJson.readText())
        assertNotNull(rootJson.get("groups"))
    }

    @Test fun sizesDriveTheSameVisualAndCollisionRadius() {
        assertEquals(11f, BallSize.SMALL.radius)
        assertEquals(16f, BallSize.DEFAULT.radius)
        assertEquals(22f, BallSize.LARGE.radius)
        BallSize.entries.forEach { size ->
            val ball = Ball(1, size = size, baseSize = size)
            assertEquals(size.radius, ball.radius)
            assertEquals(ball.radius * 2f, ball.visualDiameter)
        }
    }

    @Test fun multiballInheritsCosmeticSizeElementAndCollisionMode() {
        val session = GameSession(
            baseBallSize = BallSize.SMALL,
            selectedBallGroupName = "monsters",
            selectedBallSpriteName = "monsters_skull",
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

    @Test fun pausedSessionRoundTripsBaseSizeAndCosmeticIdentity() {
        val prefs = TestPreferences()
        val store = PausedSessionStore(prefs)
        val level = LevelRepository.level(1)
        val session = GameSession(level = level, baseBallSize = BallSize.LARGE,
            selectedBallGroupName = "monsters", selectedBallSpriteName = "monsters_skull")
        session.activatePowerUp(PowerUpType.SHRINK_BALL)
        repeat(3) { session.activatePowerUp(PowerUpType.EXPAND_PADDLE) }
        store.save(level, session)
        val restored = requireNotNull(store.restore()).second
        assertEquals(BallSize.LARGE, restored.baseBallSize)
        assertEquals(BallSize.LARGE, restored.ball.baseSize)
        assertEquals(BallSize.DEFAULT, restored.ball.size)
        assertEquals("monsters", restored.ball.cosmeticGroupName)
        assertEquals("monsters_skull", restored.ball.cosmeticSpriteName)
        assertEquals(3, restored.expandPaddleStacks)
        assertEquals(352f, restored.paddle.targetWidth)
    }

    @Test fun weaponAndStickyPaddleSelectionsNeverActivateAbilities() {
        val session = GameSession()
        assertFalse(PowerUpType.LASER_PADDLE in session.powerUps)
        assertFalse(PowerUpType.LASER_AUTO_CHARGE in session.powerUps)
        assertFalse(PowerUpType.STICKY_PADDLE in session.powerUps)
        val paddles = parsePaddles("group1") + parsePaddles("group2") + parsePaddles("group3")
        assertTrue(paddles.any { it.groupId == "weapon" })
        assertTrue(paddles.any { it.groupId == "sticky" })
    }

    @Test fun groupTwoDefaultsAreSelectedForNormalWeaponAndStickyStates() {
        val paddles = (parsePaddles("group1") + parsePaddles("group2")).associateBy { it.id }
        assertEquals("normal", requireNotNull(paddles[CosmeticDefaults.PADDLE_ID]).groupId)
        assertEquals("weapon", requireNotNull(paddles[CosmeticDefaults.WEAPON_PADDLE_ID]) { paddles.keys.filter { "pulse" in it }.joinToString() }.groupId)
        assertEquals("sticky", requireNotNull(paddles[CosmeticDefaults.STICKY_PADDLE_ID]) { paddles.keys.filter { "nano" in it }.joinToString() }.groupId)
        val custom = "group1:paddle_normal_solar_ceramic"
        assertEquals(custom, CosmeticPaddleSelection.idForState(custom, laserActive = false, stickyActive = false))
        assertEquals(CosmeticDefaults.WEAPON_PADDLE_ID, CosmeticPaddleSelection.idForState(custom, laserActive = true, stickyActive = false))
        assertEquals(CosmeticDefaults.STICKY_PADDLE_ID, CosmeticPaddleSelection.idForState(custom, laserActive = false, stickyActive = true))
    }

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

    private fun parsePaddles(group: String): List<PaddleSpriteDefinition> {
        val imageName = if (group == "group3") "paddle-group3.png" else "paddles-$group.png"
        val image = ImageIO.read(File(paddleDirectory, imageName))
        return CosmeticSpriteCatalogParser.paddles(File(paddleDirectory, "paddles-$group.json").readText(), group, image.width, image.height)
    }
}
