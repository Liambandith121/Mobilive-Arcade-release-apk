package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Keep
@IgnoreExtraProperties
data class UserPrivate(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val coins: Long = 500L,
    val xp: Long = 0L,
    val level: Long = 1L,
    val streakDays: Long = 0L,
    val lastClaimDayIndex: Long = 0L,
    val activeBuffs: List<String> = listOf("STARTER_XP_10"),
    val privacyDmPolicy: String = "everyone",
    val privacyShowOnline: Boolean = true,
    val privacyLanVisible: Boolean = true,
    val autoSaveReplays: Boolean = true,
    val blockedUserIds: List<String> = emptyList(),
    val followingUserIds: List<String> = emptyList(),
    val joinedCommunityIds: List<String> = emptyList(),
    val claimedAchievementIds: List<String> = emptyList(),
    val lastDailyClaimAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GalleryMedia(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val mediaType: String = "screenshot",
    val gameTitle: String = "",
    val mediaUri: String = "",
    val durationSec: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val notifType: String = "system",
    val readFlag: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SocialPost(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorHandle: String = "",
    val authorAvatar: String = "cyber_visor",
    val authorVerified: Boolean = false,
    val postType: String = "POST",
    val visibility: String = "public",
    val gameTag: String = "Minecraft",
    val caption: String = "",
    val mediaUri: String = "",
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val sharesCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PostComment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LiveStream(
    @DocumentId val id: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val hostHandle: String = "",
    val hostAvatar: String = "cyber_visor",
    val title: String = "",
    val gameTitle: String = "Minecraft",
    val streamStatus: String = "live",
    val viewerCount: Long = 1L,
    val peakViewers: Long = 1L,
    val totalGiftsCoins: Long = 0L,
    val slowModeSec: Long = 0L,
    val micEnabled: Boolean = true,
    val recordReplay: Boolean = true,
    val durationSec: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StreamChatMessage(
    @DocumentId val id: String = "",
    val streamId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val text: String = "",
    val messageType: String = "chat",
    val giftCoins: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MultiplayerWorld(
    @DocumentId val id: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val hostAvatar: String = "cyber_visor",
    val worldName: String = "",
    val gameName: String = "Minecraft",
    val gameMode: String = "Survival",
    val lanAddress: String = "local-lan",
    val lanVisible: Boolean = true,
    val multiplayerOn: Boolean = true,
    val worldStatus: String = "open",
    val maxPlayers: Long = 8L,
    val participantIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PartyRoom(
    @DocumentId val id: String = "",
    val leaderId: String = "",
    val leaderName: String = "",
    val partyName: String = "",
    val gameTitle: String = "Minecraft",
    val maxSlots: Long = 4L,
    val memberIds: List<String> = emptyList(),
    val mutedMemberIds: List<String> = emptyList(),
    val voiceActive: Boolean = true,
    val queueState: String = "idle",
    val partyStatus: String = "active",
    val matchedLobbyCode: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GameCommunity(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val name: String = "",
    val gameCategory: String = "",
    val description: String = "",
    val badgeColor: String = "cyan",
    val openCommunity: Boolean = true,
    val memberCount: Long = 1L,
    val memberIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Tournament(
    @DocumentId val id: String = "",
    val organizerId: String = "",
    val title: String = "",
    val gameTitle: String = "",
    val prizeCoins: Long = 500L,
    val entryCoins: Long = 0L,
    val maxTeams: Long = 16L,
    val tournamentState: String = "upcoming",
    val registeredIds: List<String> = emptyList(),
    val scheduleLabel: String = "Today 8:00 PM",
    val startsAtMillis: Long = 0L,
    val formatLabel: String = "Squad 4v4",
    val rulesSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TournamentRegistration(
    @DocumentId val id: String = "",
    val tournamentId: String = "",
    val userId: String = "",
    val playerName: String = "",
    val teamName: String = "",
    val gamerTag: String = "",
    val division: String = "Squad",
    val agreedRules: Boolean = true,
    val entryPaidCoins: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class TournamentPhase {
    UPCOMING,
    LIVE,
    COMPLETED
}

data class TournamentCountdown(
    val phase: TournamentPhase,
    val targetMillis: Long,
    val remainingMillis: Long,
    val days: Long,
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
    val isStartingSoon: Boolean,
    val isFull: Boolean,
    val formattedCompact: String,
    val formattedDigital: String
)

data class TournamentStartPreset(
    val id: String,
    val label: String,
    val offsetMillis: Long
)

@Keep
@IgnoreExtraProperties
data class FriendRequest(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatar: String = "cyber_visor",
    val receiverId: String = "",
    val receiverName: String = "",
    val participantIds: List<String> = emptyList(),
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PartyInvite(
    @DocumentId val id: String = "",
    val partyId: String = "",
    val partyName: String = "",
    val gameTitle: String = "Minecraft",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatar: String = "cyber_visor",
    val receiverId: String = "",
    val receiverName: String = "",
    val participantIds: List<String> = emptyList(),
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val title: String = "",
    val groupChat: Boolean = false,
    val participantIds: List<String> = emptyList(),
    val participantLabels: List<String> = emptyList(),
    val lastMessage: String = "",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val participantIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserReport(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetUserId: String = "",
    val targetHandle: String = "",
    val reason: String = "",
    val details: String = "",
    val status: String = "submitted",
    @ServerTimestamp val createdAt: Timestamp? = null
)

// --- Progression & Arcade Catalog Definitions ---

data class AvatarPreset(
    val id: String,
    val label: String,
    val tagline: String
)

data class DailyRewardDay(
    val day: Int,
    val coins: Long,
    val xp: Long,
    val buffLabel: String?
)

data class DailyRewardStatus(
    val canClaim: Boolean,
    val remainingMillis: Long,
    val lastClaimMillis: Long?,
    val nextClaimMillis: Long?,
    val progressFraction: Float,
    val formattedRemaining: String,
    val formattedShortRemaining: String
)

data class BuffReward(
    val code: String,
    val title: String,
    val description: String,
    val unlockLevel: Int
)

data class AchievementDef(
    val id: String,
    val title: String,
    val description: String,
    val badgeTitle: String,
    val rewardCoins: Long,
    val rewardXp: Long,
    val requiredLevel: Int = 1
)

object ArcadeCatalog {
    val avatarPresets = listOf(
        AvatarPreset("cyber_visor", "Neon Visor", "Esports Pro"),
        AvatarPreset("pixel_knight", "Voxel Knight", "LAN Builder"),
        AvatarPreset("coral_ninja", "Coral Striker", "Battle Ace"),
        AvatarPreset("volt_bot", "Volt Core", "Speedrunner"),
        AvatarPreset("gold_crown", "Arcade King", "Champion")
    )

    val dailyRewards = listOf(
        DailyRewardDay(1, 100L, 40L, null),
        DailyRewardDay(2, 150L, 60L, null),
        DailyRewardDay(3, 220L, 80L, "XP_BOOST_20"),
        DailyRewardDay(4, 300L, 100L, null),
        DailyRewardDay(5, 420L, 140L, "COIN_BUFF_15"),
        DailyRewardDay(6, 550L, 180L, null),
        DailyRewardDay(7, 1000L, 300L, "ELITE_AURA")
    )

    val levelBuffs = listOf(
        BuffReward("STARTER_XP_10", "Starter XP Boost", "+10% XP on streams & posts", 1),
        BuffReward("XP_BOOST_20", "Turbo XP Buff", "+20% XP on daily & matches", 2),
        BuffReward("COIN_BUFF_15", "Gift Coin Bonus", "+15% bonus coins on claims", 3),
        BuffReward("LAN_PRIORITY", "LAN Lobby Boost", "Featured LAN world badge", 4),
        BuffReward("ELITE_AURA", "Creator Aura", "Verified path & neon ring", 5)
    )

    val achievements = listOf(
        AchievementDef("ach_first_login", "Arcade Rookie", "Activate your Mobilive Arcade account", "Rookie", 100L, 50L, 1),
        AchievementDef("ach_level_2", "Level Up Initiate", "Reach Level 2 and unlock Turbo XP Buff", "Level 2", 200L, 100L, 2),
        AchievementDef("ach_level_3", "Coin Collector", "Reach Level 3 to unlock Gift Coin Bonus", "Level 3", 350L, 150L, 3),
        AchievementDef("ach_level_5", "Verified Contender", "Reach Level 5 Creator Aura milestone", "Elite", 750L, 300L, 5)
    )

    val popularGames = listOf(
        "Minecraft",
        "Valorant Mobile",
        "PUBG Mobile",
        "Roblox",
        "Brawl Stars",
        "Genshin Impact",
        "Rocket League Sideswipe"
    )

    val tournamentFormats = listOf(
        "Solo 1v1",
        "Duo 2v2",
        "Squad 4v4"
    )

    val tournamentStartPresets = listOf(
        TournamentStartPreset("15m", "In 15 min", 15L * 60_000L),
        TournamentStartPreset("1h", "In 1 hour", 3600_000L),
        TournamentStartPreset("6h", "In 6 hours", 6L * 3600_000L),
        TournamentStartPreset("24h", "In 24 hours", 24L * 3600_000L),
        TournamentStartPreset("3d", "In 3 days", 3L * 24L * 3600_000L)
    )

    const val DAILY_REWARD_COOLDOWN_MS = 24L * 60L * 60L * 1000L

    fun xpForNextLevel(level: Long): Long = LevelProgressionTracker.thresholdForNextLevel(level)

    fun calculateLevel(xp: Long): Long = LevelProgressionTracker.levelForXp(xp)

    fun dailyRewardStatus(
        lastClaimMillis: Long?,
        nowMillis: Long = System.currentTimeMillis()
    ): DailyRewardStatus {
        if (lastClaimMillis == null || lastClaimMillis <= 0L) {
            return DailyRewardStatus(
                canClaim = true,
                remainingMillis = 0L,
                lastClaimMillis = null,
                nextClaimMillis = null,
                progressFraction = 1f,
                formattedRemaining = "Ready now",
                formattedShortRemaining = "Ready"
            )
        }
        val elapsed = (nowMillis - lastClaimMillis).coerceAtLeast(0L)
        val remaining = (DAILY_REWARD_COOLDOWN_MS - elapsed).coerceAtLeast(0L)
        val canClaim = remaining == 0L
        val progress = (elapsed.toFloat() / DAILY_REWARD_COOLDOWN_MS.toFloat()).coerceIn(0f, 1f)
        return DailyRewardStatus(
            canClaim = canClaim,
            remainingMillis = remaining,
            lastClaimMillis = lastClaimMillis,
            nextClaimMillis = lastClaimMillis + DAILY_REWARD_COOLDOWN_MS,
            progressFraction = if (canClaim) 1f else progress,
            formattedRemaining = if (canClaim) "Ready now" else formatCooldownFull(remaining),
            formattedShortRemaining = if (canClaim) "Ready" else formatCooldownCompact(remaining)
        )
    }

    fun formatCooldownFull(remainingMillis: Long): String {
        val totalSec = ((remainingMillis + 999L) / 1000L).coerceAtLeast(0L)
        val hours = totalSec / 3600L
        val minutes = (totalSec % 3600L) / 60L
        val seconds = totalSec % 60L
        return String.format(Locale.US, "%02dh %02dm %02ds", hours, minutes, seconds)
    }

    fun formatCooldownCompact(remainingMillis: Long): String {
        val totalSec = ((remainingMillis + 999L) / 1000L).coerceAtLeast(0L)
        val hours = totalSec / 3600L
        val minutes = (totalSec % 3600L) / 60L
        val seconds = totalSec % 60L
        return when {
            hours > 0L -> "${hours}h ${minutes}m"
            minutes > 0L -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
}

fun Tournament.effectiveStartsAtMillis(): Long = when {
    startsAtMillis > 0L -> startsAtMillis
    createdAt != null -> createdAt.toDate().time + (6L * 3600_000L)
    else -> 0L
}

fun Tournament.countdownStatus(nowMillis: Long = System.currentTimeMillis()): TournamentCountdown {
    val targetMillis = effectiveStartsAtMillis()
    val isFull = registeredIds.size >= maxTeams.toInt()
    if (tournamentState == "completed") {
        return TournamentCountdown(
            phase = TournamentPhase.COMPLETED,
            targetMillis = targetMillis,
            remainingMillis = 0L,
            days = 0L,
            hours = 0L,
            minutes = 0L,
            seconds = 0L,
            isStartingSoon = false,
            isFull = isFull,
            formattedCompact = "Event Completed",
            formattedDigital = "00:00:00"
        )
    }
    if (tournamentState == "active" || (targetMillis > 0L && nowMillis >= targetMillis)) {
        return TournamentCountdown(
            phase = TournamentPhase.LIVE,
            targetMillis = targetMillis,
            remainingMillis = 0L,
            days = 0L,
            hours = 0L,
            minutes = 0L,
            seconds = 0L,
            isStartingSoon = true,
            isFull = isFull,
            formattedCompact = "Live Now",
            formattedDigital = "LIVE NOW"
        )
    }
    if (targetMillis <= 0L) {
        return TournamentCountdown(
            phase = TournamentPhase.UPCOMING,
            targetMillis = 0L,
            remainingMillis = 0L,
            days = 0L,
            hours = 0L,
            minutes = 0L,
            seconds = 0L,
            isStartingSoon = false,
            isFull = isFull,
            formattedCompact = scheduleLabel.ifBlank { "Starts soon" },
            formattedDigital = "--:--:--"
        )
    }

    val remaining = (targetMillis - nowMillis).coerceAtLeast(0L)
    val totalSec = ((remaining + 999L) / 1000L).coerceAtLeast(0L)
    val days = totalSec / 86400L
    val hours = (totalSec % 86400L) / 3600L
    val minutes = (totalSec % 3600L) / 60L
    val seconds = totalSec % 60L

    val compact = when {
        days > 0L -> String.format(Locale.US, "%dd %02dh %02dm", days, hours, minutes)
        hours > 0L -> String.format(Locale.US, "%02dh %02dm %02ds", hours, minutes, seconds)
        else -> String.format(Locale.US, "%02dm %02ds", minutes, seconds)
    }
    val digital = if (days > 0L) {
        String.format(Locale.US, "%02d:%02d:%02d:%02d", days, hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }
    return TournamentCountdown(
        phase = TournamentPhase.UPCOMING,
        targetMillis = targetMillis,
        remainingMillis = remaining,
        days = days,
        hours = hours,
        minutes = minutes,
        seconds = seconds,
        isStartingSoon = remaining <= 3600_000L,
        isFull = isFull,
        formattedCompact = compact,
        formattedDigital = digital
    )
}

fun UserPrivate?.dailyRewardStatus(nowMillis: Long = System.currentTimeMillis()): DailyRewardStatus =
    ArcadeCatalog.dailyRewardStatus(this?.lastDailyClaimAt?.toDate()?.time, nowMillis)

fun Timestamp?.formattedLastClaimed(): String {
    val date: Date = this?.toDate() ?: return "Not claimed yet"
    return SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(date)
}
