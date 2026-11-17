package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ArcadeCatalog
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.data.formattedJoinDate
import com.example.ui.components.AvatarFace
import com.example.ui.components.EmptyArcadeState
import com.example.ui.components.LevelBadge
import com.example.ui.components.VerifiedBadgeChip
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    profileState: UiState<UserProfile?>,
    isOwnProfile: Boolean,
    isFollowing: Boolean,
    onBack: () -> Unit,
    onToggleFollow: (UserProfile) -> Unit,
    onMessage: (UserProfile) -> Unit,
    onEditProfile: (() -> Unit)? = null
) {
    Scaffold(
        containerColor = ObsidianBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBg),
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("user_profile_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.user_profile_title),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    if (isOwnProfile && onEditProfile != null) {
                        IconButton(
                            onClick = onEditProfile,
                            modifier = Modifier.testTag("user_profile_edit_action")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Edit Profile",
                                tint = CyanNeon
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            when (profileState) {
                is UiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("user_profile_loading"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = CyanNeon)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = stringResource(R.string.profile_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                is UiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("user_profile_error"),
                        verticalArrangement = Arrangement.Center
                    ) {
                        EmptyArcadeState(
                            icon = Icons.Rounded.ErrorOutline,
                            title = "Could not load profile",
                            subtitle = profileState.message,
                            actionLabel = "Go Back",
                            onAction = onBack
                        )
                    }
                }

                is UiState.Success -> {
                    val profile = profileState.data
                    if (profile == null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("user_profile_empty"),
                            verticalArrangement = Arrangement.Center
                        ) {
                            EmptyArcadeState(
                                icon = Icons.Rounded.PersonOff,
                                title = stringResource(R.string.profile_not_found),
                                subtitle = "This player's profile hasn't been created in Firestore yet.",
                                actionLabel = "Go Back",
                                onAction = onBack
                            )
                        }
                    } else {
                        UserProfileDetailsContent(
                            profile = profile,
                            isOwnProfile = isOwnProfile,
                            isFollowing = isFollowing,
                            onToggleFollow = { onToggleFollow(profile) },
                            onMessage = { onMessage(profile) },
                            onEditProfile = onEditProfile
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserProfileDetailsContent(
    profile: UserProfile,
    isOwnProfile: Boolean,
    isFollowing: Boolean,
    onToggleFollow: () -> Unit,
    onMessage: () -> Unit,
    onEditProfile: (() -> Unit)?
) {
    val nextLevelXp = ArcadeCatalog.xpForNextLevel(profile.level)
    val levelProgress = ((profile.xp % 250L).toFloat() / 250f).coerceIn(0f, 1f)
    val joinDateText = stringResource(R.string.joined_date_format, profile.formattedJoinDate())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp)
            .testTag("user_profile_content"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Profile Card: Avatar + Display Name + Handle + Join Date
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AvatarFace(
                    avatarStyle = profile.avatarStyle,
                    photoUri = profile.avatarPhotoUri,
                    size = 96.dp,
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
                    text = "@${profile.handle.ifBlank { "player" }}",
                    style = MaterialTheme.typography.titleSmall,
                    color = CyanNeon,
                    modifier = Modifier.testTag("user_profile_handle")
                )

                // Join Date Pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = ObsidianSurfaceElevated,
                    border = BorderStroke(1.dp, ObsidianOutline),
                    modifier = Modifier.testTag("user_profile_join_date")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
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
                            text = joinDateText,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                    }
                }

                if (profile.bio.isNotBlank()) {
                    Text(
                        text = profile.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("user_profile_bio")
                    )
                }

                if (!isOwnProfile) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
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
                            onClick = onMessage,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("user_profile_message_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChatBubbleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Message", fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else if (onEditProfile != null) {
                    Button(
                        onClick = onEditProfile,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanNeon,
                            contentColor = ObsidianBg
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("user_profile_edit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Profile & Avatar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Account Details & Progression Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianOutline)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Arcade Details",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DetailMetric("Followers", profile.followersCount.toString())
                    DetailMetric("Following", profile.followingCount.toString())
                    DetailMetric("Level", "LV ${profile.level}")
                    DetailMetric("Total XP", profile.xp.toString())
                }

                HorizontalDivider(color = ObsidianOutline)

                DetailInfoRow(
                    icon = Icons.Rounded.SportsEsports,
                    label = "Main Game",
                    value = profile.currentGame.ifBlank { "Minecraft" }
                )
                DetailInfoRow(
                    icon = Icons.Rounded.CalendarToday,
                    label = "Member Since",
                    value = profile.formattedJoinDate()
                )
                DetailInfoRow(
                    icon = Icons.Rounded.Face,
                    label = "Avatar Style",
                    value = ArcadeCatalog.avatarPresets.firstOrNull { it.id == profile.avatarStyle }?.label
                        ?: "Custom Avatar"
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                            .height(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(ObsidianSurfaceElevated),
                        color = VoltGreen,
                        trackColor = ObsidianSurfaceElevated
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailMetric(label: String, value: String) {
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

@Composable
private fun DetailInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = CyanNeon,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}
