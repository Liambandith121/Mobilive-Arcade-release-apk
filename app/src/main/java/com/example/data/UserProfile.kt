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
