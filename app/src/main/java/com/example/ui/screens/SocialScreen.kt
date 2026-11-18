package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ArcadeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialScreen(
    viewModel: ArcadeViewModel,
    userPrivate: UserPrivate?,
    profilesState: UiState<List<UserProfile>>,
    friendRequestsState: UiState<List<FriendRequest>>,
    conversationsState: UiState<List<Conversation>>,
    communitiesState: UiState<List<GameCommunity>>,
    selectedConversationId: String?,
    onSelectConversation: (String?) -> Unit
) {
    val blockedIds = remember(userPrivate) { userPrivate?.blockedUserIds.orEmpty().toSet() }
    val followingIds = remember(userPrivate) { userPrivate?.followingUserIds.orEmpty().toSet() }
    val allProfiles = remember(profilesState, blockedIds, viewModel.currentUserId) {
        (profilesState as? UiState.Success)?.data.orEmpty()
            .filter { it.userId != viewModel.currentUserId && it.userId !in blockedIds }
    }
    val allRequests = remember(friendRequestsState) {
        (friendRequestsState as? UiState.Success)?.data.orEmpty()
    }
    val pendingIncoming = remember(allRequests, viewModel.currentUserId) {
        allRequests.filter { it.status == "pending" && it.receiverId == viewModel.currentUserId }
    }
    val friendIds = remember(allRequests, viewModel.currentUserId) {
        allRequests.filter { it.status == "accepted" }.map {
            if (it.senderId == viewModel.currentUserId) it.receiverId else it.senderId
        }.toSet()
    }
    val friendsProfiles = remember(allProfiles, friendIds) { allProfiles.filter { it.userId in friendIds } }
    val discoverList = remember(allProfiles, friendIds) { allProfiles.filter { it.userId !in friendIds } }
    val conversations = remember(conversationsState) { (conversationsState as? UiState.Success)?.data.orEmpty() }
    val communities = remember(communitiesState) { (communitiesState as? UiState.Success)?.data.orEmpty() }

    val activeConversation = remember(conversations, selectedConversationId) {
        conversations.firstOrNull { it.id == selectedConversationId }
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0=Messages, 1=Friends & Discover, 2=Communities
    var showNewChatDialog by remember { mutableStateOf(false) }
    var reportingTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedUserProfile by remember { mutableStateOf<UserProfile?>(null) }

    LaunchedEffect(selectedConversationId) {
        viewModel.selectConversation(selectedConversationId)
    }

    if (activeConversation != null) {
        ConversationDetailView(
            conversation = activeConversation,
            viewModel = viewModel,
            onBack = { onSelectConversation(null) }
        )
    } else {
        Scaffold(
            containerColor = ObsidianBg,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBg),
                    title = { Text("Social & Squads", style = MaterialTheme.typography.titleLarge) },
                    actions = {
                        IconButton(
                            onClick = { showNewChatDialog = true },
                            modifier = Modifier.testTag("new_chat_button")
                        ) {
                            Icon(Icons.Rounded.GroupAdd, contentDescription = "New Group or DM", tint = CyanNeon)
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        "Chats (${conversations.size})",
                        "Friends (${friendsProfiles.size})",
                        "Clubs (${communities.size})"
                    ).forEachIndexed { idx, label ->
                        SegmentedButton(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            shape = SegmentedButtonDefaults.itemShape(idx, 3),
                            modifier = Modifier.testTag("social_tab_$idx")
                        ) {
                            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                when (selectedTab) {
                    0 -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            if (conversations.isEmpty()) {
                                item {
                                    EmptyArcadeState(
                                        icon = Icons.Rounded.Forum,
                                        title = "No Messages or Group Chats",
                                        subtitle = "Add arcade friends or start a squad Group Chat to coordinate LAN sessions.",
                                        actionLabel = "Start Group / DM",
                                        onAction = { showNewChatDialog = true }
                                    )
                                }
                            } else {
                                items(conversations, key = { it.id }) { conv ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(18.dp))
                                            .clickable { onSelectConversation(conv.id) },
                                        shape = RoundedCornerShape(18.dp),
                                        color = ObsidianSurface,
                                        border = BorderStroke(1.dp, ObsidianOutline)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(CircleShape)
                                                    .background(if (conv.groupChat) VioletContainer else CyanContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (conv.groupChat) Icons.Rounded.Groups else Icons.Rounded.Chat,
                                                    contentDescription = null,
                                                    tint = if (conv.groupChat) VioletArcade else CyanNeon
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text(conv.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                                    if (conv.groupChat) {
                                                        Surface(shape = RoundedCornerShape(6.dp), color = VioletContainer) {
                                                            Text("GROUP", style = MaterialTheme.typography.labelSmall, color = VioletArcade, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                        }
                                                    }
                                                }
                                                Text(conv.lastMessage, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = TextMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            if (pendingIncoming.isNotEmpty()) {
                                item {
                                    Text("Friend Requests (${pendingIncoming.size})", style = MaterialTheme.typography.titleMedium, color = GoldCoin)
                                }
                                items(pendingIncoming, key = { it.id }) { req ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = ObsidianSurfaceElevated,
                                        border = BorderStroke(1.dp, GoldCoin.copy(alpha = 0.45f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            AvatarFace(avatarStyle = req.senderAvatar, size = 40.dp)
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(req.senderName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                                Text("Wants to be your arcade friend", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                            }
                                            IconButton(onClick = { viewModel.respondFriendRequest(req.id, false) }) {
                                                Icon(Icons.Rounded.Close, contentDescription = "Decline", tint = CoralLive)
                                            }
                                            Button(
                                                onClick = { viewModel.respondFriendRequest(req.id, true) },
                                                colors = ButtonDefaults.buttonColors(containerColor = VoltGreen, contentColor = ObsidianBg)
                                            ) {
                                                Text("Accept", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Text("My Friends (${friendsProfiles.size})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            }
                            if (friendsProfiles.isEmpty()) {
                                item {
                                    Text("No connected friends yet. Send a request to players below!", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                            } else {
                                items(friendsProfiles, key = { "f_${it.userId}" }) { friend ->
                                    PlayerSocialRow(
                                        profile = friend,
                                        isFriend = true,
                                        isFollowing = friend.userId in followingIds,
                                        onViewProfile = { selectedUserProfile = friend },
                                        onFollow = { viewModel.toggleFollow(friend) },
                                        onAddOrMessage = {
                                            viewModel.createConversation(
                                                title = friend.displayName,
                                                groupChat = false,
                                                participants = listOf(friend)
                                            ) { convId -> onSelectConversation(convId) }
                                        },
                                        onReport = { reportingTarget = friend.userId to friend.handle },
                                        onBlock = { viewModel.toggleBlockUser(friend.userId, friend.handle) }
                                    )
                                }
                            }

                            item {
                                Text("Discover Players & Creators", style = MaterialTheme.typography.titleMedium, color = CyanNeon)
                            }
                            if (discoverList.isEmpty()) {
                                item {
                                    EmptyArcadeState(
                                        icon = Icons.Rounded.PersonSearch,
                                        title = "Waiting for more players",
                                        subtitle = "Other Mobilive Arcade players who sign in appear here so you can follow, friend, and chat!"
                                    )
                                }
                            } else {
                                items(discoverList, key = { "d_${it.userId}" }) { player ->
                                    PlayerSocialRow(
                                        profile = player,
                                        isFriend = false,
                                        isFollowing = player.userId in followingIds,
                                        onViewProfile = { selectedUserProfile = player },
                                        onFollow = { viewModel.toggleFollow(player) },
                                        onAddOrMessage = { viewModel.sendFriendRequest(player) },
                                        onReport = { reportingTarget = player.userId to player.handle },
                                        onBlock = { viewModel.toggleBlockUser(player.userId, player.handle) }
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            if (communities.isEmpty()) {
                                item {
                                    EmptyArcadeState(
                                        icon = Icons.Rounded.Groups,
                                        title = "No Game Communities yet",
                                        subtitle = "Visit the Play tab to create your first Game Community."
                                    )
                                }
                            } else {
                                items(communities, key = { it.id }) { comm ->
                                    CommunityCard(
                                        community = comm,
                                        isMember = comm.memberIds.contains(viewModel.currentUserId),
                                        onToggleJoin = { viewModel.toggleJoinCommunity(comm) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewChatDialog) {
        NewConversationDialog(
            availableUsers = allProfiles,
            onDismiss = { showNewChatDialog = false },
            onCreate = { title, isGroup, selected ->
                viewModel.createConversation(title, isGroup, selected) { convId ->
                    showNewChatDialog = false
                    onSelectConversation(convId)
                }
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

    if (selectedUserProfile != null) {
        val target = selectedUserProfile!!
        val isFriend = target.userId in friendIds
        UserProfileBottomSheet(
            profile = target,
            isOwnProfile = target.userId == viewModel.currentUserId,
            isFollowing = target.userId in followingIds,
            onDismiss = { selectedUserProfile = null },
            onToggleFollow = { viewModel.toggleFollow(target) },
            onMessageOrAdd = {
                selectedUserProfile = null
                if (isFriend) {
                    viewModel.createConversation(
                        title = target.displayName,
                        groupChat = false,
                        participants = listOf(target)
                    ) { convId -> onSelectConversation(convId) }
                } else {
                    viewModel.sendFriendRequest(target)
                }
            },
            actionLabel = if (isFriend) "Message" else "Add Friend"
        )
    }
}

@Composable
private fun PlayerSocialRow(
    profile: UserProfile,
    isFriend: Boolean,
    isFollowing: Boolean,
    onViewProfile: () -> Unit,
    onFollow: () -> Unit,
    onAddOrMessage: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onViewProfile() },
        shape = RoundedCornerShape(16.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ObsidianOutline)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AvatarFace(
                avatarStyle = profile.avatarStyle,
                photoUri = profile.avatarPhotoUri,
                size = 44.dp,
                isLive = profile.liveNow,
                showVerified = profile.verifiedBadge
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.displayName, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("@${profile.handle} • LV ${profile.level}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            TextButton(onClick = onFollow) {
                Text(if (isFollowing) "Following" else "Follow", color = CyanNeon)
            }
            FilledTonalButton(onClick = onAddOrMessage) {
                Text(if (isFriend) "Message" else "Add")
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = TextMuted)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("View Profile") }, onClick = { menuOpen = false; onViewProfile() })
                    DropdownMenuItem(text = { Text("Report User") }, onClick = { menuOpen = false; onReport() })
                    DropdownMenuItem(text = { Text("Block User", color = CoralLive) }, onClick = { menuOpen = false; onBlock() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationDetailView(
    conversation: Conversation,
    viewModel: ArcadeViewModel,
    onBack: () -> Unit
) {
    val messagesState by viewModel.selectedMessages.collectAsStateWithLifecycle()
    val messages = (messagesState as? UiState.Success)?.data.orEmpty()
    var input by remember { mutableStateOf("") }

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
                        Text(conversation.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            conversation.participantLabels.joinToString(", "),
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanNeon,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
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
                            shape = RoundedCornerShape(16.dp),
                            color = if (isMe) CyanContainer else ObsidianSurfaceElevated
                        ) {
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.take(500) },
                    placeholder = { Text("Send message...") },
                    modifier = Modifier.weight(1f).testTag("chat_message_input"),
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        if (input.isNotBlank()) {
                            viewModel.sendMessage(conversation, input)
                            input = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CyanNeon)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = ObsidianBg)
                }
            }
        }
    }
}

@Composable
private fun NewConversationDialog(
    availableUsers: List<PublicProfile>,
    onDismiss: () -> Unit,
    onCreate: (title: String, isGroup: Boolean, participants: List<PublicProfile>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    val selectedIds = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Message / Group Chat") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
                    label = { Text("Group Name (optional for DM)") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (availableUsers.isEmpty()) {
                    Text("No other players available yet.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                        items(availableUsers, key = { it.userId }) { user ->
                            val checked = user.userId in selectedIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (checked) selectedIds.remove(user.userId) else selectedIds.add(user.userId)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = checked, onCheckedChange = {
                                    if (checked) selectedIds.remove(user.userId) else selectedIds.add(user.userId)
                                })
                                Text("${user.displayName} (@${user.handle})", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val selectedProfiles = availableUsers.filter { it.userId in selectedIds }
            val isGroup = selectedProfiles.size > 1
            val finalTitle = title.ifBlank {
                if (isGroup) "Squad Group Chat" else selectedProfiles.firstOrNull()?.displayName ?: "Direct Message"
            }
            Button(
                onClick = { onCreate(finalTitle, isGroup, selectedProfiles) },
                enabled = selectedProfiles.isNotEmpty()
            ) {
                Text(if (isGroup) "Create Group" else "Start Chat")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
