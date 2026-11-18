package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.screens.GoLiveSetupDialog
import com.example.ui.screens.LiveElapsedBadge
import com.example.ui.screens.StreamManagerDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.ArcadeViewModel
import kotlin.math.roundToInt

@Composable
fun FloatingArcadeOverlay(
    viewModel: ArcadeViewModel,
    selectedStreamId: String?,
    minimizedStreamId: String?,
    hideBubble: Boolean,
    bottomBarInsetDp: androidx.compose.ui.unit.Dp,
    onExpandStream: (String) -> Unit,
    onMinimizeStream: (String?) -> Unit,
    onOpenConversationInSocial: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val streamsState by viewModel.streams.collectAsStateWithLifecycle()
    val allStreams = remember(streamsState) {
        (streamsState as? UiState.Success)?.data.orEmpty()
    }
    val myActiveStream = remember(allStreams, viewModel.currentUserId) {
        allStreams.firstOrNull { it.streamStatus == "live" && it.hostId == viewModel.currentUserId }
    }
    val floatingStream = remember(allStreams, selectedStreamId, minimizedStreamId, myActiveStream) {
        when {
            selectedStreamId != null -> null
            minimizedStreamId != null -> allStreams.firstOrNull { it.id == minimizedStreamId && it.streamStatus != "ended" }
            myActiveStream != null -> myActiveStream
            else -> null
        }
    }

    var menuExpanded by remember { mutableStateOf(false) }
    var showQuickMessageSheet by remember { mutableStateOf(false) }
    var showGoLiveDialog by remember { mutableStateOf(false) }
    var showStreamManager by remember { mutableStateOf(false) }

    if (menuExpanded && !hideBubble) {
        BackHandler { menuExpanded = false }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val density = LocalDensity.current
        val bubbleSizeDp = 56.dp
        val edgeMarginPx = with(density) { 16.dp.toPx() }
        val bottomReservedPx = with(density) { (bottomBarInsetDp + if (floatingStream != null) 96.dp else 18.dp).toPx() }
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val bubbleSizePx = with(density) { bubbleSizeDp.toPx() }

        val minX = edgeMarginPx
        val maxX = (maxWidthPx - bubbleSizePx - edgeMarginPx).coerceAtLeast(minX)
        val minY = edgeMarginPx
        val maxY = (maxHeightPx - bubbleSizePx - bottomReservedPx).coerceAtLeast(minY)

        var offsetX by remember(maxX) { mutableFloatStateOf(maxX) }
        var offsetY by remember(maxY) { mutableFloatStateOf((maxY - with(density) { 72.dp.toPx() }).coerceIn(minY, maxY)) }

        LaunchedEffect(maxX, maxY) {
            offsetX = offsetX.coerceIn(minX, maxX)
            offsetY = offsetY.coerceIn(minY, maxY)
        }

        // Floating Live Stream Mini-Player docked cleanly above bottom bar
        if (floatingStream != null) {
            FloatingLiveStreamMiniPlayer(
                stream = floatingStream,
                isHost = floatingStream.hostId == viewModel.currentUserId,
                onExpand = {
                    onMinimizeStream(null)
                    onExpandStream(floatingStream.id)
                },
                onManage = { showStreamManager = true },
                onClose = { onMinimizeStream(null) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = bottomBarInsetDp + 10.dp)
            )
        }

        if (!hideBubble) {
            // Expanded floating actions anchored cleanly near bottom-end
            AnimatedVisibility(
                visible = menuExpanded,
                enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                    expandVertically(spring(stiffness = Spring.StiffnessMediumLow)),
                exit = fadeOut(spring(stiffness = Spring.StiffnessMedium)) +
                    shrinkVertically(spring(stiffness = Spring.StiffnessMedium)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = bottomBarInsetDp + (if (floatingStream != null) 168.dp else 88.dp))
            ) {
                FloatingActionMenuPanel(
                    isCurrentlyLive = myActiveStream != null,
                    onMessageClick = {
                        menuExpanded = false
                        showQuickMessageSheet = true
                    },
                    onGoLiveClick = {
                        menuExpanded = false
                        if (myActiveStream != null) {
                            showStreamManager = true
                        } else {
                            showGoLiveDialog = true
                        }
                    },
                    onCollapse = { menuExpanded = false }
                )
            }

            // Draggable Floating Arcade Bubble
            Surface(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .size(bubbleSizeDp)
                    .shadow(10.dp, CircleShape)
                    .pointerInput(minX, maxX, minY, maxY) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount.x).coerceIn(minX, maxX)
                            offsetY = (offsetY + dragAmount.y).coerceIn(minY, maxY)
                        }
                    }
                    .clip(CircleShape)
                    .clickable { menuExpanded = !menuExpanded }
                    .testTag("floating_hub_bubble"),
                shape = CircleShape,
                color = if (myActiveStream != null) CoralLive else ObsidianSurfaceElevated,
                border = BorderStroke(
                    2.dp,
                    if (myActiveStream != null) GoldCoin else CyanNeon
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when {
                            menuExpanded -> Icons.Rounded.Close
                            myActiveStream != null -> Icons.Rounded.Sensors
                            else -> Icons.Rounded.AutoAwesomeMotion
                        },
                        contentDescription = if (menuExpanded) "Close floating actions" else "Open floating message and Go Live menu",
                        tint = if (myActiveStream != null) Color.White else CyanNeon,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }

    if (showQuickMessageSheet) {
        FloatingQuickMessageSheet(
            viewModel = viewModel,
            onDismiss = { showQuickMessageSheet = false },
            onOpenFullSocialChat = { convId ->
                showQuickMessageSheet = false
                onOpenConversationInSocial(convId)
            }
        )
    }

    if (showGoLiveDialog) {
        GoLiveSetupDialog(
            onDismiss = { showGoLiveDialog = false },
            onStartLive = { title, game, recordReplay ->
                viewModel.startLiveStream(title, game, recordReplay) { newId ->
                    showGoLiveDialog = false
                    onMinimizeStream(newId)
                }
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
                onMinimizeStream(null)
            }
        )
    }
}

