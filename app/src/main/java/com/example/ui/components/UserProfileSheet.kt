package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ArcadeCatalog
import com.example.data.PlayerStatusOption
import com.example.data.UserProfile
import com.example.data.formattedJoinDate
import com.example.data.resolvedStatusKey
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
    actionLabel: String = "Message",
    onOpenStatusPicker: (() -> Unit)? = null
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
            onOpenStatusPicker = onOpenStatusPicker,
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
    onOpenStatusPicker: (() -> Unit)? = null,
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

        ProfileStatusButton(
            statusKey = profile.resolvedStatusKey(),
            currentGame = profile.currentGame,
            onClick = if (isOwnProfile) onOpenStatusPicker else null,
            modifier = Modifier.testTag("user_profile_status_button")
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

data class StatusVisualSpec(
    val label: String,
    val color: Color,
    val containerColor: Color,
    val icon: ImageVector
)

fun statusVisualSpec(statusKey: String): StatusVisualSpec = when (statusKey) {
    "live" -> StatusVisualSpec("Broadcasting LIVE", CoralLive, CoralContainer, Icons.Rounded.Sensors)
    PlayerStatusOption.IN_GAME.key -> StatusVisualSpec("In-Game", CyanNeon, CyanContainer, Icons.Rounded.SportsEsports)
    PlayerStatusOption.LFG.key -> StatusVisualSpec("Squad Ready", GoldCoin, GoldContainer, Icons.Rounded.Groups)
    PlayerStatusOption.AWAY.key -> StatusVisualSpec("Away", TextMuted, ObsidianSurfaceElevated, Icons.Rounded.Bedtime)
    else -> StatusVisualSpec("Online", VoltGreen, VoltGreenContainer, Icons.Rounded.RadioButtonChecked)
}

@Composable
fun ProfileStatusButton(
    statusKey: String,
    currentGame: String = "Minecraft",
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val spec = statusVisualSpec(statusKey)
    val statusText = if (statusKey == PlayerStatusOption.IN_GAME.key && currentGame.isNotBlank()) {
        "${spec.label} • $currentGame"
    } else {
        spec.label
    }

    val shape = RoundedCornerShape(if (compact) 50 else 14)
    Surface(
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier
                        .heightIn(min = if (compact) 40.dp else 48.dp)
                        .clip(shape)
                        .clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = shape,
        color = spec.containerColor.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, spec.color.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 10.dp else 14.dp,
                vertical = if (compact) 6.dp else 10.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(spec.color)
            )
            Icon(
                imageVector = spec.icon,
                contentDescription = "Status ${spec.label}",
                tint = spec.color,
                modifier = Modifier.size(if (compact) 15.dp else 18.dp)
            )
            Text(
                text = if (compact) spec.label else "Status: $statusText",
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (compact) Modifier else Modifier.weight(1f, fill = false)
            )
            if (onClick != null) {
                if (!compact) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Change",
                        style = MaterialTheme.typography.labelSmall,
                        color = spec.color,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Change profile status",
                    tint = spec.color,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusPickerBottomSheet(
    currentStatusKey: String,
    currentGame: String,
    isLiveNow: Boolean,
    onDismiss: () -> Unit,
    onSelectStatus: (PlayerStatusOption) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag("status_picker_sheet"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Profile Status",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isLiveNow) {
                            "You are broadcasting LIVE. Pick your standby status too:"
                        } else {
                            "Choose how friends and squads see you in Mobilive Arcade."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            PlayerStatusOption.selectable.forEach { option ->
                val spec = statusVisualSpec(option.key)
                val selected = currentStatusKey == option.key
                val subtitle = if (option == PlayerStatusOption.IN_GAME && currentGame.isNotBlank()) {
                    "Playing $currentGame"
                } else {
                    option.description
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelectStatus(option) }
                        .testTag("status_option_${option.key}"),
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) spec.containerColor.copy(alpha = 0.7f) else ObsidianSurfaceElevated,
                    border = BorderStroke(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) spec.color else ObsidianOutline
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(spec.color.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = spec.icon,
                                contentDescription = option.label,
                                tint = spec.color,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Selected status",
                                tint = spec.color
                            )
                        }
                    }
                }
            }
        }
    }
}

