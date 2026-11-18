package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ArcadeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ArcadeViewModel,
    userPrivate: UserPrivate?,
    myProfileState: UiState<UserProfile?>,
    galleryState: UiState<List<GalleryMedia>>,
    streamsState: UiState<List<LiveStream>>,
    postsState: UiState<List<SocialPost>>,
    allProfilesState: UiState<List<UserProfile>>,
    openSettingsInitially: Boolean = false
) {
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val myProfile = (myProfileState as? UiState.Success)?.data
    val gallery = remember(galleryState) { (galleryState as? UiState.Success)?.data.orEmpty() }
    val myStreams = remember(streamsState, viewModel.currentUserId) {
        (streamsState as? UiState.Success)?.data.orEmpty().filter { it.hostId == viewModel.currentUserId }
    }
    val myPosts = remember(postsState, viewModel.currentUserId) {
        (postsState as? UiState.Success)?.data.orEmpty().filter { it.authorId == viewModel.currentUserId }
    }
    val allProfiles = remember(allProfilesState) { (allProfilesState as? UiState.Success)?.data.orEmpty() }

    var selectedSection by remember { mutableIntStateOf(if (openSettingsInitially) 3 else 0) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showUploadMediaDialog by remember { mutableStateOf(false) }
    var showStatusSheet by remember { mutableStateOf(false) }
    val isClaimingDailyReward by viewModel.isClaimingDailyReward.collectAsStateWithLifecycle()

    val resolvedStatusKey = remember(myProfile, userPrivate) {
        when {
            myProfile?.liveNow == true -> "live"
            userPrivate?.privacyShowOnline == false && (myProfile?.playerStatus.isNullOrBlank() || myProfile?.playerStatus == PlayerStatusOption.ONLINE.key) -> PlayerStatusOption.AWAY.key
            else -> PlayerStatusOption.fromKey(myProfile?.playerStatus).key
        }
    }

    LaunchedEffect(openSettingsInitially) {
        if (openSettingsInitially) selectedSection = 3
    }

    val progress = remember(userPrivate) { userPrivate.levelProgress() }
    val level = progress.level
    val xp = progress.totalXp

    Scaffold(
        containerColor = ObsidianBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBg),
                title = { Text("Arcade Profile & Rewards", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    ProfileStatusButton(
                        statusKey = resolvedStatusKey,
                        currentGame = myProfile?.currentGame ?: "Minecraft",
                        onClick = { showStatusSheet = true },
                        compact = true,
                        modifier = Modifier.testTag("topbar_status_button")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { showEditProfileDialog = true },
                        modifier = Modifier.testTag("edit_profile_button")
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit Profile & Avatar", tint = CyanNeon)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. User Profile Header Card (Avatar Face + Verified Badge + Join Date + Stats)
            item {
                when (myProfileState) {
                    is UiState.Loading -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = ObsidianSurface,
                            border = BorderStroke(1.dp, ObsidianOutline)
                        ) {
                            Row(
                                modifier = Modifier.padding(24.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                CircularProgressIndicator(color = CyanNeon, modifier = Modifier.size(28.dp))
                                Text("Loading UserProfile from Firestore...", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                        }
                    }

                    is UiState.Error -> {
                        EmptyArcadeState(
                            icon = Icons.Rounded.ErrorOutline,
                            title = "Failed to load UserProfile",
                            subtitle = myProfileState.message
                        )
                    }

                    is UiState.Success -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = ObsidianSurface,
                            border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.45f))
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    AvatarFace(
                                        avatarStyle = myProfile?.avatarStyle ?: "cyber_visor",
                                        photoUri = myProfile?.avatarPhotoUri.orEmpty(),
                                        size = 72.dp,
                                        isLive = myProfile?.liveNow == true,
                                        showVerified = (myProfile?.verifiedBadge == true) || level >= 5L,
                                        modifier = Modifier.clickable { showEditProfileDialog = true }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = myProfile?.displayName ?: "Arcade Player",
                                                style = MaterialTheme.typography.headlineSmall,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (myProfile?.verifiedBadge == true || level >= 5L) {
                                                VerifiedBadgeChip()
                                            }
                                        }
                                        Text(
                                            text = "@${myProfile?.handle ?: "player"} • ${myProfile?.currentGame ?: "Minecraft"}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = CyanNeon
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.CalendarMonth,
                                                contentDescription = "Join Date",
                                                tint = GoldCoin,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "Joined ${myProfile?.formattedJoinDate() ?: "Just joined"}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = GoldCoin,
                                                modifier = Modifier.testTag("profile_join_date")
                                            )
                                        }
                                        Text(
                                            text = myProfile?.bio ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            maxLines = 2
                                        )
                                    }
                                }

                                ProfileStatusButton(
                                    statusKey = resolvedStatusKey,
                                    currentGame = myProfile?.currentGame ?: "Minecraft",
                                    onClick = { showStatusSheet = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("profile_status_button")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    ProfileStatItem("Followers", (myProfile?.followersCount ?: 0L).toString())
                                    ProfileStatItem("Following", (myProfile?.followingCount ?: 0L).toString())
                                    ProfileStatItem("Streak", "${userPrivate?.streakDays ?: 0L}d")
                                    ProfileStatItem("Gift Coins", (userPrivate?.coins ?: 500L).toString())
                                }

                                // Level & XP Bar
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            LevelBadge(level = progress.level)
                                            Text(
                                                text = progress.tier.title,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = CyanNeon,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Text(
                                            text = "${progress.xpIntoCurrentLevel} / ${progress.xpRequiredForLevel} XP • ${progress.totalXp} Total",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = VoltGreen
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { progress.progressFraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(50)),
                                        color = VoltGreen,
                                        trackColor = ObsidianSurfaceElevated
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Sub-Section Tabs: Rewards & Buffs | Gallery | Analytics & Badges | More Settings
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val tabs = listOf("Level & Daily Coins", "Gallery", "Analytics & Badges", "More Settings")
                    items(tabs.indices.toList(), key = { it }) { idx ->
                        FilterChip(
                            selected = selectedSection == idx,
                            onClick = { selectedSection = idx },
                            label = { Text(tabs[idx]) },
                            modifier = Modifier.testTag("profile_tab_$idx")
                        )
                    }
                }
            }

            when (selectedSection) {
                0 -> {
                    // Gamified Level & XP Progression Tracker
                    item {
                        LevelProgressionCard(userPrivate = userPrivate)
                    }

                    // 7-Day Gift Coins Daily Claim (24h Firestore cooldown)
                    item {
                        DailyRewardSectionCard(
                            userPrivate = userPrivate,
                            isClaiming = isClaimingDailyReward,
                            onClaim = { viewModel.claimDailyReward() }
                        )
                    }

                    // Level Up Buff Rewards
                    item {
                        Text("Level Up Buff Rewards", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    items(ArcadeCatalog.levelBuffs, key = { it.code }) { buff ->
                        val unlocked = level >= buff.unlockLevel || (userPrivate?.activeBuffs?.contains(buff.code) == true)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = ObsidianSurface,
                            border = BorderStroke(1.dp, if (unlocked) VoltGreen.copy(alpha = 0.5f) else ObsidianOutline)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (unlocked) VoltGreenContainer else ObsidianSurfaceElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (unlocked) Icons.Rounded.AutoAwesome else Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = if (unlocked) VoltGreen else TextMuted
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(buff.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                    Text(buff.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                Text(
                                    text = if (unlocked) "ACTIVE" else "LV ${buff.unlockLevel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (unlocked) VoltGreen else TextMuted
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // Upload Videos, Screenshots & Highlights Gallery
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Videos, Screenshots & Highlights", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Button(
                                onClick = { showUploadMediaDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = ObsidianBg),
                                modifier = Modifier.testTag("upload_gallery_button")
                            ) {
                                Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (gallery.isEmpty()) {
                        item {
                            EmptyArcadeState(
                                icon = Icons.Rounded.Collections,
                                title = "Your Arcade Gallery is empty",
                                subtitle = "Upload Videos, Screenshots, and Highlights securely using the Android Photo Picker.",
                                actionLabel = "Upload Media",
                                onAction = { showUploadMediaDialog = true }
                            )
                        }
                    } else {
                        items(gallery, key = { it.id }) { item ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = ObsidianSurface,
                                border = BorderStroke(1.dp, ObsidianOutline)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (item.mediaUri.isNotBlank()) {
                                        AsyncImage(
                                            model = item.mediaUri,
                                            contentDescription = item.title,
                                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(CyanContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Rounded.VideoLibrary, contentDescription = null, tint = CyanNeon)
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                        Text("${item.mediaType.uppercase()} • ${item.gameTitle}", style = MaterialTheme.typography.labelSmall, color = CyanNeon)
                                    }
                                    IconButton(onClick = { viewModel.deleteGalleryMedia(item.id) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = CoralLive)
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Creator Analytics + Achievements & Badges
                    item {
                        val totalLikes = remember(myPosts) { myPosts.sumOf { it.likesCount } }
                        val totalGifts = remember(myStreams) { myStreams.sumOf { it.totalGiftsCoins } }
                        AnalyticsCanvasCard(
                            postsCount = myPosts.size,
                            streamsCount = myStreams.size,
                            totalLikes = totalLikes,
                            totalGifts = totalGifts,
                            xp = xp
                        )
                    }

                    item {
                        Text("Achievements & Badges", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }

                    items(ArcadeCatalog.achievements, key = { it.id }) { ach ->
                        val claimed = userPrivate?.claimedAchievementIds?.contains(ach.id) == true
                        val eligible = level >= ach.requiredLevel
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = ObsidianSurface,
                            border = BorderStroke(1.dp, if (claimed) VoltGreen.copy(alpha = 0.4f) else GoldCoin.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MilitaryTech,
                                    contentDescription = null,
                                    tint = if (claimed) VoltGreen else GoldCoin,
                                    modifier = Modifier.size(32.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ach.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                    Text("${ach.description} (+${ach.rewardCoins} Coins)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                Button(
                                    onClick = { viewModel.claimAchievement(ach) },
                                    enabled = eligible && !claimed,
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldCoin, contentColor = ObsidianBg)
                                ) {
                                    Text(if (claimed) "Claimed" else if (eligible) "Claim" else "LV ${ach.requiredLevel}")
                                }
                            }
                        }
                    }
                }

                else -> {
                    // Settings, More Settings, Privacy Controls, Moderation & Blocked Users
                    item {
                        PrivacyAndSettingsCard(
                            userPrivate = userPrivate,
                            allProfiles = allProfiles,
                            onSavePrivacy = { dm, online, lan, replay ->
                                viewModel.updatePrivacySettings(dm, online, lan, replay)
                            },
                            onUnblock = { uid, handle ->
                                viewModel.toggleBlockUser(uid, handle)
                            },
                            onSignOut = { viewModel.signOut(credentialManager) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showEditProfileDialog) {
        EditProfileAvatarDialog(
            profile = myProfile,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, handle, bio, avatarStyle, photoUri, game ->
                viewModel.completeOnboarding(name, handle, bio, avatarStyle, photoUri, game)
                showEditProfileDialog = false
            }
        )
    }

    if (showUploadMediaDialog) {
        UploadGalleryDialog(
            onDismiss = { showUploadMediaDialog = false },
            onUpload = { title, type, game, uri ->
                viewModel.addGalleryMedia(title, type, game, uri, if (type == "screenshot") 0L else 30L)
                showUploadMediaDialog = false
            }
        )
    }

    if (showStatusSheet) {
        StatusPickerBottomSheet(
            currentStatusKey = PlayerStatusOption.fromKey(myProfile?.playerStatus).key,
            currentGame = myProfile?.currentGame ?: "Minecraft",
            isLiveNow = myProfile?.liveNow == true,
            onDismiss = { showStatusSheet = false },
            onSelectStatus = { option ->
                viewModel.updatePlayerStatus(option.key)
                showStatusSheet = false
            }
        )
    }
}

@Composable
private fun ProfileStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

private val AnalyticsBarColors = listOf(CoralLive, CyanNeon, VoltGreen, GoldCoin, VioletArcade)
private val AnalyticsBarLabels = listOf("Live", "Posts", "Likes", "Gifts", "XP")

@Composable
private fun AnalyticsCanvasCard(
    postsCount: Int,
    streamsCount: Int,
    totalLikes: Long,
    totalGifts: Long,
    xp: Long
) {
    val bars = remember(streamsCount, postsCount, totalLikes, totalGifts, xp) {
        listOf(
            (streamsCount.coerceAtLeast(1) * 20f),
            (postsCount.coerceAtLeast(1) * 18f),
            (totalLikes.toFloat().coerceAtLeast(10f)),
            (totalGifts.toFloat().coerceAtLeast(15f)),
            ((xp / 10f).coerceAtLeast(12f))
        )
    }
    val maxVal = remember(bars) { bars.maxOrNull()?.coerceAtLeast(1f) ?: 1f }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Creator & Stream Analytics", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ProfileStatItem("Streams", streamsCount.toString())
                ProfileStatItem("Posts/Shorts", postsCount.toString())
                ProfileStatItem("Likes", totalLikes.toString())
                ProfileStatItem("Gifts", totalGifts.toString())
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                val barCount = bars.size
                val slotWidth = size.width / barCount
                val barWidth = slotWidth * 0.48f
                bars.forEachIndexed { idx, v ->
                    val ratio = (v / maxVal).coerceIn(0.12f, 1f)
                    val barHeight = size.height * ratio
                    val x = idx * slotWidth + (slotWidth - barWidth) / 2f
                    val y = size.height - barHeight
                    drawRoundRect(
                        color = AnalyticsBarColors[idx],
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                AnalyticsBarLabels.forEach { lbl ->
                    Text(lbl, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun PrivacyAndSettingsCard(
    userPrivate: UserPrivate?,
    allProfiles: List<PublicProfile>,
    onSavePrivacy: (dmPolicy: String, showOnline: Boolean, lanVisible: Boolean, autoSaveReplays: Boolean) -> Unit,
    onUnblock: (uid: String, handle: String) -> Unit,
    onSignOut: () -> Unit
) {
    var dmPolicy by remember(userPrivate) { mutableStateOf(userPrivate?.privacyDmPolicy ?: "everyone") }
    var showOnline by remember(userPrivate) { mutableStateOf(userPrivate?.privacyShowOnline ?: true) }
    var lanVisible by remember(userPrivate) { mutableStateOf(userPrivate?.privacyLanVisible ?: true) }
    var autoReplay by remember(userPrivate) { mutableStateOf(userPrivate?.autoSaveReplays ?: true) }
    val blockedIds = userPrivate?.blockedUserIds.orEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianOutline)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Privacy Controls & More Settings", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text("Who can message you:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("everyone", "friends", "nobody").forEach { policy ->
                        FilterChip(
                            selected = dmPolicy == policy,
                            onClick = { dmPolicy = policy },
                            label = { Text(policy.replaceFirstChar(Char::titlecase)) }
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Show Online Status", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = showOnline, onCheckedChange = { showOnline = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Visible to LAN Players", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = lanVisible, onCheckedChange = { lanVisible = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Auto-Save Stream Replays", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = autoReplay, onCheckedChange = { autoReplay = it })
                }
                Button(
                    onClick = { onSavePrivacy(dmPolicy, showOnline, lanVisible, autoReplay) },
                    modifier = Modifier.fillMaxWidth().testTag("save_privacy_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = ObsidianBg)
                ) {
                    Text("Save Settings", fontWeight = FontWeight.Bold)
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianOutline)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Moderation & Blocked Users (${blockedIds.size})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                if (blockedIds.isEmpty()) {
                    Text("No blocked users.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                } else {
                    blockedIds.forEach { uid ->
                        val prof = allProfiles.firstOrNull { it.userId == uid }
                        val handle = prof?.handle ?: uid.take(8)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("@$handle", style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { onUnblock(uid, handle) }) {
                                Text("Unblock", color = CyanNeon)
                            }
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sign_out_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralLive),
            border = BorderStroke(1.dp, CoralLive)
        ) {
            Icon(Icons.Rounded.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EditProfileAvatarDialog(
    profile: PublicProfile?,
    onDismiss: () -> Unit,
    onSave: (name: String, handle: String, bio: String, avatarStyle: String, photoUri: String, game: String) -> Unit
) {
    var name by remember { mutableStateOf(profile?.displayName ?: "Arcade Player") }
    var handle by remember { mutableStateOf(profile?.handle ?: "arcade_pro") }
    var bio by remember { mutableStateOf(profile?.bio ?: "") }
    var selectedAvatar by remember { mutableStateOf(profile?.avatarStyle ?: "cyber_visor") }
    var photoUri by remember { mutableStateOf(profile?.avatarPhotoUri ?: "") }
    var game by remember { mutableStateOf(profile?.currentGame ?: "Minecraft") }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) photoUri = uri.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile & Avatar Face") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Choose Avatar (Profile Face):", style = MaterialTheme.typography.labelMedium, color = CyanNeon)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(ArcadeCatalog.avatarPresets, key = { it.id }) { preset ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedAvatar = preset.id
                                    photoUri = ""
                                }
                                .padding(4.dp)
                        ) {
                            AvatarFace(
                                avatarStyle = preset.id,
                                size = 48.dp,
                                isLive = selectedAvatar == preset.id && photoUri.isBlank()
                            )
                            Text(preset.label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Face, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (photoUri.isBlank()) "Pick Custom Face Photo" else "Custom Photo Selected")
                }
                OutlinedTextField(value = name, onValueChange = { name = it.take(50) }, label = { Text("Display Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = handle, onValueChange = { handle = it.take(30) }, label = { Text("Handle") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bio, onValueChange = { bio = it.take(200) }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = game, onValueChange = { game = it.take(60) }, label = { Text("Favorite Game") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, handle, bio, selectedAvatar, photoUri, game) },
                enabled = name.isNotBlank() && handle.length >= 2
            ) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun UploadGalleryDialog(
    onDismiss: () -> Unit,
    onUpload: (title: String, mediaType: String, game: String, uri: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var mediaType by remember { mutableStateOf("screenshot") }
    var game by remember { mutableStateOf("Minecraft") }
    var uri by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { picked ->
        if (picked != null) uri = picked.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload to Arcade Gallery") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf("screenshot", "video", "highlight").forEachIndexed { idx, t ->
                        SegmentedButton(
                            selected = mediaType == t,
                            onClick = { mediaType = t },
                            shape = SegmentedButtonDefaults.itemShape(idx, 3)
                        ) {
                            Text(t.replaceFirstChar(Char::titlecase), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it.take(100) }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = game, onValueChange = { game = it.take(60) }, label = { Text("Game") }, modifier = Modifier.fillMaxWidth())
                OutlinedButton(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.PermMedia, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (uri.isBlank()) "Choose Photo or Video" else "Media Ready")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onUpload(title.ifBlank { "Arcade Capture" }, mediaType, game, uri) },
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = ObsidianBg)
            ) {
                Text("Save to Gallery", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