@Composable
internal fun FloatingActionMenuPanel(
    isCurrentlyLive: Boolean,
    onMessageClick: () -> Unit,
    onGoLiveClick: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .widthIn(min = 210.dp, max = 260.dp)
            .testTag("floating_action_menu"),
        shape = RoundedCornerShape(22.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.5.dp, CyanNeon.copy(alpha = 0.55f)),
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Floating Hub",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }

            Button(
                onClick = onMessageClick,
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = ObsidianBg),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("floating_message_button")
            ) {
                Icon(Icons.Rounded.Forum, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Message", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onGoLiveClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCurrentlyLive) GoldCoin else CoralLive,
                    contentColor = if (isCurrentlyLive) ObsidianBg else Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("floating_go_live_button")
            ) {
                Icon(
                    imageVector = if (isCurrentlyLive) Icons.Rounded.Tune else Icons.Rounded.Sensors,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCurrentlyLive) "Live Controls" else "Go Live Stream",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
internal fun FloatingLiveStreamMiniPlayer(
    stream: LiveStream,
    isHost: Boolean,
    onExpand: () -> Unit,
    onManage: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onExpand)
            .testTag("floating_live_mini_player"),
        shape = RoundedCornerShape(18.dp),
        color = ObsidianSurfaceElevated,
        border = BorderStroke(1.5.dp, if (stream.streamStatus == "live") CoralLive else CyanNeon),
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AvatarFace(
                avatarStyle = stream.hostAvatar,
                size = 42.dp,
                isLive = stream.streamStatus == "live"
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LiveElapsedBadge(
                        streamId = stream.id,
                        streamStatus = stream.streamStatus,
                        initialDurationSec = stream.durationSec
                    )
                    Text(
                        text = stream.gameTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanNeon,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = stream.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${stream.hostName} • ${stream.viewerCount} viewers • ${stream.totalGiftsCoins} Coins",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isHost && stream.streamStatus == "live") {
                IconButton(
                    onClick = onManage,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("floating_stream_manage_button")
                ) {
                    Icon(Icons.Rounded.Tune, contentDescription = "Stream controls", tint = GoldCoin)
                }
            }
            IconButton(
                onClick = onExpand,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("floating_stream_expand_button")
            ) {
                Icon(Icons.Rounded.OpenInFull, contentDescription = "Expand stream", tint = CyanNeon)
            }
            if (!isHost) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("floating_stream_close_button")
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Dismiss mini player", tint = TextMuted)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FloatingQuickMessageSheet(
    viewModel: ArcadeViewModel,
    onDismiss: () -> Unit,
    onOpenFullSocialChat: (String) -> Unit
) {
    val conversationsState by viewModel.conversations.collectAsStateWithLifecycle()
    val profilesState by viewModel.allProfiles.collectAsStateWithLifecycle()
    val selectedMessagesState by viewModel.selectedMessages.collectAsStateWithLifecycle()

    val conversations = remember(conversationsState) {
        (conversationsState as? UiState.Success)?.data.orEmpty()
    }
    val profiles = remember(profilesState, viewModel.currentUserId) {
        (profilesState as? UiState.Success)?.data.orEmpty().filter { it.userId != viewModel.currentUserId }
    }
    val messages = remember(selectedMessagesState) {
        (selectedMessagesState as? UiState.Success)?.data.orEmpty()
    }

    var activeConvId by remember { mutableStateOf<String?>(null) }
    var draftMessage by remember { mutableStateOf("") }

    val activeConversation = remember(conversations, activeConvId) {
        conversations.firstOrNull { it.id == activeConvId }
    }

    LaunchedEffect(activeConvId) {
        if (activeConvId != null) {
            viewModel.selectConversation(activeConvId)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .testTag("floating_message_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (activeConversation != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { activeConvId = null }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to chats", tint = TextPrimary)
                        }
                        Column {
                            Text(activeConversation.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("Floating Quick Chat", style = MaterialTheme.typography.labelSmall, color = CyanNeon)
                        }
                    }
                    TextButton(onClick = { onOpenFullSocialChat(activeConversation.id) }) {
                        Text("Full View", color = CyanNeon)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Text(
                                text = "Say hi to start the conversation!",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        items(messages, key = { it.id }) { msg ->
                            val isMe = msg.senderId == viewModel.currentUserId
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                            ) {
                                if (!isMe) {
                                    Text(msg.senderName, style = MaterialTheme.typography.labelSmall, color = CyanNeon)
                                }
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isMe) CyanContainer else ObsidianSurfaceElevated
                                ) {
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = draftMessage,
                        onValueChange = { draftMessage = it.take(500) },
                        placeholder = { Text("Quick message...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("floating_chat_input"),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            if (draftMessage.isNotBlank()) {
                                viewModel.sendMessage(activeConversation, draftMessage)
                                draftMessage = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(CyanNeon)
                            .testTag("floating_chat_send")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send message", tint = ObsidianBg)
                    }
                }
            } else {
                Text(
                    text = "Floating Messages",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                if (profiles.isNotEmpty()) {
                    Text("Quick Message a Player", style = MaterialTheme.typography.labelMedium, color = CyanNeon)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(profiles.take(10), key = { it.userId }) { player ->
                            AssistChip(
                                onClick = {
                                    viewModel.createConversation(
                                        title = player.displayName,
                                        groupChat = false,
                                        participants = listOf(player)
                                    ) { newConvId ->
                                        activeConvId = newConvId
                                    }
                                },
                                leadingIcon = {
                                    AvatarFace(avatarStyle = player.avatarStyle, photoUri = player.avatarPhotoUri, size = 24.dp)
                                },
                                label = { Text(player.displayName, maxLines = 1) }
                            )
                        }
                    }
                }

                if (conversations.isEmpty()) {
                    EmptyArcadeState(
                        icon = Icons.Rounded.Forum,
                        title = "No active chats yet",
                        subtitle = "Tap a player above to start a floating DM without leaving your screen."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(conversations, key = { it.id }) { conv ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { activeConvId = conv.id },
                                shape = RoundedCornerShape(14.dp),
                                color = ObsidianSurfaceElevated,
                                border = BorderStroke(1.dp, ObsidianOutline)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (conv.groupChat) Icons.Rounded.Groups else Icons.Rounded.Chat,
                                        contentDescription = null,
                                        tint = CyanNeon
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(conv.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                        Text(
                                            text = conv.lastMessage.ifBlank { "Tap to message" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = TextMuted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
