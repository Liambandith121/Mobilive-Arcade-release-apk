package com.example.ui.screens

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
    onSelectConversation: (String?) -> Unit,
    partiesState: UiState<List<PartyRoom>> = UiState.Loading,
    partyInvitesState: UiState<List<PartyInvite>> = UiState.Loading,
    onOpenPartyVoice: () -> Unit = {}
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
    val pendingOutgoing = remember(allRequests, viewModel.currentUserId) {
        allRequests.filter { it.status == "pending" && it.senderId == viewModel.currentUserId }
    }
    val outgoingTargetIds = remember(pendingOutgoing) {
        pendingOutgoing.map { it.receiverId }.toSet()
    }
    val acceptedRequestsByFriendId = remember(allRequests, viewModel.currentUserId) {
        allRequests.filter { it.status == "accepted" }.associateBy { req ->
            if (req.senderId == viewModel.currentUserId) req.receiverId else req.senderId
        }
    }
    val friendIds = remember(acceptedRequestsByFriendId) { acceptedRequestsByFriendId.keys }
    val friendsProfiles = remember(allProfiles, friendIds) {
        allProfiles.filter { it.userId in friendIds }
            .sortedWith(compareBy<UserProfile> { it.statusSortPriority() }.thenBy { it.displayName.lowercase() })
    }
    val onlineFriendsCount = remember(friendsProfiles) { friendsProfiles.count { it.isOnlineOrActive() } }
    val lfgFriendsCount = remember(friendsProfiles) {
        friendsProfiles.count { it.resolvedStatusKey() == PlayerStatusOption.LFG.key }
    }

    val allParties = remember(partiesState) { (partiesState as? UiState.Success)?.data.orEmpty() }
    val allPartyInvites = remember(partyInvitesState) { (partyInvitesState as? UiState.Success)?.data.orEmpty() }
    val incomingPartyInvites = remember(allPartyInvites, viewModel.currentUserId) {
        allPartyInvites.filter { it.status == "pending" && it.receiverId == viewModel.currentUserId }
    }
    val outgoingPartyInvites = remember(allPartyInvites, viewModel.currentUserId) {
        allPartyInvites.filter { it.status == "pending" && it.senderId == viewModel.currentUserId }
    }
    val invitedFriendIds = remember(outgoingPartyInvites) {
        outgoingPartyInvites.map { it.receiverId }.toSet()
    }

    val conversations = remember(conversationsState) { (conversationsState as? UiState.Success)?.data.orEmpty() }
    val communities = remember(communitiesState) { (communitiesState as? UiState.Success)?.data.orEmpty() }

    val activeConversation = remember(conversations, selectedConversationId) {
        conversations.firstOrNull { it.id == selectedConversationId }
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0=Chats, 1=Friends & Invites, 2=Clubs
    var friendSearchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf(FriendStatusFilter.ALL) }
    var showNewChatDialog by remember { mutableStateOf(false) }
    var showPartyInviteSheet by remember { mutableStateOf(false) }
    var preselectedFriendForInvite by remember { mutableStateOf<UserProfile?>(null) }
    var friendPendingRemoval by remember { mutableStateOf<Pair<UserProfile, FriendRequest>?>(null) }
    var reportingTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedUserProfile by remember { mutableStateOf<UserProfile?>(null) }

    val filteredFriends = remember(friendsProfiles, friendSearchQuery, selectedStatusFilter) {
        friendsProfiles.filterAndSortFriends(friendSearchQuery, selectedStatusFilter)
    }
    val discoverList = remember(allProfiles, friendIds, pendingIncoming, friendSearchQuery, selectedStatusFilter) {
        val incomingSenderIds = pendingIncoming.map { it.senderId }.toSet()
        allProfiles
            .filter { it.userId !in friendIds && it.userId !in incomingSenderIds }
            .filterAndSortFriends(friendSearchQuery, selectedStatusFilter)
    }

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
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showNewChatDialog = true },
                    containerColor = CyanNeon,
                    contentColor = ObsidianBg,
                    modifier = Modifier.testTag("social_message_fab")
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = "New Message or Group Chat")
                }
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
                    val friendsBadge = if (onlineFriendsCount > 0) " (${onlineFriendsCount} on)" else " (${friendsProfiles.size})"
                    listOf(
                        "Chats (${conversations.size})",
                        "Friends$friendsBadge",
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

                if (incomingPartyInvites.isNotEmpty() && selectedTab != 1) {
                    PartyInviteBannerCard(
                        invite = incomingPartyInvites.first(),
                        onAccept = {
                            viewModel.respondPartyInvite(incomingPartyInvites.first(), true) {
                                onOpenPartyVoice()
                            }
                        },
                        onDecline = {
                            viewModel.respondPartyInvite(incomingPartyInvites.first(), false)
                        }
                    )
                }

                when (selectedTab) {
                    0 -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 88.dp)
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
                            contentPadding = PaddingValues(bottom = 88.dp)
                        ) {
                            item {
                                OnlineFriendsSummaryCard(
                                    totalFriends = friendsProfiles.size,
                                    onlineFriends = onlineFriendsCount,
                                    lfgFriends = lfgFriendsCount,
                                    pendingPartyInvites = incomingPartyInvites.size,
                                    onInviteSquad = {
                                        preselectedFriendForInvite = null
                                        showPartyInviteSheet = true
                                    }
                                )
                            }

                            if (incomingPartyInvites.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Party Invitations (${incomingPartyInvites.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = CyanNeon
                                    )
                                }
                                items(incomingPartyInvites, key = { "pi_${it.id}" }) { invite ->
                                    PartyInviteBannerCard(
                                        invite = invite,
                                        onAccept = {
                                            viewModel.respondPartyInvite(invite, true) {
                                                onOpenPartyVoice()
                                            }
                                        },
                                        onDecline = { viewModel.respondPartyInvite(invite, false) }
                                    )
                                }
                            }

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
                                OutlinedTextField(
                                    value = friendSearchQuery,
                                    onValueChange = { friendSearchQuery = it.take(40) },
                                    placeholder = { Text("Search friends or players...") },
                                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                    trailingIcon = {
                                        if (friendSearchQuery.isNotEmpty()) {
                                            IconButton(onClick = { friendSearchQuery = "" }) {
                                                Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("friend_search_input")
                                )
                            }

                            item {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(FriendStatusFilter.entries, key = { it.name }) { filter ->
                                        FilterChip(
                                            selected = selectedStatusFilter == filter,
                                            onClick = { selectedStatusFilter = filter },
                                            label = { Text(filter.label) },
                                            modifier = Modifier.testTag("friend_filter_${filter.name.lowercase()}")
                                        )
                                    }
                                }
                            }

                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "My Friends (${filteredFriends.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    if (friendsProfiles.isNotEmpty()) {
                                        TextButton(
                                            onClick = {
                                                preselectedFriendForInvite = null
                                                showPartyInviteSheet = true
                                            }
                                        ) {
                                            Text("Invite All Online", color = VoltGreen)
                                        }
                                    }
                                }
                            }

                            if (filteredFriends.isEmpty()) {
                                item {
                                    EmptyArcadeState(
                                        icon = Icons.Rounded.Groups,
                                        title = if (friendsProfiles.isEmpty()) "No Friends Connected Yet" else "No Matching Friends",
                                        subtitle = if (friendsProfiles.isEmpty()) {
                                            "Send a friend request to players below to track their online status and invite them to multiplayer parties."
                                        } else {
                                            "No friends match the selected online status filter or search."
                                        }
                                    )
                                }
                            } else {
                                items(filteredFriends, key = { "f_${it.userId}" }) { friend ->
                                    FriendManagementCard(
                                        profile = friend,
                                        isFollowing = friend.userId in followingIds,
                                        hasPendingPartyInvite = friend.userId in invitedFriendIds,
                                        onInviteToParty = {
                                            preselectedFriendForInvite = friend
                                            showPartyInviteSheet = true
                                        },
                                        onMessage = {
                                            viewModel.createConversation(
                                                title = friend.displayName,
                                                groupChat = false,
                                                participants = listOf(friend)
                                            ) { convId -> onSelectConversation(convId) }
                                        },
                                        onToggleFollow = { viewModel.toggleFollow(friend) },
                                        onViewProfile = { selectedUserProfile = friend },
                                        onRemoveFriend = {
                                            val req = acceptedRequestsByFriendId[friend.userId]
                                            if (req != null) {
                                                friendPendingRemoval = friend to req
                                            }
                                        },
                                        onReport = { reportingTarget = friend.userId to friend.handle },
                                        onBlock = { viewModel.toggleBlockUser(friend.userId, friend.handle) }
                                    )
                                }
                            }

                            if (pendingOutgoing.isNotEmpty()) {
                                item {
                                    Text("Sent Friend Requests (${pendingOutgoing.size})", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                                }
                                items(pendingOutgoing, key = { "out_${it.id}" }) { req ->
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = ObsidianSurface,
                                        border = BorderStroke(1.dp, ObsidianOutline),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(Icons.Rounded.ScheduleSend, contentDescription = null, tint = GoldCoin)
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(req.receiverName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                                Text("Pending friend request", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                            }
                                            TextButton(
                                                onClick = { viewModel.cancelFriendRequest(req.id, req.receiverName) }
                                            ) {
                                                Text("Cancel", color = CoralLive)
                                            }
                                        }
                                    }
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
                                        subtitle = "Other Mobilive Arcade players who sign in appear here so you can follow, friend, and invite them to parties!"
                                    )
                                }
                            } else {
                                items(discoverList, key = { "d_${it.userId}" }) { player ->
                                    val alreadyRequested = player.userId in outgoingTargetIds
                                    PlayerSocialRow(
                                        profile = player,
                                        isFriend = false,
                                        isPendingSent = alreadyRequested,
                                        isFollowing = player.userId in followingIds,
                                        onViewProfile = { selectedUserProfile = player },
                                        onFollow = { viewModel.toggleFollow(player) },
                                        onAddOrMessage = {
                                            if (!alreadyRequested) {
                                                viewModel.sendFriendRequest(player)
                                            }
                                        },
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
                            contentPadding = PaddingValues(bottom = 88.dp)
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

    if (showPartyInviteSheet) {
        InviteFriendsToPartySheet(
            friends = friendsProfiles,
            activeParties = allParties,
            pendingSentInvites = outgoingPartyInvites,
            currentUserId = viewModel.currentUserId,
            initialFriend = preselectedFriendForInvite,
            onDismiss = {
                showPartyInviteSheet = false
                preselectedFriendForInvite = null
            },
            onInviteToExistingParty = { party, invitees ->
                viewModel.inviteFriendsToParty(invitees, party)
                showPartyInviteSheet = false
                preselectedFriendForInvite = null
            },
            onCreatePartyAndInvite = { partyName, gameTitle, maxSlots, invitees ->
                viewModel.createPartyAndInviteFriends(partyName, gameTitle, maxSlots, invitees) {
                    showPartyInviteSheet = false
                    preselectedFriendForInvite = null
                }
            }
        )
    }

    if (friendPendingRemoval != null) {
        val (targetFriend, req) = friendPendingRemoval!!
        AlertDialog(
            onDismissRequest = { friendPendingRemoval = null },
            title = { Text("Remove ${targetFriend.displayName}?") },
            text = {
                Text("Remove @${targetFriend.handle} from your friends list? You can always send a new friend request later.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeFriend(req.id, targetFriend.displayName)
                        friendPendingRemoval = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralLive, contentColor = Color.White),
                    modifier = Modifier.testTag("confirm_remove_friend_button")
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { friendPendingRemoval = null }) { Text("Cancel") }
            }
        )
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
    isPendingSent: Boolean,
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
            AvatarWithOnlineDot(profile = profile, sizeDp = 44)
            Column(modifier = Modifier.weight(1f)) {
                val statusSpec = statusVisualSpec(profile.resolvedStatusKey())
                Text(profile.displayName, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(statusSpec.color)
                    )
                    Text(
                        text = "@${profile.handle} • ${statusSpec.label} • LV ${profile.level}",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusSpec.color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            TextButton(onClick = onFollow) {
                Text(if (isFollowing) "Following" else "Follow", color = CyanNeon)
            }
            FilledTonalButton(
                onClick = onAddOrMessage,
                enabled = !isPendingSent || isFriend
            ) {
                Text(
                    when {
                        isFriend -> "Message"
                        isPendingSent -> "Sent"
                        else -> "Add"
                    }
                )
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
