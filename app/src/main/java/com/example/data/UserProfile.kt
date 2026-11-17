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
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

typealias PublicProfile = UserProfile

fun UserProfile.formattedJoinDate(): String {
    val date = createdAt?.toDate() ?: return "Just joined"
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
}
