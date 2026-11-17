package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
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
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val handle: String = "",
    val displayName: String = "",
    val bio: String = "",
    val avatarStyle: String = "cyber_visor",
    val avatarPhotoUri: String = "",
    val level: Long = 1L,
    val xp: Long = 0L,
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val verifiedBadge: Boolean = false,
    val liveNow: Boolean = false,
    val searchable: Boolean = true,
    val currentGame: String = "Minecraft",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

typealias PublicProfile = UserProfile

fun UserProfile.formattedJoinDate(): String {
    val date = createdAt?.toDate() ?: return "Just joined"
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
}

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
    val lanAddress: String = "192.168.1.100:19132",
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
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
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

    fun xpForNextLevel(level: Long): Long = level * 250L

    fun calculateLevel(xp: Long): Long = (xp / 250L) + 1L
}
