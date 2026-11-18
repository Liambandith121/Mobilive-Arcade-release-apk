package com.example

import com.example.data.ArcadeCatalog
import com.example.data.LevelProgressionTracker
import com.example.data.LevelTier
import com.example.data.PlayerStatusOption
import com.example.data.Tournament
import com.example.data.TournamentPhase
import com.example.data.UserPrivate
import com.example.data.UserProfile
import com.example.data.countdownStatus
import com.example.data.effectiveStartsAtMillis
import com.example.data.filterAndSortFriends
import com.example.data.formattedJoinDate
import com.example.data.formattedStatusLabel
import com.example.data.isOnlineOrActive
import com.example.data.levelProgress
import com.example.data.resolvedStatusKey
import com.example.data.statusSortPriority
import com.example.data.unwrapFirestoreMessage
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ArcadeLogicUnitTest {

    @Test
    fun `calculateLevel scales every 250 XP`() {
        assertEquals(1L, ArcadeCatalog.calculateLevel(0L))
        assertEquals(1L, ArcadeCatalog.calculateLevel(249L))
        assertEquals(2L, ArcadeCatalog.calculateLevel(250L))
        assertEquals(5L, ArcadeCatalog.calculateLevel(1000L))
    }

    @Test
    fun `xpForNextLevel calculates next threshold`() {
        assertEquals(250L, ArcadeCatalog.xpForNextLevel(1L))
        assertEquals(750L, ArcadeCatalog.xpForNextLevel(3L))
    }

    @Test
    fun `userProfile formattedJoinDate handles null and valid timestamps`() {
        val pendingProfile = UserProfile(displayName = "Nova", createdAt = null)
        assertEquals("Just joined", pendingProfile.formattedJoinDate())

        val epochDate = Date(1700000000000L)
        val expected = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(epochDate)
        val existingProfile = UserProfile(
            displayName = "Nova",
            createdAt = Timestamp(epochDate)
        )
        assertEquals(expected, existingProfile.formattedJoinDate())
    }

    @Test
    fun `dailyRewards spans 7 consecutive days`() {
        assertEquals(7, ArcadeCatalog.dailyRewards.size)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), ArcadeCatalog.dailyRewards.map { it.day })
        assertTrue(ArcadeCatalog.dailyRewards.all { it.coins > 0 && it.xp > 0 })
    }

    @Test
    fun `unwrapFirestoreMessage uses localized message or fallback`() {
        val withMessage = IllegalStateException("Network unavailable")
        assertEquals("Network unavailable", withMessage.unwrapFirestoreMessage("Fallback"))

        val withoutMessage = IllegalStateException()
        assertEquals("Fallback", withoutMessage.unwrapFirestoreMessage("Fallback"))
    }

    @Test
    fun `userProfile resolvedStatusKey and formattedStatusLabel prioritize live and normalize keys`() {
        val liveProfile = UserProfile(liveNow = true, playerStatus = "away")
        assertEquals("live", liveProfile.resolvedStatusKey())
        assertEquals("LIVE", liveProfile.formattedStatusLabel())

        val lfgProfile = UserProfile(liveNow = false, playerStatus = "lfg")
        assertEquals("lfg", lfgProfile.resolvedStatusKey())
        assertEquals("Squad Ready", lfgProfile.formattedStatusLabel())

        val unknownProfile = UserProfile(liveNow = false, playerStatus = "unknown")
        assertEquals(PlayerStatusOption.ONLINE.key, unknownProfile.resolvedStatusKey())
        assertEquals("Online", unknownProfile.formattedStatusLabel())
    }

    @Test
    fun `dailyRewardStatus is ready when never claimed`() {
        val status = ArcadeCatalog.dailyRewardStatus(lastClaimMillis = null, nowMillis = 1_000_000L)
        assertTrue(status.canClaim)
        assertEquals(0L, status.remainingMillis)
        assertEquals(1f, status.progressFraction, 0.001f)
        assertEquals("Ready now", status.formattedRemaining)
        assertEquals("Ready", status.formattedShortRemaining)
    }

    @Test
    fun `dailyRewardStatus locks claim within 24 hours and unlocks at 24 hours`() {
        val lastClaim = 1_700_000_000_000L
        val twelveHoursLater = lastClaim + (12L * 60L * 60L * 1000L)
        val locked = ArcadeCatalog.dailyRewardStatus(lastClaimMillis = lastClaim, nowMillis = twelveHoursLater)
        assertEquals(false, locked.canClaim)
        assertEquals(12L * 60L * 60L * 1000L, locked.remainingMillis)
        assertEquals(0.5f, locked.progressFraction, 0.01f)
        assertEquals("12h 00m 00s", locked.formattedRemaining)
        assertEquals("12h 0m", locked.formattedShortRemaining)

        val exactly24HoursLater = lastClaim + ArcadeCatalog.DAILY_REWARD_COOLDOWN_MS
        val unlocked = ArcadeCatalog.dailyRewardStatus(lastClaimMillis = lastClaim, nowMillis = exactly24HoursLater)
        assertTrue(unlocked.canClaim)
        assertEquals(0L, unlocked.remainingMillis)
        assertEquals(1f, unlocked.progressFraction, 0.001f)
    }

    @Test
    fun `formatCooldownFull and formatCooldownCompact format hours minutes and seconds`() {
        val twoHoursFiveMinNineSec = ((2L * 3600L) + (5L * 60L) + 9L) * 1000L
        assertEquals("02h 05m 09s", ArcadeCatalog.formatCooldownFull(twoHoursFiveMinNineSec))
        assertEquals("2h 5m", ArcadeCatalog.formatCooldownCompact(twoHoursFiveMinNineSec))
        assertEquals("45s", ArcadeCatalog.formatCooldownCompact(45_000L))
    }

    @Test
    fun `levelProgressionTracker computes progress tier and remaining xp`() {
        val progress = UserPrivate(xp = 325L, level = 2L).levelProgress()
        assertEquals(2L, progress.level)
        assertEquals(325L, progress.totalXp)
        assertEquals(250L, progress.currentLevelFloorXp)
        assertEquals(500L, progress.nextLevelThresholdXp)
        assertEquals(75L, progress.xpIntoCurrentLevel)
        assertEquals(175L, progress.xpRemainingToNextLevel)
        assertEquals(0.3f, progress.progressFraction, 0.001f)
        assertEquals(LevelTier.CHALLENGER, progress.tier)
        assertEquals(LevelTier.COLLECTOR, progress.nextTier)
    }

    @Test
    fun `applyXpGain calculates boosted xp level ups and bonus coins`() {
        val gain = LevelProgressionTracker.applyXpGain(
            currentXp = 220L,
            baseXp = 50L,
            multiplierPercent = 120L
        )
        assertEquals(60L, gain.xpEarned)
        assertEquals(280L, gain.newXp)
        assertEquals(1L, gain.previousLevel)
        assertEquals(2L, gain.newLevel)
        assertTrue(gain.leveledUp)
        assertEquals(200L, gain.levelUpBonusCoins)
    }

    @Test
    fun `levelProgressionTracker caps at max level`() {
        val maxProgress = LevelProgressionTracker.fromXp(LevelProgressionTracker.MAX_XP)
        assertEquals(LevelProgressionTracker.MAX_LEVEL, maxProgress.level)
        assertTrue(maxProgress.isMaxLevel)
        assertEquals(1f, maxProgress.progressFraction, 0.001f)
        assertEquals(LevelTier.LEGEND, maxProgress.tier)
    }

    @Test
    fun `isOnlineOrActive and statusSortPriority rank live lfg inGame online ahead of away`() {
        val live = UserProfile(userId = "u1", displayName = "Zed", liveNow = true, playerStatus = "away")
        val lfg = UserProfile(userId = "u2", displayName = "Yara", liveNow = false, playerStatus = "lfg")
        val inGame = UserProfile(userId = "u3", displayName = "Alex", liveNow = false, playerStatus = "in_game")
        val online = UserProfile(userId = "u4", displayName = "Bea", liveNow = false, playerStatus = "online")
        val away = UserProfile(userId = "u5", displayName = "Carl", liveNow = false, playerStatus = "away")

        assertTrue(live.isOnlineOrActive())
        assertTrue(lfg.isOnlineOrActive())
        assertTrue(inGame.isOnlineOrActive())
        assertTrue(online.isOnlineOrActive())
        assertEquals(false, away.isOnlineOrActive())

        val sorted = listOf(away, online, inGame, lfg, live)
            .filterAndSortFriends(query = "", filter = com.example.data.FriendStatusFilter.ALL)
        assertEquals(listOf("u1", "u2", "u3", "u4", "u5"), sorted.map { it.userId })
        assertTrue(live.statusSortPriority() < lfg.statusSortPriority())
        assertTrue(lfg.statusSortPriority() < inGame.statusSortPriority())
        assertTrue(inGame.statusSortPriority() < online.statusSortPriority())
        assertTrue(online.statusSortPriority() < away.statusSortPriority())
    }

    @Test
    fun `filterAndSortFriends filters by status chip and search query`() {
        val friends = listOf(
            UserProfile(userId = "1", displayName = "Aria", handle = "aria_mc", currentGame = "Minecraft", playerStatus = "lfg"),
            UserProfile(userId = "2", displayName = "Blaze", handle = "blaze_val", currentGame = "Valorant Mobile", playerStatus = "in_game"),
            UserProfile(userId = "3", displayName = "Cyrus", handle = "cyrus_afk", currentGame = "Roblox", playerStatus = "away"),
            UserProfile(userId = "4", displayName = "Dune", handle = "dune_live", currentGame = "Minecraft", liveNow = true, playerStatus = "online")
        )

        val onlineOnly = friends.filterAndSortFriends("", com.example.data.FriendStatusFilter.ONLINE)
        assertEquals(listOf("4", "1", "2"), onlineOnly.map { it.userId })

        val lfgOnly = friends.filterAndSortFriends("", com.example.data.FriendStatusFilter.LFG)
        assertEquals(listOf("1"), lfgOnly.map { it.userId })

        val inGameMinecraft = friends.filterAndSortFriends("minecraft", com.example.data.FriendStatusFilter.IN_GAME)
        assertEquals(listOf("4"), inGameMinecraft.map { it.userId })
    }

    @Test
    fun `tournament countdownStatus formats multi-day upcoming countdown`() {
        val now = 1_700_000_000_000L
        val offset = ((2L * 86400L) + (3L * 3600L) + (15L * 60L) + 42L) * 1000L
        val tournament = Tournament(
            id = "t1",
            tournamentState = "upcoming",
            startsAtMillis = now + offset,
            maxTeams = 16L,
            registeredIds = listOf("u1", "u2")
        )

        val status = tournament.countdownStatus(now)
        assertEquals(TournamentPhase.UPCOMING, status.phase)
        assertEquals(2L, status.days)
        assertEquals(3L, status.hours)
        assertEquals(15L, status.minutes)
        assertEquals(42L, status.seconds)
        assertFalse(status.isStartingSoon)
        assertFalse(status.isFull)
        assertEquals("2d 03h 15m", status.formattedCompact)
        assertEquals("02:03:15:42", status.formattedDigital)
    }

    @Test
    fun `tournament countdownStatus flags starting soon within one hour`() {
        val now = 1_700_000_000_000L
        val offset = ((45L * 60L) + 9L) * 1000L
        val tournament = Tournament(
            id = "t2",
            tournamentState = "upcoming",
            startsAtMillis = now + offset,
            maxTeams = 2L,
            registeredIds = listOf("u1", "u2")
        )

        val status = tournament.countdownStatus(now)
        assertEquals(TournamentPhase.UPCOMING, status.phase)
        assertEquals(0L, status.days)
        assertEquals(0L, status.hours)
        assertEquals(45L, status.minutes)
        assertEquals(9L, status.seconds)
        assertTrue(status.isStartingSoon)
        assertTrue(status.isFull)
        assertEquals("45m 09s", status.formattedCompact)
        assertEquals("00:45:09", status.formattedDigital)
    }

    @Test
    fun `tournament countdownStatus transitions to LIVE and COMPLETED and falls back for legacy events`() {
        val now = 1_700_000_000_000L

        val reachedStart = Tournament(
            id = "t3",
            tournamentState = "upcoming",
            startsAtMillis = now - 5_000L
        ).countdownStatus(now)
        assertEquals(TournamentPhase.LIVE, reachedStart.phase)
        assertEquals("LIVE NOW", reachedStart.formattedDigital)

        val explicitActive = Tournament(
            id = "t4",
            tournamentState = "active",
            startsAtMillis = now + 3600_000L
        ).countdownStatus(now)
        assertEquals(TournamentPhase.LIVE, explicitActive.phase)

        val completed = Tournament(
            id = "t5",
            tournamentState = "completed",
            startsAtMillis = now + 3600_000L
        ).countdownStatus(now)
        assertEquals(TournamentPhase.COMPLETED, completed.phase)
        assertEquals("Event Completed", completed.formattedCompact)

        val createdDate = Date(now - 3600_000L) // created 1h ago -> legacy fallback +6h = +5h from now
        val legacy = Tournament(
            id = "t6",
            tournamentState = "upcoming",
            startsAtMillis = 0L,
            createdAt = Timestamp(createdDate)
        )
        assertEquals(createdDate.time + (6L * 3600_000L), legacy.effectiveStartsAtMillis())
        val legacyCountdown = legacy.countdownStatus(now)
        assertEquals(TournamentPhase.UPCOMING, legacyCountdown.phase)
        assertEquals(5L, legacyCountdown.hours)
    }
}
