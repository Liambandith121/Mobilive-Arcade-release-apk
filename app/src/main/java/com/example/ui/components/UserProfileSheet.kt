package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ArcadeCatalog
import com.example.data.UserProfile
import com.example.data.formattedJoinDate
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileBottomSheet(
    profile: UserProfile,
    isOwnProfile: Boolean,
    isFollowing: Boolean,
    onDismiss: () -> Unit,
    onToggleFollow: () -> Unit,
    onMessageOrAdd: () -> Unit,
    actionLabel: String = "Message"
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface
    ) {
        UserProfileCard(
            profile = profile,
            isOwnProfile = isOwnProfile,
            isFollowing = isFollowing,
            onToggleFollow = onToggleFollow,
            onMessageOrAdd = onMessageOrAdd,
            actionLabel = actionLabel,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        )
    }
}

@Composable
fun UserProfileCard(
    profile: UserProfile,
    isOwnProfile: Boolean,
    isFollowing: Boolean,
    onToggleFollow: () -> Unit,
    onMessageOrAdd: () -> Unit,
    actionLabel: String = "Message",
    modifier: Modifier = Modifier
) {
    val nextLevelXp = ArcadeCatalog.xpForNextLevel(profile.level)
    val levelProgress = ((profile.xp % 250L).toFloat() / 250f).coerceIn(0f, 1f)

    Column(
        modifier = modifier.testTag("user_profile_card"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AvatarFace(
            avatarStyle = profile.avatarStyle,
            photoUri = profile.avatarPhotoUri,
            size = 84.dp,
            isLive = profile.liveNow,
            showVerified = profile.verifiedBadge || profile.level >= 5L,
            modifier = Modifier.testTag("user_profile_avatar")
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = profile.displayName.ifBlank { "Arcade Player" },
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("user_profile_display_name")
            )
            if (profile.verifiedBadge || profile.level >= 5L) {
                VerifiedBadgeChip()
            }
        }

        Text(
            text = "@${profile.handle.ifBlank { "player" }} • ${profile.currentGame.ifBlank { "Minecraft" }}",
            style = MaterialTheme.typography.labelLarge,
            color = CyanNeon
        )

        Surface(
            shape = RoundedCornerShape(50),
            color = ObsidianSurfaceElevated,
            border = BorderStroke(1.dp, ObsidianOutline)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = "Join Date",
                    tint = GoldCoin,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Joined ${profile.formattedJoinDate()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = GoldCoin,
                    modifier = Modifier.testTag("user_profile_join_date")
                )
            }
        }

        if (profile.bio.isNotBlank()) {
            Text(
                text = profile.bio,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ObsidianSurfaceElevated)
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            UserProfileStat("Followers", profile.followersCount.toString())
            UserProfileStat("Following", profile.followingCount.toString())
            UserProfileStat("Level", "LV ${profile.level}")
            UserProfileStat("XP", profile.xp.toString())
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LevelBadge(level = profile.level)
                Text(
                    text = "${profile.xp} / $nextLevelXp XP",
                    style = MaterialTheme.typography.labelSmall,
                    color = VoltGreen
                )
            }
            LinearProgressIndicator(
                progress = { levelProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = VoltGreen,
                trackColor = ObsidianSurfaceElevated
            )
        }

        if (!isOwnProfile) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onToggleFollow,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowing) ObsidianSurfaceElevated else CyanNeon,
                        contentColor = if (isFollowing) CyanNeon else ObsidianBg
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("user_profile_follow_button")
                ) {
                    Icon(
                        imageVector = if (isFollowing) Icons.Rounded.Check else Icons.Rounded.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isFollowing) "Following" else "Follow", fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = onMessageOrAdd,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("user_profile_action_button")
                ) {
                    Text(actionLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun UserProfileStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}
