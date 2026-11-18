package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ArcadeViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchScreen(
    viewModel: ArcadeViewModel,
    userPrivate: UserPrivate?,
    streamsState: UiState<List<LiveStream>>,
    postsState: UiState<List<SocialPost>>,
    selectedStreamId: String?,
    onSelectStream: (String?) -> Unit,
    onFloatStream: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val blockedIds = remember(userPrivate) { userPrivate?.blockedUserIds.orEmpty().toSet() }
    val allStreams = remember(streamsState, blockedIds) {
        (streamsState as? UiState.Success)?.data.orEmpty().filter { it.hostId !in blockedIds }
    }
    val liveStreams = remember(allStreams) { allStreams.filter { it.streamStatus == "live" } }
    val replayStreams = remember(allStreams) { allStreams.filter { it.streamStatus == "replay" } }
    val shorts = remember(postsState, blockedIds) {
        (postsState as? UiState.Success)?.data.orEmpty()
            .filter { it.postType == "SHORT" && it.authorId !in blockedIds }
    }

    val activeStream = remember(allStreams, selectedStreamId) {
        allStreams.firstOrNull { it.id == selectedStreamId }
    }
    val myActiveStream = remember(liveStreams, viewModel.currentUserId) {
        liveStreams.firstOrNull { it.hostId == viewModel.currentUserId }
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0=Live, 1=Shorts, 2=Replays
    var showGoLiveDialog by remember { mutableStateOf(false) }
    var showCreateShortDialog by remember { mutableStateOf(false) }
    var showStreamManager by remember { mutableStateOf(false) }
    var reportingTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(selectedStreamId) {
        viewModel.selectStream(selectedStreamId)
    }

    if (activeStream != null) {
        StreamPlayerView(
            stream = activeStream,
            viewModel = viewModel,
            userPrivate = userPrivate,
            isHost = activeStream.hostId == viewModel.currentUserId,
            onBack = { onSelectStream(null) },
            onFloat = { onFloatStream(activeStream.id) },
            onOpenManager = { showStreamManager = true }
        )
    } else {
        Scaffold(
            containerColor = ObsidianBg,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBg),
                    title = { Text("Watch & Stream", style = MaterialTheme.typography.titleLarge) },
                    actions = {
                        if (myActiveStream != null) {
                            FilledTonalButton(
                                onClick = { showStreamManager = true },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = CoralContainer,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.testTag("open_stream_manager_button")
                            ) {
                                Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Manager")
                            }
                        } else {
                            Button(
                                onClick = { showGoLiveDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CoralLive, contentColor = Color.White),
                                modifier = Modifier.testTag("go_live_button")
                            ) {
                                Icon(Icons.Rounded.Sensors, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Go Live", fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf("Live (${liveStreams.size})", "Shorts (${shorts.size})", "Replays (${replayStreams.size})")
                        .forEachIndexed { idx, label ->
                            SegmentedButton(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                shape = SegmentedButtonDefaults.itemShape(idx, 3),
                                modifier = Modifier.testTag("watch_tab_$idx")
                            ) {
                                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                }

                when (selectedTab) {
                    0 -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            if (liveStreams.isEmpty()) {
                                item {
                                    EmptyArcadeState(
                                        icon = Icons.Rounded.LiveTv,
                                        title = "No live streams right now",
                                        subtitle = "Tap Go Live to launch your own broadcast with real-time Stream Chat, Gift Coins, and Replay recording.",
                                        actionLabel = "Go Live Now",
                                        onAction = { showGoLiveDialog = true }
                                    )
                                }
                            } else {
                                items(liveStreams, key = { it.id }) { stream ->
                                    StreamCard(
                                        stream = stream,
                                        isReplay = false,
                                        onClick = { onSelectStream(stream.id) }
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Arcade Shorts", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Button(
                                        onClick = { showCreateShortDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = ObsidianBg),
                                        modifier = Modifier.testTag("create_short_button")
                                    ) {
                                        Icon(Icons.Rounded.VideoCall, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Post Short", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            if (shorts.isEmpty()) {
                                item {
                                    EmptyArcadeState(
                                        icon = Icons.Rounded.MovieFilter,
                                        title = "No Shorts yet",
                                        subtitle = "Post vertical arcade highlights, clutch moments, and trick shots!",
                                        actionLabel = "Create Short",
                                        onAction = { showCreateShortDialog = true }
                                    )
                                }
                            } else {
                                items(shorts, key = { it.id }) { shortPost ->
                                    ShortReelCard(
                                        shortPost = shortPost,
                                        isAuthor = shortPost.authorId == viewModel.currentUserId,
                                        onLike = { viewModel.toggleLike(shortPost) },
                                        onComment = { viewModel.openPostComments(shortPost) },
                                        onShare = {
                                            viewModel.sharePost(shortPost)
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, "Watch ${shortPost.authorName}'s Short on Mobilive Arcade: ${shortPost.caption}")
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Short"))
                                        },
                                        onDelete = { viewModel.deletePost(shortPost.id) },
                                        onReport = { reportingTarget = shortPost.authorId to shortPost.authorHandle }
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            if (replayStreams.isEmpty()) {
                                item {
                                    EmptyArcadeState(
                                        icon = Icons.Rounded.ReplayCircleFilled,
                                        title = "No Stream Replays saved",
                                        subtitle = "Enable 'Record Replay' when going live. Once you end your stream, it appears here for replay!"
                                    )
                                }
                            } else {
                                items(replayStreams, key = { it.id }) { stream ->
                                    StreamCard(
                                        stream = stream,
                                        isReplay = true,
                                        onClick = { onSelectStream(stream.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGoLiveDialog) {
        GoLiveSetupDialog(
            onDismiss = { showGoLiveDialog = false },
            onStartLive = { title, game, recordReplay ->
                viewModel.startLiveStream(title, game, recordReplay) { newId ->
                    showGoLiveDialog = false
                    onSelectStream(newId)
                }
            }
        )
    }

    if (showCreateShortDialog) {
        CreatePostDialog(
            initialType = "SHORT",
            onDismiss = { showCreateShortDialog = false },
            onSubmit = { type, game, caption, uri ->
                viewModel.createPost(type, game, caption, uri)
                showCreateShortDialog = false
            }
        )
    }

    if (showStreamManager && myActiveStream != null) {
        StreamManagerDialog(
            stream = myActiveStream,
            onDismiss = { showStreamManager = false },
            onSaveControls = { mic, slow, replay, title ->
                viewModel.updateStreamControls(myActiveStream, mic, slow, replay, title)
            },
            onEndStream = { elapsed ->
                viewModel.endLiveStream(myActiveStream, elapsed)
                showStreamManager = false
                onSelectStream(null)
            }
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
}

@Composable
private fun StreamCard(
    stream: LiveStream,
    isReplay: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, if (isReplay) ObsidianOutline else CoralLive.copy(alpha = 0.5f))
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(165.dp)) {
                AsyncImage(
                    model = R.drawable.img_stream_voxel,
                    contentDescription = stream.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.85f))
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (isReplay) {
                        Surface(shape = RoundedCornerShape(6.dp), color = VioletArcade) {
                            Text(
                                "REPLAY • ${stream.durationSec}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    } else {
                        LivePulseTag("LIVE • ${stream.viewerCount} viewers")
                    }
                    Surface(shape = RoundedCornerShape(50), color = ObsidianBg.copy(alpha = 0.8f)) {
                        Text(
                            stream.gameTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanNeon,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AvatarFace(avatarStyle = stream.hostAvatar, size = 42.dp, isLive = !isReplay)
                Column(modifier = Modifier.weight(1f)) {
                    Text(stream.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${stream.hostName} • ${stream.totalGiftsCoins} Gift Coins", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Icon(Icons.Rounded.PlayCircleFilled, contentDescription = "Watch", tint = CyanNeon, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
private fun ShortReelCard(
    shortPost: SocialPost,
    isAuthor: Boolean,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.35f))
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(340.dp)) {
            if (shortPost.mediaUri.isNotBlank()) {
                AsyncImage(
                    model = shortPost.mediaUri,
                    contentDescription = shortPost.caption,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                AsyncImage(
                    model = R.drawable.img_hero_arcade,
                    contentDescription = shortPost.caption,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.94f))))
            )
            // Right Action Rail
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(onClick = onLike, modifier = Modifier.background(ObsidianBg.copy(alpha = 0.65f), CircleShape)) {
                    Icon(Icons.Rounded.Favorite, contentDescription = "Like Short", tint = CoralLive)
                }
                Text("${shortPost.likesCount}", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                IconButton(onClick = onComment, modifier = Modifier.background(ObsidianBg.copy(alpha = 0.65f), CircleShape)) {
                    Icon(Icons.Rounded.ChatBubble, contentDescription = "Comments", tint = CyanNeon)
                }
                Text("${shortPost.commentsCount}", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                IconButton(onClick = onShare, modifier = Modifier.background(ObsidianBg.copy(alpha = 0.65f), CircleShape)) {
                    Icon(Icons.Rounded.Share, contentDescription = "Share Short", tint = VoltGreen)
                }
                Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.background(ObsidianBg.copy(alpha = 0.65f), CircleShape)) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = TextPrimary)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (isAuthor) {
                            DropdownMenuItem(
                                text = { Text("Delete Short", color = CoralLive) },
                                onClick = { menuOpen = false; onDelete() }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Report Creator") },
                                onClick = { menuOpen = false; onReport() }
                            )
                        }
                    }
                }
            }
            // Bottom Info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .padding(end = 68.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(shape = RoundedCornerShape(50), color = CoralLive) {
                    Text("SHORT • ${shortPost.gameTag}", style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                }
                Text("@${shortPost.authorHandle}", style = MaterialTheme.typography.titleMedium, color = CyanNeon)
                Text(shortPost.caption, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, maxLines = 3)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StreamPlayerView(
    stream: LiveStream,
    viewModel: ArcadeViewModel,
    userPrivate: UserPrivate?,
    isHost: Boolean,
    onBack: () -> Unit,
    onFloat: () -> Unit,
    onOpenManager: () -> Unit
) {
    val chatState by viewModel.selectedStreamChat.collectAsStateWithLifecycle()
    val messages = (chatState as? UiState.Success)?.data.orEmpty()
    var chatInput by remember { mutableStateOf("") }

    Scaffold(
        containerColor = ObsidianBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBg),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                title = {
                    Column {
                        Text(stream.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${stream.hostName} • ${stream.gameTitle}", style = MaterialTheme.typography.labelSmall, color = CyanNeon)
                    }
                },
                actions = {
                    IconButton(
                        onClick = onFloat,
                        modifier = Modifier.testTag("float_stream_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PictureInPictureAlt,
                            contentDescription = "Float stream",
                            tint = CyanNeon
                        )
                    }
                    if (isHost && stream.streamStatus == "live") {
                        FilledTonalButton(onClick = onOpenManager, modifier = Modifier.testTag("stream_manager_btn")) {
                            Text("Stream Manager")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Stream Video Surface
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(ObsidianSurface)
            ) {
                AsyncImage(
                    model = R.drawable.img_stream_voxel,
                    contentDescription = "Stream video feed",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LiveElapsedBadge(
                        streamId = stream.id,
                        streamStatus = stream.streamStatus,
                        initialDurationSec = stream.durationSec
                    )
                    CoinPill(coins = stream.totalGiftsCoins)
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = ObsidianBg.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = "Mic: ${if (stream.micEnabled) "ON" else "MUTED"} • Slow Mode: ${stream.slowModeSec}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Gift Coins Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Gift Coins:", style = MaterialTheme.typography.labelMedium, color = GoldCoin)
                listOf(25L, 100L, 250L).forEach { amt ->
                    AssistChip(
                        onClick = { viewModel.sendGiftCoins(stream, amt) },
                        label = { Text("+$amt", color = GoldCoin) },
                        leadingIcon = { Icon(Icons.Rounded.CardGiftcard, null, tint = GoldCoin, modifier = Modifier.size(16.dp)) },
                        enabled = (userPrivate?.coins ?: 0L) >= amt
                    )
                }
            }

            Text("Stream Chat", style = MaterialTheme.typography.titleSmall, color = TextPrimary)

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isGift = msg.messageType == "gift"
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isGift) GoldContainer else ObsidianSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    msg.authorName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isGift) GoldCoin else CyanNeon
                                )
                                Text(msg.text, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            }
                            if (isHost || msg.authorId == viewModel.currentUserId) {
                                IconButton(onClick = { viewModel.deleteStreamChat(stream.id, msg.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete chat", tint = TextMuted, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = chatInput,
                    onValueChange = { chatInput = it.take(250) },
                    placeholder = { Text("Chat in stream...") },
                    modifier = Modifier.weight(1f).testTag("stream_chat_input"),
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        if (chatInput.isNotBlank()) {
                            viewModel.sendStreamChat(stream.id, chatInput)
                            chatInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CyanNeon)
                        .testTag("stream_chat_send")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = ObsidianBg)
                }
            }
        }
    }
}

@Composable
internal fun GoLiveSetupDialog(
    onDismiss: () -> Unit,
    onStartLive: (title: String, game: String, recordReplay: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("Survival LAN Co-Op & Build!") }
    var selectedGame by remember { mutableStateOf(ArcadeCatalog.popularGames.first()) }
    var recordReplay by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Go Live Setup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(100) },
                    label = { Text("Stream Title") },
                    modifier = Modifier.fillMaxWidth().testTag("stream_title_input")
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ArcadeCatalog.popularGames, key = { it }) { g ->
                        FilterChip(selected = selectedGame == g, onClick = { selectedGame = g }, label = { Text(g) })
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Save Stream Replay automatically")
                    Switch(checked = recordReplay, onCheckedChange = { recordReplay = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onStartLive(title, selectedGame, recordReplay) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CoralLive, contentColor = Color.White),
                modifier = Modifier.testTag("confirm_go_live")
            ) {
                Text("Start Broadcast")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
internal fun StreamManagerDialog(
    stream: LiveStream,
    onDismiss: () -> Unit,
    onSaveControls: (mic: Boolean, slow: Long, replay: Boolean, title: String) -> Unit,
    onEndStream: (elapsedSec: Long) -> Unit
) {
    var title by remember { mutableStateOf(stream.title) }
    var micEnabled by remember { mutableStateOf(stream.micEnabled) }
    var recordReplay by remember { mutableStateOf(stream.recordReplay) }
    var slowMode by remember { mutableLongStateOf(stream.slowModeSec) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stream Manager") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(100) },
                    label = { Text("Stream Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Host Microphone")
                    Switch(checked = micEnabled, onCheckedChange = { micEnabled = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Record Replay")
                    Switch(checked = recordReplay, onCheckedChange = { recordReplay = it })
                }
                Text("Chat Slow Mode: ${slowMode}s", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0L, 5L, 15L, 30L).forEach { s ->
                        FilterChip(selected = slowMode == s, onClick = { slowMode = s }, label = { Text("${s}s") })
                    }
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = { onSaveControls(micEnabled, slowMode, recordReplay, title) }) {
                    Text("Save Controls")
                }
                Button(
                    onClick = { onEndStream(stream.durationSec + 60L) },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralLive, contentColor = Color.White),
                    modifier = Modifier.testTag("end_stream_button")
                ) {
                    Text("End Stream")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
internal fun LiveElapsedBadge(
    streamId: String,
    streamStatus: String,
    initialDurationSec: Long
) {
    var elapsedSec by remember(streamId) { mutableLongStateOf(initialDurationSec) }
    LaunchedEffect(streamId, streamStatus) {
        if (streamStatus == "live") {
            while (true) {
                delay(1000L)
                elapsedSec += 1L
            }
        }
    }
    LivePulseTag(
        if (streamStatus == "live") "LIVE • ${elapsedSec}s" else "REPLAY • ${initialDurationSec}s"
    )
}

