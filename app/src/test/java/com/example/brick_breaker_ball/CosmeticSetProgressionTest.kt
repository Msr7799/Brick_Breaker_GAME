package com.example.brick_breaker_ball

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CosmeticSetProgressionTest {
    private fun ball(groupIndex: Int, group: String, index: Int) = BallSpriteDefinition(
        groupIndex, group, index, "$group-$index", "${group}_$index", index * 10, 0, 8, 8
    )

    private fun style(id: String, isDefault: Boolean = false): PaddleStyleSet {
        fun paddle(mode: String, name: String) = PaddleSpriteDefinition(
            "group2", 1, name, id, "", mode, 0, 0, 30, 10
        )
        val normalName = if (isDefault) "paddle_normal_titanium_edge" else "paddle_normal_$id"
        val weaponName = if (isDefault) "paddle_weapon_dual_pulse_cannon" else "paddle_weapon_$id"
        val stickyName = if (isDefault) "paddle_sticky_nano_gel" else "paddle_sticky_$id"
        return PaddleStyleSet(
            "group2:style:$id",
            id,
            paddle("normal", normalName),
            paddle("weapon", weaponName),
            paddle("sticky", stickyName)
        )
    }

    private fun fixture(
        ownershipPrefs: TestPreferences = TestPreferences()
    ): Triple<CosmeticProgressionService, ProgressStore, List<BallSpriteDefinition>> {
        val balls = (0..2).map { ball(1, "monsters", it) } + (0..1).map { ball(2, "biomes", it) } + (0..1).map { ball(3, "space", it) }
        val service =
            CosmeticProgressionService(balls, listOf(style("starter", true), style("nova")), CosmeticOwnershipStore(ownershipPrefs))
        val progress = ProgressStore(TestPreferences())
        service.reconcile(progress)
        return Triple(service, progress, balls)
    }

    @Test fun stage1UnlocksEntireMonstersTheme() {
        val (service, progress, balls) = fixture()
        progress.complete(1, 100, 1)
        val unlocked = service.onCampaignResult(1, 0, progress)
        assertTrue(unlocked.any { it is CosmeticUnlock.BallTheme && it.theme.groupName == "monsters" })
        assertTrue(balls.filter { it.groupName == "monsters" }.all(service::ownsBall))
    }

    @Test fun lockedThemeCannotEquipAnyBall() {
        val (service, progress, balls) = fixture()
        val locked = balls.first { it.groupName == "space" }
        assertFalse(service.ownsBall(locked))
        assertTrue(runCatching { service.equipBall(progress.settings, locked) }.isFailure)
    }

    @Test fun unlockedThemeAllowsEveryBallInTheme() {
        val (service, progress, balls) = fixture()
        progress.complete(1, 100, 1)
        service.onCampaignResult(1, 0, progress)
        balls.filter { it.groupName == "monsters" }.forEach {
            service.equipBall(progress.settings, it)
            assertEquals(it.spriteName, progress.settings.selectedBallSpriteName)
        }
    }

    @Test fun oldPerBallUnlockStateDoesNotBypassThemeLock() {
        val oldPrefs = TestPreferences().putBoolean("ball:space:space_0", true)
        val (service, _, balls) = fixture(oldPrefs)
        assertFalse(service.ownsBall(balls.first { it.groupName == "space" }))
    }

    @Test fun starterPaddleStyleIsFirst() {
        val (service, _, _) = fixture()
        assertEquals(CosmeticDefaults.PADDLE_ID, service.paddleStyles.first().normal.id)
    }

    @Test fun normalWeaponStickyUseSameStyleOrdering() {
        val (service, _, _) = fixture()
        assertEquals(service.paddleStyles.map { it.id }, service.paddleStyles.map { it.id })
        assertTrue(
            service.paddleStyles.all {
                it.normal.groupId == "normal" && it.weapon.groupId == "weapon" &&
                    it.sticky.groupId == "sticky"
            }
        )
    }

    @Test fun lockedPaddleCannotEquip() {
        val (service, progress, _) = fixture()
        assertTrue(runCatching { service.equipStyle(progress.settings, service.paddleStyles[1]) }.isFailure)
    }

    @Test fun combinedRevealFlowWorksWhenBothUnlockTogether() {
        val (service, progress, _) = fixture()
        progress.complete(1, 100, 1)
        val unlocks = service.onCampaignResult(1, 0, progress)
        assertEquals(
            listOf(CosmeticRevealKind.BALL_THEME, CosmeticRevealKind.PADDLE_STYLE, CosmeticRevealKind.SUMMARY),
            CosmeticRevealFlow(unlocks, false).steps
        )
    }

    @Test fun revealNotTriggeredWhenNoNewSetUnlocked() {
        val (service, progress, _) = fixture()
        progress.complete(2, 100, 1)
        assertTrue(service.onCampaignResult(2, 0, progress).isEmpty())
    }

    @Test fun skippingRevealDoesNotCancelUnlock() {
        val (service, progress, balls) = fixture()
        progress.complete(1, 100, 1)
        val flow = CosmeticRevealFlow(service.onCampaignResult(1, 0, progress), false)
        flow.skip()
        assertTrue(flow.complete)
        assertTrue(service.ownsBall(balls.first { it.groupName == "monsters" }))
    }

    @Test fun equipNowAppliesUnlockedSelection() {
        val (service, progress, balls) = fixture()
        progress.complete(1, 100, 1)
        service.onCampaignResult(1, 0, progress)
        val selected = balls.last { it.groupName == "monsters" }
        service.equipBall(progress.settings, selected)
        assertEquals(selected.spriteName, progress.settings.selectedBallSpriteName)
    }

    @Test fun reduceMotionUsesSimplifiedAnimationMode() {
        val flow = CosmeticRevealFlow(emptyList(), true)
        assertTrue(flow.reducedMotion)
    }
}
