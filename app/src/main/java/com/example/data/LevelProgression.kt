package com.example.data

enum class LevelTier(
    val minLevel: Long,
    val title: String,
    val badgeTag: String,
    val perkSummary: String
) {
    ROOKIE(1L, "Arcade Rookie", "TIER I", "+10% Starter XP Boost"),
    CHALLENGER(2L, "Challenger", "TIER II", "Unlocks +20% Turbo XP Buff"),
    COLLECTOR(3L, "Coin Tactician", "TIER III", "Unlocks +15% Gift Coin Bonus"),
    VETERAN(4L, "LAN Veteran", "TIER IV", "Unlocks Featured LAN Lobby Boost"),
    LEGEND(5L, "Creator Legend", "TIER V", "Creator Aura & Verified Path");

    companion object {
        fun forLevel(level: Long): LevelTier =
            entries.lastOrNull { level >= it.minLevel } ?: ROOKIE

        fun nextAfter(level: Long): LevelTier? =
            entries.firstOrNull { it.minLevel > level }
    }
}

data class LevelProgress(
    val level: Long,
    val totalXp: Long,
    val currentLevelFloorXp: Long,
    val nextLevelThresholdXp: Long,
    val xpIntoCurrentLevel: Long,
    val xpRequiredForLevel: Long,
    val xpRemainingToNextLevel: Long,
    val progressFraction: Float,
    val tier: LevelTier,
    val nextTier: LevelTier?,
    val isMaxLevel: Boolean
)

data class XpGainResult(
    val previousXp: Long,
    val newXp: Long,
    val xpEarned: Long,
    val previousLevel: Long,
    val newLevel: Long,
    val leveledUp: Boolean,
    val levelUpBonusCoins: Long
)

data class XpSourceInfo(
    val id: String,
    val actionLabel: String,
    val xpLabel: String
)

object LevelProgressionTracker {
    const val XP_PER_LEVEL = 250L
    const val MAX_LEVEL = 999L
    const val MAX_XP = 100_000_000L
    const val LEVEL_UP_COIN_BONUS = 200L

    val xpSources = listOf(
        XpSourceInfo("daily", "Daily Bonus", "+40–300 XP"),
        XpSourceInfo("post", "Publish Post", "+35 XP"),
        XpSourceInfo("stream", "Go Live", "+80 XP"),
        XpSourceInfo("world", "Host World", "+45 XP"),
        XpSourceInfo("badge", "Badges", "+50–300 XP")
    )

    fun levelForXp(xp: Long): Long {
        val safeXp = xp.coerceIn(0L, MAX_XP)
        return ((safeXp / XP_PER_LEVEL) + 1L).coerceIn(1L, MAX_LEVEL)
    }

    fun thresholdForNextLevel(level: Long): Long {
        val safeLevel = level.coerceIn(1L, MAX_LEVEL)
        return safeLevel * XP_PER_LEVEL
    }

    fun fromXp(totalXp: Long): LevelProgress {
        val safeXp = totalXp.coerceIn(0L, MAX_XP)
        val level = levelForXp(safeXp)
        val isMax = level >= MAX_LEVEL
        val floorXp = (level - 1L) * XP_PER_LEVEL
        val nextThreshold = level * XP_PER_LEVEL
        val xpIntoLevel = if (isMax) XP_PER_LEVEL else (safeXp - floorXp).coerceIn(0L, XP_PER_LEVEL)
        val remaining = if (isMax) 0L else (nextThreshold - safeXp).coerceAtLeast(0L)
        val fraction = if (isMax) 1f else (xpIntoLevel.toFloat() / XP_PER_LEVEL.toFloat()).coerceIn(0f, 1f)
        return LevelProgress(
            level = level,
            totalXp = safeXp,
            currentLevelFloorXp = floorXp,
            nextLevelThresholdXp = nextThreshold,
            xpIntoCurrentLevel = xpIntoLevel,
            xpRequiredForLevel = XP_PER_LEVEL,
            xpRemainingToNextLevel = remaining,
            progressFraction = fraction,
            tier = LevelTier.forLevel(level),
            nextTier = LevelTier.nextAfter(level),
            isMaxLevel = isMax
        )
    }

    fun applyXpGain(
        currentXp: Long,
        baseXp: Long,
        multiplierPercent: Long = 100L
    ): XpGainResult {
        val safeCurrentXp = currentXp.coerceIn(0L, MAX_XP)
        val previousLevel = levelForXp(safeCurrentXp)
        val safeBase = baseXp.coerceAtLeast(0L)
        val safeMultiplier = multiplierPercent.coerceAtLeast(100L)
        val earned = (safeBase * safeMultiplier) / 100L
        val newXp = (safeCurrentXp + earned).coerceIn(0L, MAX_XP)
        val newLevel = levelForXp(newXp)
        val levelsGained = (newLevel - previousLevel).coerceAtLeast(0L)
        return XpGainResult(
            previousXp = safeCurrentXp,
            newXp = newXp,
            xpEarned = earned,
            previousLevel = previousLevel,
            newLevel = newLevel,
            leveledUp = levelsGained > 0L,
            levelUpBonusCoins = levelsGained * LEVEL_UP_COIN_BONUS
        )
    }
}

fun UserPrivate?.levelProgress(): LevelProgress =
    LevelProgressionTracker.fromXp(this?.xp ?: 0L)

fun UserProfile?.levelProgress(): LevelProgress =
    LevelProgressionTracker.fromXp(this?.xp ?: 0L)
