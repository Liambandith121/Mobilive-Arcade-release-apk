package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.ui.theme.*

@Composable
fun OnlineFriendsSummaryCard(
    totalFriends: Int,
    onlineFriends: Int,
    lfgFriends: Int,
    pendingPartyInvites: Int,
    onInviteSquad: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summaryGradient = Brush.linearGradient(
        listOf(ObsidianSurfaceElevated, ObsidianSurface)
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("online_friends_summary_card"),
        shape = RoundedCornerShape(20.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .background(summaryGradient)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(if (onlineFriends > 0) VoltGreen else TextMuted)
                        )
                        Text(
                            text = "Squad & Friends Hub",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "$onlineFriends of $totalFriends online • $lfgFriends squad ready",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.testTag("online_friends_count_label")
                    )
                }

                if (pendingPartyInvites > 0) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = GoldContainer,
                        border = BorderStroke(1.dp, GoldCoin.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "$pendingPartyInvites Invite${if (pendingPartyInvites == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldCoin,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Button(
                onClick = onInviteSquad,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VoltGreen,
                    contentColor = ObsidianBg
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("open_party_invite_sheet_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.GroupAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Invite Friends to Multiplayer Party", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PartyInviteBannerCard(
    invite: PartyInvite,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("party_invite_card_${invite.id}"),
        shape = RoundedCornerShape(18.dp),
        color = ObsidianSurfaceElevated,
        border = BorderStroke(1.5.dp, CyanNeon.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AvatarFace(
                    avatarStyle = invite.senderAvatar,
                    size = 44.dp,
                    isLive = false
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${invite.senderName} invited you to a Party",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "${invite.partyName} • ${invite.gameTitle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = CyanNeon,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("decline_party_invite_${invite.id}"),
                    border = BorderStroke(1.dp, CoralLive.copy(alpha = 0.5f))
                ) {
                    Text("Decline", color = CoralLive)
                }
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VoltGreen,
                        contentColor = ObsidianBg
                    ),
                    modifier = Modifier
                        .weight(1.4f)
                        .heightIn(min = 48.dp)
                        .testTag("accept_party_invite_${invite.id}")
                ) {
                    Icon(Icons.Rounded.HeadsetMic, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Accept & Join", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AvatarWithOnlineDot(
    profile: UserProfile,
    sizeDp: Int = 46
) {
    val statusSpec = statusVisualSpec(profile.resolvedStatusKey())
    Box(modifier = Modifier.size(sizeDp.dp)) {
        AvatarFace(
            avatarStyle = profile.avatarStyle,
            photoUri = profile.avatarPhotoUri,
            size = sizeDp.dp,
            isLive = profile.liveNow,
            showVerified = profile.verifiedBadge
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(13.dp)
                .clip(CircleShape)
                .background(statusSpec.color)
                .border(2.dp, ObsidianSurface, CircleShape)
        )
    }
}

@Composable
fun FriendManagementCard(
    profile: UserProfile,
    isFollowing: Boolean,
    hasPendingPartyInvite: Boolean,
    onInviteToParty: () -> Unit,
    onMessage: () -> Unit,
    onToggleFollow: () -> Unit,
    onViewProfile: () -> Unit,
    onRemoveFriend: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusKey = profile.resolvedStatusKey()
    val statusSpec = statusVisualSpec(statusKey)
    val isOnline = profile.isOnlineOrActive()
    var menuOpen by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onViewProfile)
            .testTag("friend_card_${profile.userId}"),
        shape = RoundedCornerShape(18.dp),
        color = ObsidianSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isOnline) statusSpec.color.copy(alpha = 0.45f) else ObsidianOutline
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AvatarWithOnlineDot(profile = profile, sizeDp = 48)

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = profile.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = ObsidianSurfaceElevated
                        ) {
                            Text(
                                text = "LV ${profile.level}",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldCoin,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "@${profile.handle} • ${profile.currentGame.ifBlank { "Minecraft" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = statusSpec.containerColor.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, statusSpec.color.copy(alpha = 0.45f)),
                        modifier = Modifier.testTag("friend_status_badge_${profile.userId}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
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
                                text = if (statusKey == PlayerStatusOption.IN_GAME.key && profile.currentGame.isNotBlank()) {
                                    "${statusSpec.label} • ${profile.currentGame}"
                                } else {
                                    statusSpec.label
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = statusSpec.color,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.testTag("friend_menu_${profile.userId}")
                    ) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Manage friend ${profile.displayName}", tint = TextMuted)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("View Profile") },
                            onClick = { menuOpen = false; onViewProfile() }
                        )
                        DropdownMenuItem(
                            text = { Text("Invite to Party") },
                            onClick = { menuOpen = false; onInviteToParty() }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isFollowing) "Unfollow" else "Follow") },
                            onClick = { menuOpen = false; onToggleFollow() }
                        )
                        DropdownMenuItem(
                            text = { Text("Remove Friend", color = CoralLive) },
                            onClick = { menuOpen = false; onRemoveFriend() }
                        )
                        DropdownMenuItem(
                            text = { Text("Report User") },
                            onClick = { menuOpen = false; onReport() }
                        )
                        DropdownMenuItem(
                            text = { Text("Block User", color = CoralLive) },
                            onClick = { menuOpen = false; onBlock() }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onInviteToParty,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasPendingPartyInvite) ObsidianSurfaceElevated else VoltGreen,
                        contentColor = if (hasPendingPartyInvite) CyanNeon else ObsidianBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("friend_invite_party_${profile.userId}")
                ) {
                    Icon(
                        imageVector = if (hasPendingPartyInvite) Icons.Rounded.MarkEmailRead else Icons.Rounded.GroupAdd,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasPendingPartyInvite) "Invited • Again" else "Invite to Party",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalButton(
                    onClick = onMessage,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("friend_message_${profile.userId}")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Message")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteFriendsToPartySheet(
    friends: List<UserProfile>,
    activeParties: List<PartyRoom>,
    pendingSentInvites: List<PartyInvite>,
    currentUserId: String,
    initialParty: PartyRoom? = null,
    initialFriend: UserProfile? = null,
    onDismiss: () -> Unit,
    onInviteToExistingParty: (PartyRoom, List<UserProfile>) -> Unit,
    onCreatePartyAndInvite: (partyName: String, gameTitle: String, maxSlots: Long, invitees: List<UserProfile>) -> Unit
) {
    val myParties = remember(activeParties, currentUserId) {
        activeParties.filter { it.partyStatus == "active" && (it.leaderId == currentUserId || it.memberIds.contains(currentUserId)) }
    }
    var createNewParty by remember(myParties, initialParty) {
        mutableStateOf(initialParty == null && myParties.isEmpty())
    }
    var selectedPartyId by remember(myParties, initialParty) {
        mutableStateOf(initialParty?.id ?: myParties.firstOrNull()?.id.orEmpty())
    }
    val selectedParty = remember(myParties, selectedPartyId, createNewParty) {
        if (createNewParty) null else myParties.firstOrNull { it.id == selectedPartyId }
    }

    var newPartyName by remember { mutableStateOf("Squad Co-op Party") }
    var newPartyGame by remember { mutableStateOf(ArcadeCatalog.popularGames.first()) }
    var newPartySlots by remember { mutableLongStateOf(4L) }

    var searchQuery by remember { mutableStateOf("") }
    var onlineOnly by remember { mutableStateOf(false) }

    val selectedFriendIds = remember(initialFriend) {
        mutableStateListOf<String>().apply {
            if (initialFriend != null) add(initialFriend.userId)
        }
    }

    val filteredFriends = remember(friends, searchQuery, onlineOnly) {
        friends.filterAndSortFriends(
            query = searchQuery,
            filter = if (onlineOnly) FriendStatusFilter.ONLINE else FriendStatusFilter.ALL
        )
    }

    val pendingInviteReceiversForParty = remember(pendingSentInvites, selectedParty) {
        if (selectedParty == null) emptySet()
        else pendingSentInvites
            .filter { it.partyId == selectedParty.id && it.status == "pending" }
            .map { it.receiverId }
            .toSet()
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .testTag("invite_friends_party_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Invite Friends to Party",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pick an active multiplayer party or start a new squad",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close party invite sheet", tint = TextSecondary)
                }
            }

            // Party target selector
            if (myParties.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(myParties, key = { it.id }) { party ->
                        val isSelected = !createNewParty && party.id == selectedParty?.id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                createNewParty = false
                                selectedPartyId = party.id
                            },
                            label = {
                                Text("${party.partyName} (${party.memberIds.size}/${party.maxSlots})")
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.HeadsetMic, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                    item {
                        FilterChip(
                            selected = createNewParty,
                            onClick = { createNewParty = true },
                            label = { Text("+ New Party") }
                        )
                    }
                }
            }

            if (createNewParty) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianSurfaceElevated,
                    border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newPartyName,
                            onValueChange = { newPartyName = it.take(60) },
                            label = { Text("New Party Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("invite_new_party_name_input")
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(ArcadeCatalog.popularGames, key = { it }) { g ->
                                FilterChip(
                                    selected = newPartyGame == g,
                                    onClick = { newPartyGame = g },
                                    label = { Text(g) }
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Slots:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            listOf(2L, 4L, 6L, 8L).forEach { slots ->
                                FilterChip(
                                    selected = newPartySlots == slots,
                                    onClick = { newPartySlots = slots },
                                    label = { Text("$slots") }
                                )
                            }
                        }
                    }
                }
            }

            // Search & Online Filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it.take(40) },
                placeholder = { Text("Search friends by name or game...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("invite_friend_search_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !onlineOnly,
                        onClick = { onlineOnly = false },
                        label = { Text("All (${friends.size})") }
                    )
                    FilterChip(
                        selected = onlineOnly,
                        onClick = { onlineOnly = true },
                        label = { Text("Online Only (${friends.count { it.isOnlineOrActive() }})") },
                        modifier = Modifier.testTag("invite_filter_online_only")
                    )
                }
                val eligibleOnline = filteredFriends.filter { f ->
                    f.isOnlineOrActive() &&
                        (selectedParty?.memberIds?.contains(f.userId) != true)
                }
                if (eligibleOnline.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            eligibleOnline.forEach { f ->
                                if (f.userId !in selectedFriendIds) selectedFriendIds.add(f.userId)
                            }
                        }
                    ) {
                        Text("Select Online", color = VoltGreen)
                    }
                }
            }

            if (filteredFriends.isEmpty()) {
                EmptyArcadeState(
                    icon = Icons.Rounded.PersonSearch,
                    title = if (friends.isEmpty()) "No Friends Yet" else "No Matching Friends",
                    subtitle = if (friends.isEmpty()) {
                        "Accept or send friend requests in Discover so you can invite friends to your party."
                    } else {
                        "Try clearing your search or showing Offline/Away friends too."
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredFriends, key = { it.userId }) { friend ->
                        val alreadyInParty = selectedParty?.memberIds?.contains(friend.userId) == true
                        val alreadyInvited = friend.userId in pendingInviteReceiversForParty
                        val checked = friend.userId in selectedFriendIds
                        val statusSpec = statusVisualSpec(friend.resolvedStatusKey())

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(enabled = !alreadyInParty) {
                                    if (checked) selectedFriendIds.remove(friend.userId)
                                    else selectedFriendIds.add(friend.userId)
                                }
                                .testTag("invite_friend_row_${friend.userId}"),
                            shape = RoundedCornerShape(14.dp),
                            color = if (checked) VoltGreenContainer.copy(alpha = 0.4f) else ObsidianSurfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (checked) VoltGreen else ObsidianOutline
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AvatarWithOnlineDot(profile = friend, sizeDp = 40)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = friend.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${statusSpec.label} • ${friend.currentGame.ifBlank { "Minecraft" }}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = statusSpec.color,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                when {
                                    alreadyInParty -> {
                                        Surface(shape = RoundedCornerShape(8.dp), color = CyanContainer) {
                                            Text(
                                                text = "In Party",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = CyanNeon,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    alreadyInvited -> {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Surface(shape = RoundedCornerShape(8.dp), color = GoldContainer) {
                                                Text(
                                                    text = "Invited",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = GoldCoin,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                            Checkbox(
                                                checked = checked,
                                                onCheckedChange = {
                                                    if (checked) selectedFriendIds.remove(friend.userId)
                                                    else selectedFriendIds.add(friend.userId)
                                                }
                                            )
                                        }
                                    }
                                    else -> {
                                        Checkbox(
                                            checked = checked,
                                            onCheckedChange = {
                                                if (checked) selectedFriendIds.remove(friend.userId)
                                                else selectedFriendIds.add(friend.userId)
                                            },
                                            modifier = Modifier.testTag("invite_friend_checkbox_${friend.userId}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val selectedFriends = friends.filter { f ->
                f.userId in selectedFriendIds && (selectedParty?.memberIds?.contains(f.userId) != true)
            }
            Button(
                onClick = {
                    val party = selectedParty
                    if (!createNewParty && party != null) {
                        onInviteToExistingParty(party, selectedFriends)
                    } else {
                        onCreatePartyAndInvite(newPartyName, newPartyGame, newPartySlots, selectedFriends)
                    }
                },
                enabled = if (createNewParty) newPartyName.isNotBlank() else (selectedParty != null && selectedFriends.isNotEmpty()),
                colors = ButtonDefaults.buttonColors(containerColor = VoltGreen, contentColor = ObsidianBg),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("send_party_invites_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (createNewParty) {
                        if (selectedFriends.isEmpty()) "Create Party"
                        else "Create Party & Invite (${selectedFriends.size})"
                    } else {
                        "Send Party Invite (${selectedFriends.size})"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
