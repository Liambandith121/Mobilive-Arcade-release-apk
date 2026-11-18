package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ArcadeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ArcadeViewModel,
    userPrivate: UserPrivate?,
    myProfile: UserProfile?,
    postsState: UiState<List<SocialPost>>,
    streamsState: UiState<List<LiveStream>>,
    profilesState: UiState<List<UserProfile>>,
    worldsState: UiState<List<MultiplayerWorld>>,
    notificationsState: UiState<List<UserNotification>>,
    onNavigateWatch: () -> Unit,
    onNavigatePlay: () -> Unit,
    onNavigateProfile: () -> Unit,
    onOpenStream: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val blockedIds = remember(userPrivate) { userPrivate?.blockedUserIds.orEmpty().toSet() }
    val posts = remember(postsState, blockedIds) {
        (postsState as? UiState.Success)?.data.orEmpty().filter { it.authorId !in blockedIds }
    }
    val stories = remember(posts) { posts.filter { it.postType == "STORY" } }
    val feedPosts = remember(posts) { posts.filter { it.postType == "POST" } }
    val liveStreams = remember(streamsState, blockedIds) {
        (streamsState as? UiState.Success)?.data.orEmpty()
            .filter { it.streamStatus == "live" && it.hostId !in blockedIds }
    }
    val notifications = remember(notificationsState) {
        (notificationsState as? UiState.Success)?.data.orEmpty()
    }
    val unreadNotifs = remember(notifications) { notifications.count { !it.readFlag } }

    var showCreateDialog by remember { mutableStateOf(false) }
    var initialCreateType by remember { mutableStateOf("POST") }
    var showSearchSheet by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var viewingStory by remember { mutableStateOf<SocialPost?>(null) }
    var reportingTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedUserProfile by remember { mutableStateOf<UserProfile?>(null) }
    var showStatusSheet by remember { mutableStateOf(false) }

    val myStatusKey = remember(myProfile, userPrivate) {
        when {
            myProfile?.liveNow == true -> "live"
            userPrivate?.privacyShowOnline == false && (myProfile?.playerStatus.isNullOrBlank() || myProfile?.playerStatus == PlayerStatusOption.ONLINE.key) -> PlayerStatusOption.AWAY.key
            else -> PlayerStatusOption.fromKey(myProfile?.playerStatus).key
        }
    }
    val myStatusSpec = remember(myStatusKey) { statusVisualSpec(myStatusKey) }

    Scaffold(
        containerColor = ObsidianBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBg),
                title = {
                    Column {
                        Text(
                            text = "MOBILIVE ARCADE",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showStatusSheet = true }
                                .padding(vertical = 2.dp)
                                .testTag("home_status_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(myStatusSpec.color)
                            )
                            Text(
                                text = myProfile?.let { "@${it.handle} • ${myStatusSpec.label}" } ?: "Arcade Feed",
                                style = MaterialTheme.typography.labelSmall,
                                color = myStatusSpec.color,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    CoinPill(
                        coins = userPrivate?.coins ?: 500L,
                        onClick = onNavigateProfile
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { showSearchSheet = true },
                        modifier = Modifier.testTag("home_search_button")
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = "Search", tint = TextPrimary)
                    }
                    IconButton(
                        onClick = { showNotificationsSheet = true },
                        modifier = Modifier.testTag("home_notifications_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifs > 0) {
                                    Badge(containerColor = CoralLive) {
                                        Text(unreadNotifs.toString(), color = Color.White)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Rounded.Notifications, contentDescription = "Notifications", tint = TextPrimary)
                        }
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("home_settings_button")
                    ) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = TextPrimary)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    initialCreateType = "POST"
                    showCreateDialog = true
                },
                containerColor = CyanNeon,
                contentColor = ObsidianBg,
                modifier = Modifier.testTag("create_post_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stories Tray
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Arcade Stories", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        TextButton(onClick = {
                            initialCreateType = "STORY"
                            showCreateDialog = true
                        }) {
                            Text("+ Add Story", color = CyanNeon)
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(72.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        initialCreateType = "STORY"
                                        showCreateDialog = true
                                    }
                                    .padding(4.dp)
                                    .testTag("add_story_button")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(58.dp)
                                        .clip(CircleShape)
                                        .background(CyanContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.AddAPhoto, contentDescription = "Add Story", tint = CyanNeon)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Your Story", style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
                            }
                        }
                        items(stories, key = { it.id }) { story ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(72.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { viewingStory = story }
                                    .padding(4.dp)
                            ) {
                                AvatarFace(
                                    avatarStyle = story.authorAvatar,
                                    size = 58.dp,
                                    isLive = true,
                                    showVerified = story.authorVerified
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = story.authorName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Hero Featured Banner
            item {
                HeroArcadeBanner(
                    liveStreams = liveStreams,
                    onGoLive = onNavigateWatch,
                    onOpenStream = onOpenStream
                )
            }

            // Quick Actions Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        icon = Icons.Rounded.LiveTv,
                        label = "Go Live",
                        accent = CoralLive,
                        onClick = onNavigateWatch,
                        modifier = Modifier.weight(1f).testTag("quick_go_live")
                    )
                    QuickActionTile(
                        icon = Icons.Rounded.WifiTethering,
                        label = "Host LAN",
                        accent = VoltGreen,
                        onClick = onNavigatePlay,
                        modifier = Modifier.weight(1f).testTag("quick_host_lan")
                    )
                    QuickActionTile(
                        icon = Icons.Rounded.CardGiftcard,
                        label = "Daily Gift",
                        accent = GoldCoin,
                        onClick = { viewModel.claimDailyReward() },
                        modifier = Modifier.weight(1f).testTag("quick_daily_gift")
                    )
                    QuickActionTile(
                        icon = Icons.Rounded.HeadsetMic,
                        label = "Party",
                        accent = CyanNeon,
                        onClick = onNavigatePlay,
                        modifier = Modifier.weight(1f).testTag("quick_party")
                    )
                }
            }

            // Feed Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Community Feed", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    LevelBadge(level = userPrivate?.level ?: 1L)
                }
            }

            when (postsState) {
                is UiState.Loading -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = CyanNeon)
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        EmptyArcadeState(
                            icon = Icons.Rounded.ErrorOutline,
                            title = "Feed unavailable",
                            subtitle = postsState.message
                        )
                    }
                }
                is UiState.Success -> {
                    if (feedPosts.isEmpty()) {
                        item {
                            EmptyArcadeState(
                                icon = Icons.Rounded.DynamicFeed,
                                title = "No posts yet",
                                subtitle = "Share a screenshot, highlight, or game update to kick off the arcade feed.",
                                actionLabel = "Create First Post",
                                onAction = {
                                    initialCreateType = "POST"
                                    showCreateDialog = true
                                }
                            )
                        }
                    } else {
                        items(feedPosts, key = { it.id }) { post ->
                            PostCard(
                                post = post,
                                isAuthor = post.authorId == viewModel.currentUserId,
                                onLike = { viewModel.toggleLike(post) },
                                onComment = { viewModel.openPostComments(post) },
                                onShare = {
                                    viewModel.sharePost(post)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "${post.authorName} on Mobilive Arcade (${post.gameTag}): ${post.caption}"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Arcade Post"))
                                },
                                onDelete = { viewModel.deletePost(post.id) },
                                onReport = { reportingTarget = post.authorId to post.authorHandle },
                                onBlock = { viewModel.toggleBlockUser(post.authorId, post.authorHandle) }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showCreateDialog) {
        CreatePostDialog(
            initialType = initialCreateType,
            onDismiss = { showCreateDialog = false },
            onSubmit = { type, game, caption, uri ->
                viewModel.createPost(type, game, caption, uri)
                showCreateDialog = false
            }
        )
    }

    if (viewingStory != null) {
        val story = viewingStory!!
        AlertDialog(
            onDismissRequest = { viewingStory = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AvatarFace(avatarStyle = story.authorAvatar, size = 38.dp, isLive = true)
                    Column {
                        Text(story.authorName, style = MaterialTheme.typography.titleMedium)
                        Text(story.gameTag, style = MaterialTheme.typography.labelSmall, color = CyanNeon)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (story.mediaUri.isNotBlank()) {
                        AsyncImage(
                            model = story.mediaUri,
                            contentDescription = "Story media",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Text(story.caption, style = MaterialTheme.typography.bodyLarge)
                }
            },
            confirmButton = {
                if (story.authorId == viewModel.currentUserId) {
                    TextButton(onClick = {
                        viewModel.deletePost(story.id)
                        viewingStory = null
                    }) {
                        Text("Delete Story", color = CoralLive)
                    }
                }
                Button(onClick = { viewingStory = null }) { Text("Close") }
            }
        )
    }

    if (showSearchSheet) {
        val searchProfiles = remember(profilesState, blockedIds) {
            (profilesState as? UiState.Success)?.data.orEmpty().filter { it.userId !in blockedIds }
        }
        val searchWorlds = remember(worldsState) {
            (worldsState as? UiState.Success)?.data.orEmpty()
        }
        SearchModalSheet(
            profiles = searchProfiles,
            streams = liveStreams,
            worlds = searchWorlds,
            posts = posts,
            onDismiss = { showSearchSheet = false },
            onSelectStream = { streamId ->
                showSearchSheet = false
                onOpenStream(streamId)
            },
            onInspectProfile = { target ->
                showSearchSheet = false
                selectedUserProfile = target
            },
            onSendFriendRequest = { target -> viewModel.sendFriendRequest(target) },
            onFollow = { target -> viewModel.toggleFollow(target) }
        )
    }

    if (showNotificationsSheet) {
        NotificationsModalSheet(
            notifications = notifications,
            onDismiss = { showNotificationsSheet = false },
            onMarkRead = { id -> viewModel.markNotificationRead(id) }
        )
    }

    if (reportingTarget != null) {
        ReportUserDialog(
            targetHandle = reportingTarget!!.second,
            onDismiss = { reportingTarget = null },
            onSubmitReport = { reason, details ->
                viewModel.submitReport(reportingTarget!!.first, reportingTarget!!.second, reason, details)
                reportingTarget = null
            }
        )
    }

    if (selectedUserProfile != null) {
        val target = selectedUserProfile!!
        val followingIds = remember(userPrivate) { userPrivate?.followingUserIds.orEmpty().toSet() }
        UserProfileBottomSheet(
            profile = target,
            isOwnProfile = target.userId == viewModel.currentUserId,
            isFollowing = target.userId in followingIds,
            onDismiss = { selectedUserProfile = null },
            onToggleFollow = { viewModel.toggleFollow(target) },
            onMessageOrAdd = {
                selectedUserProfile = null
                viewModel.sendFriendRequest(target)
            },
            actionLabel = "Add Friend"
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
private fun HeroArcadeBanner(
    liveStreams: List<LiveStream>,
    onGoLive: () -> Unit,
    onOpenStream: (String) -> Unit
) {
    val featured = liveStreams.firstOrNull()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable {
                if (featured != null) onOpenStream(featured.id) else onGoLive()
            },
        shape = RoundedCornerShape(22.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.45f))
    ) {
        Box(modifier = Modifier.height(178.dp)) {
            AsyncImage(
                model = if (featured != null) R.drawable.img_stream_voxel else R.drawable.img_hero_arcade,
                contentDescription = "Featured live arcade stream",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                ObsidianBg.copy(alpha = 0.2f),
                                ObsidianBg.copy(alpha = 0.92f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (featured != null) {
                        LivePulseTag("LIVE • ${featured.viewerCount} watching")
                    } else {
                        LivePulseTag("ARCADE ARENA")
                    }
                    Surface(shape = RoundedCornerShape(50), color = ObsidianBg.copy(alpha = 0.75f)) {
                        Text(
                            text = featured?.gameTitle ?: "Minecraft • Co-Op",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanNeon,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = featured?.title ?: "Start Your Live Stream or LAN Party",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = featured?.let { "Hosted by ${it.hostName}" }
                                ?: "Broadcast gameplay, chat with fans & earn Gift Coins",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            if (featured != null) onOpenStream(featured.id) else onGoLive()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = ObsidianBg),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(if (featured != null) "Watch" else "Go Live", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = accent, modifier = Modifier.size(22.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextPrimary, maxLines = 1)
        }
    }
}

@Composable
fun PostCard(
    post: SocialPost,
    isAuthor: Boolean,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ObsidianOutline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    AvatarFace(
                        avatarStyle = post.authorAvatar,
                        size = 42.dp,
                        showVerified = post.authorVerified
                    )
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = post.authorName,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (post.authorVerified) {
                                Icon(
                                    Icons.Rounded.Verified,
                                    contentDescription = "Verified",
                                    tint = CyanNeon,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        Text(
                            text = "@${post.authorHandle} • ${post.gameTag}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.testTag("post_more_button_${post.id}")
                    ) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "More options", tint = TextSecondary)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        if (isAuthor) {
                            DropdownMenuItem(
                                text = { Text("Delete Post", color = CoralLive) },
                                leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, tint = CoralLive) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Report User") },
                                leadingIcon = { Icon(Icons.Rounded.Flag, null) },
                                onClick = {
                                    menuExpanded = false
                                    onReport()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Block User", color = CoralLive) },
                                leadingIcon = { Icon(Icons.Rounded.Block, null, tint = CoralLive) },
                                onClick = {
                                    menuExpanded = false
                                    onBlock()
                                }
                            )
                        }
                    }
                }
            }

            Text(
                text = post.caption,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )

            if (post.mediaUri.isNotBlank()) {
                AsyncImage(
                    model = post.mediaUri,
                    contentDescription = "Post media",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            HorizontalDivider(color = ObsidianOutline.copy(alpha = 0.6f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onLike, modifier = Modifier.testTag("like_button_${post.id}")) {
                    Icon(Icons.Rounded.FavoriteBorder, contentDescription = "Like", tint = CoralLive, modifier = Modifier.size(19.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${post.likesCount}", color = TextPrimary)
                }
                TextButton(onClick = onComment, modifier = Modifier.testTag("comment_button_${post.id}")) {
                    Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = "Comment", tint = CyanNeon, modifier = Modifier.size(19.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${post.commentsCount}", color = TextPrimary)
                }
                TextButton(onClick = onShare, modifier = Modifier.testTag("share_button_${post.id}")) {
                    Icon(Icons.Rounded.Share, contentDescription = "Share", tint = VoltGreen, modifier = Modifier.size(19.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${post.sharesCount}", color = TextPrimary)
                }
            }
        }
    }
}

@Composable
fun CreatePostDialog(
    initialType: String,
    onDismiss: () -> Unit,
    onSubmit: (postType: String, gameTag: String, caption: String, mediaUri: String) -> Unit
) {
    val types = listOf("POST", "STORY", "SHORT")
    var selectedType by remember { mutableStateOf(initialType) }
    var selectedGame by remember { mutableStateOf(ArcadeCatalog.popularGames.first()) }
    var caption by remember { mutableStateOf("") }
    var mediaUri by remember { mutableStateOf("") }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) mediaUri = uri.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Publish to Mobilive Arcade") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    types.forEachIndexed { idx, t ->
                        SegmentedButton(
                            selected = selectedType == t,
                            onClick = { selectedType = t },
                            shape = SegmentedButtonDefaults.itemShape(idx, types.size)
                        ) {
                            Text(t, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ArcadeCatalog.popularGames, key = { it }) { game ->
                        FilterChip(
                            selected = selectedGame == game,
                            onClick = { selectedGame = game },
                            label = { Text(game, maxLines = 1) }
                        )
                    }
                }
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it.take(500) },
                    label = { Text("What's happening in-game?") },
                    modifier = Modifier.fillMaxWidth().testTag("create_post_caption"),
                    minLines = 3
                )
                OutlinedButton(
                    onClick = {
                        mediaPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (mediaUri.isBlank()) "Attach Screenshot or Clip" else "Media Selected")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedType, selectedGame, caption, mediaUri) },
                enabled = caption.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = ObsidianBg),
                modifier = Modifier.testTag("publish_post_button")
            ) {
                Text("Publish", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchModalSheet(
    profiles: List<UserProfile>,
    streams: List<LiveStream>,
    worlds: List<MultiplayerWorld>,
    posts: List<SocialPost>,
    onDismiss: () -> Unit,
    onSelectStream: (String) -> Unit,
    onInspectProfile: (UserProfile) -> Unit,
    onSendFriendRequest: (UserProfile) -> Unit,
    onFollow: (UserProfile) -> Unit
) {
    var query by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = ObsidianSurface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                placeholder = { Text("Search players, live streams, LAN worlds, posts...") },
                modifier = Modifier.fillMaxWidth().testTag("search_input"),
                singleLine = true
            )
            val q = remember(query) { query.trim().lowercase() }
            val matchedStreams = remember(q, streams) {
                streams.filter { q.isEmpty() || it.title.lowercase().contains(q) || it.gameTitle.lowercase().contains(q) }
            }
            val matchedProfiles = remember(q, profiles) {
                profiles.filter { q.isEmpty() || it.displayName.lowercase().contains(q) || it.handle.lowercase().contains(q) }
            }
            val matchedWorlds = remember(q, worlds) {
                worlds.filter { q.isEmpty() || it.worldName.lowercase().contains(q) }
            }
            val matchedPosts = remember(q, posts) {
                posts.filter { q.isNotEmpty() && it.caption.lowercase().contains(q) }
            }

            LazyColumn(
                modifier = Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (matchedStreams.isNotEmpty()) {
                    item { Text("Live Streams", style = MaterialTheme.typography.labelMedium, color = CoralLive) }
                    items(matchedStreams, key = { "s_${it.id}" }) { s ->
                        ListItem(
                            headlineContent = { Text(s.title) },
                            supportingContent = { Text("${s.hostName} • ${s.gameTitle}") },
                            trailingContent = { TextButton(onClick = { onSelectStream(s.id) }) { Text("Watch") } }
                        )
                    }
                }
                if (matchedWorlds.isNotEmpty()) {
                    item { Text("LAN Worlds", style = MaterialTheme.typography.labelMedium, color = VoltGreen) }
                    items(matchedWorlds, key = { "w_${it.id}" }) { w ->
                        ListItem(
                            headlineContent = { Text(w.worldName) },
                            supportingContent = { Text("Host: ${w.hostName} • ${w.gameMode}") }
                        )
                    }
                }
                if (matchedProfiles.isNotEmpty()) {
                    item { Text("Players", style = MaterialTheme.typography.labelMedium, color = CyanNeon) }
                    items(matchedProfiles, key = { "p_${it.userId}" }) { p ->
                        ListItem(
                            modifier = Modifier.clickable { onInspectProfile(p) },
                            leadingContent = { AvatarFace(avatarStyle = p.avatarStyle, photoUri = p.avatarPhotoUri, size = 36.dp) },
                            headlineContent = { Text(p.displayName) },
                            supportingContent = { Text("@${p.handle} • LV ${p.level}") },
                            trailingContent = {
                                Row {
                                    TextButton(onClick = { onFollow(p) }) { Text("Follow") }
                                    TextButton(onClick = { onSendFriendRequest(p) }) { Text("Add") }
                                }
                            }
                        )
                    }
                }
                if (matchedPosts.isNotEmpty()) {
                    item { Text("Posts", style = MaterialTheme.typography.labelMedium, color = GoldCoin) }
                    items(matchedPosts, key = { "post_${it.id}" }) { post ->
                        ListItem(
                            headlineContent = { Text(post.caption, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text("@${post.authorHandle} • ${post.gameTag}") }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsModalSheet(
    notifications: List<UserNotification>,
    onDismiss: () -> Unit,
    onMarkRead: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = ObsidianSurface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Notifications", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            if (notifications.isEmpty()) {
                EmptyArcadeState(
                    icon = Icons.Rounded.NotificationsNone,
                    title = "All caught up",
                    subtitle = "Stream alerts, friend requests, and gift coin rewards appear here."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notifications, key = { it.id }) { notif ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (notif.readFlag) ObsidianSurfaceElevated else CyanContainer.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMarkRead(notif.id) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(notif.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text(notif.body, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}
