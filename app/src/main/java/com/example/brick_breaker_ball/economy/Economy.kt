/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/economy/Economy.kt
 * الوظيفة: رصيد العملات والجواهر وأسعار العناصر.
 * الحسابات: حساب الخصم والمنح وحدود الرصيد.
 */

package com.example.brick_breaker_ball.economy

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.example.brick_breaker_ball.campaign.campaignClearGems
import com.example.brick_breaker_ball.gameplay.PowerUpType
import java.util.Calendar
import java.util.TimeZone

/** Persistent soft-currency wallet used by campaign rewards and the in-game shop. */
data class WalletSnapshot(val coins: Int, val gems: Int)

data class CampaignEconomyReward(
    val coins: Int,
    val gems: Int,
    val firstClear: Boolean,
    val starUpgrade: Boolean
)

data class DailyWalletReward(val coins: Int, val gems: Int, val streak: Int)

/** Local fourteen-day calendar, independent from rewarded-video ad limits. */
data class DailyStreakStatus(
    val dayToClaim: Int,
    val claimedToday: Boolean,
    val completedDays: Int
)

class EconomyStore(
    private val prefs: Preferences = Gdx.app.getPreferences("brickbreakerball-economy-v1")
) {
    companion object {
        private const val STARTING_COINS = 600
        private const val STARTING_GEMS = 25
        private const val MAX_BALANCE = 9_999_999
        const val DAILY_REWARD_DAYS = 14
    }

    var coins: Int = prefs.getInteger("coins", STARTING_COINS).coerceIn(0, MAX_BALANCE)
        private set
    var gems: Int = prefs.getInteger("gems", STARTING_GEMS).coerceIn(0, MAX_BALANCE)
        private set

    fun snapshot() = WalletSnapshot(coins, gems)

    fun addCoins(amount: Int) {
        if (amount <= 0) return
        coins = (coins.toLong() + amount).coerceAtMost(MAX_BALANCE.toLong()).toInt()
        save()
    }

    /** One durable credit per sold Orb UID, including a retry after process death. */
    fun creditOrbSale(uid: String, amount: Int): Boolean {
        if (uid.isBlank() || amount <= 0 || amount > 100_000) return false
        val receipt = "orb-sale:$uid"
        if (prefs.getBoolean(receipt, false)) return true
        coins = (coins.toLong() + amount).coerceAtMost(MAX_BALANCE.toLong()).toInt()
        prefs.putInteger("coins", coins).putBoolean(receipt, true).flush()
        return true
    }

    /** A retry can finish opening the same Orb slot without charging its coins twice. */
    fun spendCoinsForOrbSlot(slot: Int, amount: Int): Boolean {
        if (slot !in 1..5 || amount <= 0) return false
        val receipt = "orb-slot-payment:${slot + 1}"
        if (prefs.getBoolean(receipt, false)) return true
        if (coins < amount) return false
        coins -= amount
        prefs.putInteger("coins", coins).putBoolean(receipt, true).flush()
        return true
    }

    /** Purchasing one family/rank is recoverable if the app stops between wallet and copy saves. */
    fun spendForOrbRank(family: String, rank: Int, amount: Int, useGems: Boolean): Boolean {
        if (!family.matches(Regex("[A-Z_]+")) || rank !in 0..5 || amount <= 0) return false
        val receipt = "orb-rank-payment:$family:$rank"
        if (prefs.getBoolean(receipt, false)) return true
        if (useGems) {
            if (gems < amount) return false
            gems -= amount
            prefs.putInteger("gems", gems)
        } else {
            if (coins < amount) return false
            coins -= amount
            prefs.putInteger("coins", coins)
        }
        prefs.putBoolean(receipt, true).flush()
        return true
    }

    fun addGems(amount: Int) {
        if (amount <= 0) return
        gems = (gems.toLong() + amount).coerceAtMost(MAX_BALANCE.toLong()).toInt()
        save()
    }

    fun spendCoins(amount: Int): Boolean {
        if (amount <= 0 || coins < amount) return false
        coins -= amount
        save()
        return true
    }

    /**
     * Atomic wallet + receipt marker in one Preferences edit. A restored consumable purchase
     * can never add the same Gems twice, even if the app stopped before ledger processing.
     * Returns true only on the first credit for a nonblank Google Play purchase token.
     */
    fun creditPurchasedGems(transactionId: String, amount: Int): Boolean {
        if (transactionId.isBlank() || amount <= 0) return false
        val marker = "gem-purchase:$transactionId"
        if (prefs.getBoolean(marker, false)) return false
        gems = (gems.toLong() + amount).coerceAtMost(MAX_BALANCE.toLong()).toInt()
        prefs.putInteger("gems", gems).putBoolean(marker, true).flush()
        return true
    }

    /** Apply a server-authorized daily receipt at most once per Firebase account/day. */
    @Synchronized fun creditServerDailyReward(reward: com.example.brick_breaker_ball.app.ServerDailyAward): Boolean {
        if (reward.id.isBlank() || reward.calendarDay < 0 || reward.streak !in 1..DAILY_REWARD_DAYS ||
            reward.coins !in 0..2_500 || reward.gems !in 0..40) return false
        val expected = dailyRewardForDay(reward.streak)
        if (expected.coins != reward.coins || expected.gems != reward.gems) return false
        val receipt = "server-daily:${reward.id}"
        if (prefs.getBoolean(receipt, false)) return false
        coins = (coins.toLong() + reward.coins).coerceAtMost(MAX_BALANCE.toLong()).toInt()
        gems = (gems.toLong() + reward.gems).coerceAtMost(MAX_BALANCE.toLong()).toInt()
        // Single Preferences flush keeps receipt, wallet and calendar in sync on this device.
        prefs.putBoolean(receipt, true).putInteger("coins", coins).putInteger("gems", gems)
            .putLong("daily_day", reward.calendarDay).putBoolean("daily_calendar_migrated", true)
            .putInteger("daily_streak", reward.streak).flush()
        return true
    }

    fun spendGems(amount: Int): Boolean {
        if (amount <= 0 || gems < amount) return false
        gems -= amount
        save()
        return true
    }

    /**
     * Volley stage-clear economy:
     * - Every victory earns coins (including replays).
     * - First clear of every global fifth level earns exactly 3 gems.
     * - First clear of a world boss earns exactly 30 gems, taking precedence over
     *   the fifth-level milestone: no 33-gem double payouts.
     * - Regular stages, star upgrades and replays never award premium gems.
     *   Other independent systems (e.g. daily calendar) keep their own rewards.
     */
    fun rewardCampaignClear(
        levelId: Int,
        world: Int,
        stars: Int,
        score: Int,
        previousStars: Int,
        isBossStage: Boolean = false,
        coinMultiplier: Float = 1f
    ): CampaignEconomyReward {
        val safeStars = stars.coerceIn(1, 3)
        val firstClear = previousStars <= 0
        val starUpgrade = safeStars > previousStars.coerceIn(0, 3)
        val scoreBonus = (score / 500).coerceIn(0, 80)
        val replayCoins = 55 + world.coerceAtLeast(1) * 15 + safeStars * 30 + scoreBonus
        val firstClearBonus = if (firstClear) 125 + (levelId / 10) * 20 else 0
        val earnedCoins = ((replayCoins + firstClearBonus) * coinMultiplier.coerceIn(1f, 2.5f)).toInt()
        val earnedGems = campaignClearGems(levelId, isBossStage, firstClear)
        addCoins(earnedCoins)
        addGems(earnedGems)
        return CampaignEconomyReward(earnedCoins, earnedGems, firstClear, starUpgrade)
    }

    /**
     * Read-only presentation of today's streak. Claimed streaks don't advance just by
     * opening the game. A qualifying game session calls [claimDailyIfAvailable].
     */
    @Synchronized fun dailyStreakStatus(now: Long = System.currentTimeMillis()): DailyStreakStatus {
        val today = dayIndex(now)
        val last = lastRewardDay()
        // The v1 value is already the number of the last claimed day. In particular, a
        // player who claimed day 7 yesterday must see day 8, never another day 7 gift.
        val oldStep = prefs.getInteger("daily_streak", 0).coerceIn(0, DAILY_REWARD_DAYS)
        val claimed = last >= today // Don't give another reward after clock rollback.
        val step = when {
            claimed -> oldStep.coerceAtLeast(1)
            last == today - 1 -> (oldStep % DAILY_REWARD_DAYS) + 1
            else -> 1
        }
        return DailyStreakStatus(step, claimed, if (claimed) step else step - 1)
    }

    /** The calendar and the claim path use the same rewards; v1 days 1..7 stay unchanged. */
    fun dailyRewardForDay(day: Int): DailyWalletReward {
        require(day in 1..DAILY_REWARD_DAYS)
        return when (day) {
            7 -> DailyWalletReward(1_500, 25, day)
            14 -> DailyWalletReward(2_500, 40, day)
            in 1..6 -> DailyWalletReward(120 + day * 30, if (day % 3 == 0) 2 else 1, day)
            8, 9, 11, 12 -> DailyWalletReward(120 + day * 30, 0, day)
            10 -> DailyWalletReward(0, 5, day)
            13 -> DailyWalletReward(0, 7, day)
            else -> error("Unconfigured daily reward: $day")
        }
    }

    /**
     * Called only after the player fires Volley balls.
     * One claim per local calendar day. Days 7 and 14 are gifts; day 15 starts a new cycle.
     * Missing any day resets to day 1.
     * The economy preferences are flushed in one commit alongside the wallet balances.
     *
     * For an account-synchronised / server-authoritative economy, move this award to
     * a backend with authenticated timestamps. Local device time can be manipulated.
     */
    @Synchronized fun claimDailyIfAvailable(now: Long = System.currentTimeMillis()): DailyWalletReward? {
        val status = dailyStreakStatus(now)
        if (status.claimedToday) return null
        val day = status.dayToClaim
        val reward = dailyRewardForDay(day)
        coins = (coins.toLong() + reward.coins).coerceAtMost(MAX_BALANCE.toLong()).toInt()
        gems = (gems.toLong() + reward.gems).coerceAtMost(MAX_BALANCE.toLong()).toInt()
        prefs.putLong("daily_day", dayIndex(now))
            .putBoolean("daily_calendar_migrated", true)
            .putInteger("daily_streak", day)
            .putInteger("coins", coins)
            .putInteger("gems", gems)
            .flush()
        return reward
    }

    /** Migrate the older year * 400 + dayOfYear key without granting twice today. */
    private fun lastRewardDay(): Long {
        val old = prefs.getLong("daily_day", Long.MIN_VALUE)
        if (prefs.getBoolean("daily_calendar_migrated", false) || old == Long.MIN_VALUE) return old
        val migrated = if (old in 800_000L..880_000L) {
            val year = (old / 400L).toInt()
            val ordinal = (old % 400L).toInt()
            val date = Calendar.getInstance().apply {
                clear()
                set(year, Calendar.JANUARY, 1, 12, 0, 0)
            }
            if (ordinal in 1..date.getActualMaximum(Calendar.DAY_OF_YEAR)) {
                date.add(Calendar.DAY_OF_YEAR, ordinal - 1)
                dayIndex(date.timeInMillis)
            } else {
                Long.MIN_VALUE
            }
        } else {
            old
        }
        prefs.putLong("daily_day", migrated).putBoolean("daily_calendar_migrated", true).flush()
        return migrated
    }

    /** Individual, predictable prices per charm; available for Coins OR Gems. */
    fun itemCoinPrice(type: PowerUpType): Int = when (type) {
        PowerUpType.LASER_AUTO_CHARGE -> 230
        PowerUpType.EXTRA_LIFE -> 200
        PowerUpType.EXPAND_PADDLE -> 130
        PowerUpType.TIMED_BOMB_BRICKS -> 250
        PowerUpType.RANDOM_GOOD -> 200
        PowerUpType.FIRE_BALL -> 180
        PowerUpType.MAGNETIC_PADDLE -> 140
        PowerUpType.SLOW_BALL -> 120
        PowerUpType.MULTIBALL_PLUS_4 -> 240
        PowerUpType.DUAL_PADDLE -> 160
        PowerUpType.MEGA_BALL -> 240
        PowerUpType.STICKY_PADDLE -> 160
        PowerUpType.ONE_HIT_ANY_BRICK -> 300
        PowerUpType.MULTIBALL_15 -> 550
        else -> 0
    }

    fun itemGemPrice(type: PowerUpType): Int = when (type) {
        PowerUpType.LASER_AUTO_CHARGE -> 7
        PowerUpType.EXTRA_LIFE -> 6
        PowerUpType.EXPAND_PADDLE -> 4
        PowerUpType.TIMED_BOMB_BRICKS -> 8
        PowerUpType.RANDOM_GOOD -> 6
        PowerUpType.FIRE_BALL -> 6
        PowerUpType.MAGNETIC_PADDLE -> 4
        PowerUpType.SLOW_BALL -> 3
        PowerUpType.MULTIBALL_PLUS_4 -> 8
        PowerUpType.DUAL_PADDLE -> 5
        PowerUpType.MEGA_BALL -> 8
        PowerUpType.STICKY_PADDLE -> 5
        PowerUpType.ONE_HIT_ANY_BRICK -> 10
        PowerUpType.MULTIBALL_15 -> 18
        else -> 0
    }

    fun ballCoinPrice(index: Int): Int = 2_500 + (index.coerceAtLeast(0) / 6) * 1_250

    fun ballGemPrice(index: Int): Int = 35 + (index.coerceAtLeast(0) / 6) * 12

    private fun save() {
        prefs.putInteger("coins", coins).putInteger("gems", gems).flush()
    }

    /** Consecutive local dates, including New Year and daylight-saving transitions. */
    private fun dayIndex(now: Long): Long = Math.floorDiv(now + TimeZone.getDefault().getOffset(now).toLong(), 86_400_000L)
}
