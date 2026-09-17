package com.example.brick_breaker_ball

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CosmeticSetProgressionTest {
    private fun ball(index: Int): BallSpriteDefinition {
        val sprite = if (index == 0) CosmeticDefaults.BALL_SPRITE else "ball_${index.toString().padStart(2, '0')}"
        return BallSpriteDefinition(
            groupIndex = 0,
            groupName = CosmeticDefaults.BALL_GROUP,
            index = index + 1,
            name = "Ball ${index + 1}",
            spriteName = sprite,
            x = 0,
            y = 0,
            width = 64,
            height = 64,
        )
    }

    private fun style(id: String, isDefault: Boolean = false): PaddleStyleSet {
        val source = if (isDefault) "group1" else "group2"
        fun paddle(mode: String, name: String) = PaddleSpriteDefinition(
            sourceAtlas = source,
            spriteNumber = 1,
            name = name,
            displayNameEn = id,
            displayNameAr = "",
            groupId = mode,
            x = 0,
            y = 0,
            width = 30,
            height = 10,
        )
        val normalName = if (isDefault) "paddle_normal_titanium_edge" else "paddle_normal_$id"
        val weaponName = if (isDefault) "paddle_weapon_dual_pulse_cannon" else "paddle_weapon_$id"
        val stickyName = if (isDefault) "paddle_sticky_nano_gel" else "paddle_sticky_$id"
        return PaddleStyleSet(
            id = "$source:style:$id",
            displayName = id,
            normal = paddle("normal", normalName),
            weapon = paddle("weapon", weaponName),
            sticky = paddle("sticky", stickyName),
        )
    }

    private fun fixture(): Triple<CosmeticProgressionService, ProgressStore, List<BallSpriteDefinition>> {
        val balls = (0 until 105).map(::ball)
        val service = CosmeticProgressionService(
            balls = balls,
            paddles = listOf(style("starter", true), style("nova")),
            ownership = CosmeticOwnershipStore(TestPreferences()),
        )
        val progress = ProgressStore(TestPreferences())
        service.reconcile(progress)
        return Triple(service, progress, balls)
    }

    @Test fun starterBallAndPaddleAreOwnedImmediately() {
        val (service, _, balls) = fixture()
        assertTrue(service.ownsBall(balls.first()))
        assertEquals(CosmeticDefaults.PADDLE_ID, service.paddleStyles.first().normal.id)
        assertTrue(service.ownsPaddle(service.paddleStyles.first()))
    }

    @Test fun oneHundredFiveBallsAreSpreadAcrossTheFullCampaign() {
        val (service, _, _) = fixture()
        val stages = service.balls.drop(1).map { requireNotNull(service.ballUnlockStage(it)) }
        assertEquals(104, stages.size)
        assertEquals(1, stages.first())
        assertEquals(LevelRepository.TOTAL_LEVELS, stages.last())
        assertTrue(stages.zipWithNext().all { (a, b) -> b >= a })
        assertTrue(stages.groupingBy { it }.eachCount().values.all { it <= 2 })
        assertEquals(LevelRepository.TOTAL_LEVELS, stages.toSet().size)
    }

    @Test fun allOneHundredFiveBallsCanBeAwardedAcrossTheCampaign() {
        val (service, progress, _) = fixture()
        val unlocked = mutableListOf<CosmeticUnlock.Ball>()
        (1..LevelRepository.TOTAL_LEVELS).forEach { stage ->
            progress.complete(stage, 100, 1)
            unlocked += service.onCampaignResult(stage, 0, progress).filterIsInstance<CosmeticUnlock.Ball>()
        }

        assertEquals(104, unlocked.size)
        assertEquals(105, service.ownedBallCount())
    }

    @Test fun stageOneCanUnlockOneBallAndOnePaddleStyleTogether() {
        val (service, progress, _) = fixture()
        progress.complete(1, 100, 1)
        val unlocks = service.onCampaignResult(1, 0, progress)
        assertTrue(unlocks.any { it is CosmeticUnlock.Ball })
        assertTrue(unlocks.any { it is CosmeticUnlock.Paddle })
        assertEquals(
            listOf(CosmeticRevealKind.BALL, CosmeticRevealKind.PADDLE_STYLE, CosmeticRevealKind.SUMMARY),
            CosmeticRevealFlow(unlocks, false).steps,
        )
    }

    @Test fun lockedBallCannotEquipBeforeItsMilestone() {
        val (service, progress, _) = fixture()
        val locked = service.balls.last()
        assertFalse(service.ownsBall(locked))
        assertTrue(runCatching { service.equipBall(progress.settings, locked) }.isFailure)
    }

    @Test fun earnedBallCanBeEquippedAtItsMilestone() {
        val (service, progress, _) = fixture()
        val target = service.balls[1]
        val stage = requireNotNull(service.ballUnlockStage(target))
        progress.complete(stage, 100, 1)
        service.onCampaignResult(stage, 0, progress)
        assertTrue(service.ownsBall(target))
        service.equipBall(progress.settings, target)
        assertEquals(target.spriteName, progress.settings.selectedBallSpriteName)
    }

    @Test fun lockedPaddleCannotEquip() {
        val (service, progress, _) = fixture()
        assertTrue(runCatching { service.equipStyle(progress.settings, service.paddleStyles[1]) }.isFailure)
    }

    @Test fun skippingRevealDoesNotCancelDurableUnlock() {
        val (service, progress, _) = fixture()
        progress.complete(1, 100, 1)
        val flow = CosmeticRevealFlow(service.onCampaignResult(1, 0, progress), false)
        flow.skip()
        assertTrue(flow.complete)
        assertTrue(service.ownsBall(service.balls[1]))
    }

    @Test fun reduceMotionUsesSimplifiedRevealMode() {
        val flow = CosmeticRevealFlow(emptyList(), true)
        assertTrue(flow.reducedMotion)
    }
}
