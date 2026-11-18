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
    val playerStatus: String = "online",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

typealias PublicProfile = UserProfile

enum class PlayerStatusOption(
    val key: String,
    val label: String,
    val description: String
) {
    ONLINE("online", "Online", "Visible & ready for invites"),
    IN_GAME("in_game", "In-Game", "Currently playing in a session"),
    LFG("lfg", "Squad Ready", "Looking for co-op & LAN players"),
    AWAY("away", "Away", "Appear offline & pause invites");

    companion object {
        val selectable = listOf(ONLINE, IN_GAME, LFG, AWAY)

        fun fromKey(key: String?): PlayerStatusOption =
            selectable.firstOrNull { it.key == key } ?: ONLINE
    }
}

fun UserProfile.resolvedStatusKey(): String = when {
    liveNow -> "live"
    else -> PlayerStatusOption.fromKey(playerStatus).key
}

fun UserProfile.formattedStatusLabel(): String = when {
    liveNow -> "LIVE"
    else -> PlayerStatusOption.fromKey(playerStatus).label
}

fun UserProfile.formattedJoinDate(): String {
    val date = createdAt?.toDate() ?: return "Just joined"
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
}

fun UserProfile.isOnlineOrActive(): Boolean =
    liveNow || PlayerStatusOption.fromKey(playerStatus) != PlayerStatusOption.AWAY

fun UserProfile.statusSortPriority(): Int = when (resolvedStatusKey()) {
    "live" -> 0
    PlayerStatusOption.LFG.key -> 1
    PlayerStatusOption.IN_GAME.key -> 2
    PlayerStatusOption.ONLINE.key -> 3
    else -> 4
}

enum class FriendStatusFilter(val label: String) {
    ALL("All"),
    ONLINE("Online"),
    IN_GAME("In-Game"),
    LFG("Squad Ready");

    fun matches(profile: UserProfile): Boolean = when (this) {
        ALL -> true
        ONLINE -> profile.isOnlineOrActive()
        IN_GAME -> profile.resolvedStatusKey() == PlayerStatusOption.IN_GAME.key || profile.liveNow
        LFG -> profile.resolvedStatusKey() == PlayerStatusOption.LFG.key
    }
}

fun List<UserProfile>.filterAndSortFriends(
    query: String,
    filter: FriendStatusFilter
): List<UserProfile> {
    val q = query.trim().lowercase()
    return filter { profile ->
        filter.matches(profile) && (
            q.isEmpty() ||
                profile.displayName.lowercase().contains(q) ||
                profile.handle.lowercase().contains(q) ||
                profile.currentGame.lowercase().contains(q)
            )
    }.sortedWith(
        compareBy<UserProfile> { it.statusSortPriority() }
            .thenBy { it.displayName.lowercase() }
    )
}

