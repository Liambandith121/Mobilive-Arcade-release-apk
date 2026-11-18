package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ArcadeViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayScreen(
    viewModel: ArcadeViewModel,
    worldsState: UiState<List<MultiplayerWorld>>,
    partiesState: UiState<List<PartyRoom>>,
    communitiesState: UiState<List<GameCommunity>>,
    tournamentsState: UiState<List<Tournament>>,
    onShowMessage: (String) -> Unit,
    profilesState: UiState<List<UserProfile>> = UiState.Loading,
    friendRequestsState: UiState<List<FriendRequest>> = UiState.Loading,
    partyInvitesState: UiState<List<PartyInvite>> = UiState.Loading,
    jumpToPartyVoiceToken: Int = 0,
    userPrivate: UserPrivate? = null,
    myProfile: UserProfile? = null,
    selectedTournamentRegistrationsState: UiState<List<TournamentRegistration>> = UiState.Loading,
    isSubmittingTournamentRegistration: Boolean = false,
    jumpToTournamentsToken: Int = 0
) {
    val context = LocalContext.current
    val worlds = remember(worldsState) { (worldsState as? UiState.Success)?.data.orEmpty() }
    val parties = remember(partiesState) { (partiesState as? UiState.Success)?.data.orEmpty() }
    val communities = remember(communitiesState) { (communitiesState as? UiState.Success)?.data.orEmpty() }
    val tournaments = remember(tournamentsState) { (tournamentsState as? UiState.Success)?.data.orEmpty() }
    val allProfiles = remember(profilesState, viewModel.currentUserId) {
        (profilesState as? UiState.Success)?.data.orEmpty().filter { it.userId != viewModel.currentUserId }
    }
    val friendIds = remember(friendRequestsState, viewModel.currentUserId) {
        (friendRequestsState as? UiState.Success)?.data.orEmpty()
            .filter { it.status == "accepted" }
            .map { if (it.senderId == viewModel.currentUserId) it.receiverId else it.senderId }
            .toSet()
    }
    val friendsProfiles = remember(allProfiles, friendIds) {
        allProfiles.filter { it.userId in friendIds }
            .sortedWith(compareBy<UserProfile> { it.statusSortPriority() }.thenBy { it.displayName.lowercase() })
    }
    val allPartyInvites = remember(partyInvitesState) { (partyInvitesState as? UiState.Success)?.data.orEmpty() }
    val incomingPartyInvites = remember(allPartyInvites, viewModel.currentUserId) {
        allPartyInvites.filter { it.status == "pending" && it.receiverId == viewModel.currentUserId }
    }
    val outgoingPartyInvites = remember(allPartyInvites, viewModel.currentUserId) {
        allPartyInvites.filter { it.status == "pending" && it.senderId == viewModel.currentUserId }
    }

    var selectedSection by remember { mutableIntStateOf(0) } // 0=LAN Lobby, 1=Party & Voice, 2=Tournaments & Clubs
    var showHostWorldDialog by remember { mutableStateOf(false) }
    var joiningWorldModal by remember { mutableStateOf<MultiplayerWorld?>(null) }
    var showCreatePartyDialog by remember { mutableStateOf(false) }
    var inviteTargetParty by remember { mutableStateOf<PartyRoom?>(null) }
    var showInviteSheet by remember { mutableStateOf(false) }
    var showCreateTournamentDialog by remember { mutableStateOf(false) }
    var showCreateCommunityDialog by remember { mutableStateOf(false) }
    var pendingVoiceParty by remember { mutableStateOf<PartyRoom?>(null) }
    var tournamentFilter by remember { mutableStateOf("All") }
    var selectedTournamentIdForRegistration by remember { mutableStateOf<String?>(null) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val selectedTournamentForRegistration = remember(tournaments, selectedTournamentIdForRegistration) {
        tournaments.firstOrNull { it.id == selectedTournamentIdForRegistration }
    }
    val selectedTournamentRegistrations = remember(selectedTournamentRegistrationsState) {
        (selectedTournamentRegistrationsState as? UiState.Success)?.data.orEmpty()
    }

    LaunchedEffect(selectedSection, tournaments.size, selectedTournamentIdForRegistration) {
        if (selectedSection == 2 || selectedTournamentIdForRegistration != null) {
            while (true) {
                nowMillis = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

    LaunchedEffect(selectedTournamentIdForRegistration) {
        viewModel.selectTournament(selectedTournamentIdForRegistration)
    }

    LaunchedEffect(jumpToPartyVoiceToken) {
        if (jumpToPartyVoiceToken > 0) {
            selectedSection = 1
        }
    }

    LaunchedEffect(jumpToTournamentsToken) {
        if (jumpToTournamentsToken > 0) {
            selectedSection = 2
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val target = pendingVoiceParty
        pendingVoiceParty = null
        if (granted && target != null) {
            viewModel.togglePartyMicMute(target)
        } else if (!granted) {
            onShowMessage("Microphone permission is needed to unmute in Party Voice Chat.")
        }
    }

    Scaffold(
        containerColor = ObsidianBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBg),
                title = { Text("Multiplayer & LAN", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    Button(
                        onClick = { showHostWorldDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = VoltGreen, contentColor = ObsidianBg),
                        modifier = Modifier.testTag("host_world_top_button")
                    ) {
                        Icon(Icons.Rounded.WifiTethering, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Host World", fontWeight = FontWeight.Bold)
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
                listOf("LAN Worlds", "Party Voice", "Tournaments").forEachIndexed { idx, label ->
                    SegmentedButton(
                        selected = selectedSection == idx,
                        onClick = { selectedSection = idx },
                        shape = SegmentedButtonDefaults.itemShape(idx, 3),
                        modifier = Modifier.testTag("play_section_$idx")
                    ) {
                        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            when (selectedSection) {
                0 -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item { LanInstructionBanner() }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Open Multiplayer Worlds (${worlds.size})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                TextButton(onClick = { showHostWorldDialog = true }) {
                                    Text("+ Host New", color = VoltGreen)
                                }
                            }
                        }

                        if (worlds.isEmpty()) {
                            item {
                                EmptyArcadeState(
                                    icon = Icons.Rounded.Public,
                                    title = "No open LAN worlds",
                                    subtitle = stringResource(R.string.lan_hosting_instruction),
                                    actionLabel = "Host Your World",
                                    onAction = { showHostWorldDialog = true }
                                )
                            }
                        } else {
                            items(worlds, key = { it.id }) { world ->
                                WorldLobbyCard(
                                    world = world,
                                    isHost = world.hostId == viewModel.currentUserId,
                                    onJoinHere = {
                                        viewModel.joinWorld(world)
                                        joiningWorldModal = world
                                    },
                                    onCloseWorld = { viewModel.closeWorld(world.id) }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        if (incomingPartyInvites.isNotEmpty()) {
                            item {
                                Text("Party Invitations (${incomingPartyInvites.size})", style = MaterialTheme.typography.titleMedium, color = CyanNeon)
                            }
                            items(incomingPartyInvites, key = { "play_pi_${it.id}" }) { invite ->
                                PartyInviteBannerCard(
                                    invite = invite,
                                    onAccept = { viewModel.respondPartyInvite(invite, true) },
                                    onDecline = { viewModel.respondPartyInvite(invite, false) }
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Party System & Voice Chat", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text("Squad voice rooms + invite online friends", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                Button(
                                    onClick = { showCreatePartyDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = ObsidianBg),
                                    modifier = Modifier.testTag("create_party_button")
                                ) {
                                    Text("Create Party", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (parties.isEmpty()) {
                            item {
                                EmptyArcadeState(
                                    icon = Icons.Rounded.HeadsetMic,
                                    title = "No active Voice Parties",
                                    subtitle = "Create a party room to chat over voice and invite your friends.",
                                    actionLabel = "Start Party Voice",
                                    onAction = { showCreatePartyDialog = true }
                                )
                            }
                        } else {
                            items(parties, key = { it.id }) { party ->
                                PartyVoiceCard(
                                    party = party,
                                    currentUserId = viewModel.currentUserId,
                                    onJoinLeave = { viewModel.joinOrLeaveParty(party) },
                                    onToggleMic = {
                                        val isCurrentlyMuted = party.mutedMemberIds.contains(viewModel.currentUserId)
                                        val hasPerm = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (isCurrentlyMuted && !hasPerm) {
                                            pendingVoiceParty = party
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        } else if (!hasPerm) {
                                            pendingVoiceParty = party
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        } else {
                                            viewModel.togglePartyMicMute(party)
                                        }
                                    },
                                    onToggleMatchmaking = { viewModel.toggleMatchmaking(party) },
                                    onInviteFriends = {
                                        inviteTargetParty = party
                                        showInviteSheet = true
                                    }
                                )
                            }
                        }
                    }
                }

                else -> {
                    val featuredUpcoming = remember(tournaments, nowMillis) {
                        tournaments.firstOrNull {
                            it.countdownStatus(nowMillis).phase == TournamentPhase.UPCOMING
                        }
                    }
                    val filteredTournaments = remember(tournaments, tournamentFilter, nowMillis, viewModel.currentUserId) {
                        when (tournamentFilter) {
                            "Upcoming" -> tournaments.filter { it.countdownStatus(nowMillis).phase == TournamentPhase.UPCOMING }
                            "Registered" -> tournaments.filter { it.registeredIds.contains(viewModel.currentUserId) }
                            "Live" -> tournaments.filter { it.countdownStatus(nowMillis).phase == TournamentPhase.LIVE }
                            else -> tournaments
                        }
                    }

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
                                Text("Tournaments & Events", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                TextButton(
                                    onClick = { showCreateTournamentDialog = true },
                                    modifier = Modifier.testTag("host_tournament_button")
                                ) {
                                    Text("+ Host Tournament", color = GoldCoin, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (featuredUpcoming != null) {
                            item(key = "spotlight_${featuredUpcoming.id}") {
                                TournamentSpotlightCard(
                                    tournament = featuredUpcoming,
                                    nowMillis = nowMillis,
                                    isRegistered = featuredUpcoming.registeredIds.contains(viewModel.currentUserId),
                                    onOpenRegistration = { selectedTournamentIdForRegistration = featuredUpcoming.id }
                                )
                            }
                        }

                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf("All", "Upcoming", "Registered", "Live"), key = { it }) { filter ->
                                    FilterChip(
                                        selected = tournamentFilter == filter,
                                        onClick = { tournamentFilter = filter },
                                        label = { Text(filter) },
                                        modifier = Modifier.testTag("tournament_filter_${filter.lowercase()}")
                                    )
                                }
                            }
                        }

                        if (filteredTournaments.isEmpty()) {
                            item {
                                EmptyArcadeState(
                                    icon = Icons.Rounded.EmojiEvents,
                                    title = if (tournaments.isEmpty()) "No tournaments scheduled" else "No matching tournaments",
                                    subtitle = if (tournaments.isEmpty()) {
                                        "Launch an upcoming gaming tournament with a live countdown and prize pool!"
                                    } else {
                                        "Try switching the filter or host a new upcoming event."
                                    },
                                    actionLabel = "Host Tournament",
                                    onAction = { showCreateTournamentDialog = true }
                                )
                            }
                        } else {
                            items(filteredTournaments, key = { it.id }) { t ->
                                TournamentCard(
                                    tournament = t,
                                    nowMillis = nowMillis,
                                    isRegistered = t.registeredIds.contains(viewModel.currentUserId),
                                    onOpenRegistration = { selectedTournamentIdForRegistration = t.id }
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Game Communities", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                TextButton(onClick = { showCreateCommunityDialog = true }) {
                                    Text("+ New Community", color = CyanNeon)
                                }
                            }
                        }

                        if (communities.isEmpty()) {
                            item {
                                EmptyArcadeState(
                                    icon = Icons.Rounded.Groups,
                                    title = "No communities yet",
                                    subtitle = "Create a hub for your favorite game to organize LAN events.",
                                    actionLabel = "Launch Community",
                                    onAction = { showCreateCommunityDialog = true }
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

    if (showHostWorldDialog) {
        HostWorldDialog(
            onDismiss = { showHostWorldDialog = false },
            onHost = { name, game, mode, max ->
                viewModel.hostWorld(name, game, mode, max)
                showHostWorldDialog = false
            }
        )
    }

    if (joiningWorldModal != null) {
        val world = joiningWorldModal!!
        JoinWorldDialog(
            world = world,
            onDismiss = { joiningWorldModal = null },
            onOpenMinecraft = {
                val launchIntent = context.packageManager.getLaunchIntentForPackage("com.mojang.minecraftpe")
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                } else {
                    onShowMessage("Minecraft app not detected on this device. Open Minecraft > Play > Worlds manually!")
                }
                joiningWorldModal = null
            }
        )
    }

    if (showCreatePartyDialog) {
        CreatePartyDialog(
            onDismiss = { showCreatePartyDialog = false },
            onCreate = { name, game, slots ->
                viewModel.createParty(name, game, slots)
                showCreatePartyDialog = false
            }
        )
    }

    if (showCreateTournamentDialog) {
        CreateTournamentDialog(
            onDismiss = { showCreateTournamentDialog = false },
            onCreate = { title, game, prize, entry, teams, schedule, startsAtMillis, formatLabel, rulesSummary ->
                viewModel.createTournament(
                    title = title,
                    gameTitle = game,
                    prizeCoins = prize,
                    entryCoins = entry,
                    maxTeams = teams,
                    schedule = schedule,
                    startsAtMillis = startsAtMillis,
                    formatLabel = formatLabel,
                    rulesSummary = rulesSummary
                )
                showCreateTournamentDialog = false
            }
        )
    }

    if (selectedTournamentForRegistration != null) {
        TournamentRegistrationSheet(
            tournament = selectedTournamentForRegistration,
            nowMillis = nowMillis,
            currentUserId = viewModel.currentUserId,
            userPrivate = userPrivate,
            myProfile = myProfile,
            registrations = selectedTournamentRegistrations,
            isSubmitting = isSubmittingTournamentRegistration,
            onDismiss = { selectedTournamentIdForRegistration = null },
            onConfirmRegister = { teamName, gamerTag, division ->
                viewModel.registerForTournament(
                    tournament = selectedTournamentForRegistration,
                    teamName = teamName,
                    gamerTag = gamerTag,
                    division = division
                )
            },
            onWithdrawRegistration = { paidCoins ->
                viewModel.withdrawTournamentRegistration(
                    tournament = selectedTournamentForRegistration,
                    entryPaidCoins = paidCoins
                )
            },
            onUpdateTournamentState = { nextState ->
                viewModel.updateTournamentState(selectedTournamentForRegistration.id, nextState)
            }
        )
    }

    if (showCreateCommunityDialog) {
        CreateCommunityDialog(
            onDismiss = { showCreateCommunityDialog = false },
            onCreate = { name, cat, desc ->
                viewModel.createCommunity(name, cat, desc)
                showCreateCommunityDialog = false
            }
        )
    }

    if (showInviteSheet) {
        InviteFriendsToPartySheet(
            friends = friendsProfiles,
            activeParties = parties,
            pendingSentInvites = outgoingPartyInvites,
            currentUserId = viewModel.currentUserId,
            initialParty = inviteTargetParty,
            onDismiss = {
                showInviteSheet = false
                inviteTargetParty = null
            },
            onInviteToExistingParty = { party, invitees ->
                viewModel.inviteFriendsToParty(invitees, party)
                showInviteSheet = false
                inviteTargetParty = null
            },
            onCreatePartyAndInvite = { partyName, gameTitle, maxSlots, invitees ->
                viewModel.createPartyAndInviteFriends(partyName, gameTitle, maxSlots, invitees) {
                    showInviteSheet = false
                    inviteTargetParty = null
                }
            }
        )
    }
}

@Composable
internal fun WorldLobbyCard(
    world: MultiplayerWorld,
    isHost: Boolean,
    onJoinHere: () -> Unit,
    onCloseWorld: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("world_lobby_card_${world.id}"),
        shape = RoundedCornerShape(18.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, VoltGreen.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    AvatarFace(avatarStyle = world.hostAvatar, size = 42.dp, isLive = true)
                    Column {
                        Text(world.worldName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("Host: ${world.hostName} • ${world.gameName}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
                Surface(shape = RoundedCornerShape(50), color = VoltGreenContainer) {
                    Text(
                        "${world.participantIds.size}/${world.maxPlayers}",
                        style = MaterialTheme.typography.labelSmall,
                        color = VoltGreen,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ObsidianSurfaceElevated,
                    border = BorderStroke(1.dp, ObsidianOutline)
                ) {
                    Text(
                        text = world.gameMode,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = VoltGreenContainer.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, VoltGreen.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WifiTethering,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = VoltGreen
                        )
                        Text(
                            text = if (world.lanVisible) "LAN Visible" else "Multiplayer",
                            style = MaterialTheme.typography.labelMedium,
                            color = VoltGreen
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isHost) {
                    TextButton(onClick = onCloseWorld) {
                        Text("Close World", color = CoralLive)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Button(
                    onClick = onJoinHere,
                    colors = ButtonDefaults.buttonColors(containerColor = VoltGreen, contentColor = ObsidianBg),
                    modifier = Modifier.testTag("join_world_button_${world.id}")
                ) {
                    Text("Join here", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PartyVoiceCard(
    party: PartyRoom,
    currentUserId: String,
    onJoinLeave: () -> Unit,
    onToggleMic: () -> Unit,
    onToggleMatchmaking: () -> Unit,
    onInviteFriends: () -> Unit
) {
    val inParty = party.memberIds.contains(currentUserId)
    val isMuted = party.mutedMemberIds.contains(currentUserId)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(party.partyName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text("${party.gameTitle} • Leader: ${party.leaderName}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Surface(shape = RoundedCornerShape(50), color = CyanContainer) {
                    Text(
                        "${party.memberIds.size}/${party.maxSlots} Voice",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanNeon,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Voice Members Strip
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                party.memberIds.forEach { memberId ->
                    val memberMuted = party.mutedMemberIds.contains(memberId)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (memberMuted) ObsidianSurfaceElevated else VoltGreenContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (memberMuted) Icons.Rounded.MicOff else Icons.Rounded.GraphicEq,
                            contentDescription = "Voice member",
                            tint = if (memberMuted) TextMuted else VoltGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Matchmaking Banner
            Surface(shape = RoundedCornerShape(12.dp), color = ObsidianSurfaceElevated) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = when (party.queueState) {
                                "searching" -> "Matchmaking: Searching..."
                                "matched" -> "Match Found! Lobby ${party.matchedLobbyCode}"
                                else -> "Matchmaking: Ready"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = when (party.queueState) {
                                "matched" -> VoltGreen
                                "searching" -> GoldCoin
                                else -> TextPrimary
                            }
                        )
                        Text("Tap to cycle queue status", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    if (party.leaderId == currentUserId) {
                        FilledTonalButton(onClick = onToggleMatchmaking) {
                            Text(if (party.queueState == "idle") "Find Match" else "Next")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (inParty) {
                    OutlinedButton(
                        onClick = onToggleMic,
                        modifier = Modifier.weight(1f).testTag("toggle_party_mic_${party.id}")
                    ) {
                        Icon(if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isMuted) "Unmute Mic" else "Mic Live")
                    }
                }
                Button(
                    onClick = onJoinLeave,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (inParty) CoralContainer else CyanNeon,
                        contentColor = if (inParty) Color.White else ObsidianBg
                    )
                ) {
                    Text(if (inParty) "Leave Party" else "Join Voice", fontWeight = FontWeight.Bold)
                }
            }
            if (inParty && party.memberIds.size < party.maxSlots.toInt()) {
                FilledTonalButton(
                    onClick = onInviteFriends,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("party_invite_friends_${party.id}")
                ) {
                    Icon(Icons.Rounded.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Invite Friends to Party", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun CommunityCard(
    community: GameCommunity,
    isMember: Boolean,
    onToggleJoin: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ObsidianOutline)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyanContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Groups, contentDescription = null, tint = CyanNeon)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(community.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text("${community.gameCategory} • ${community.memberCount} members", style = MaterialTheme.typography.labelSmall, color = CyanNeon)
                Text(community.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            FilledTonalButton(onClick = onToggleJoin) {
                Text(if (isMember) "Joined" else "Join")
            }
        }
    }
}

@Composable
internal fun JoinWorldDialog(
    world: MultiplayerWorld,
    onDismiss: () -> Unit,
    onOpenMinecraft: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ready to Join ${world.worldName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(12.dp), color = VoltGreenContainer) {
                    Text(
                        text = stringResource(R.string.lan_joining_instruction),
                        style = MaterialTheme.typography.bodyMedium,
                        color = VoltGreen,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Surface(shape = RoundedCornerShape(12.dp), color = ObsidianSurfaceElevated) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Host: ${world.hostName}",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = "${world.gameName} • ${world.gameMode}",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyanNeon
                            )
                        }
                        Surface(shape = RoundedCornerShape(50), color = VoltGreenContainer) {
                            Text(
                                text = "${world.participantIds.size}/${world.maxPlayers}",
                                style = MaterialTheme.typography.labelSmall,
                                color = VoltGreen,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onOpenMinecraft,
                colors = ButtonDefaults.buttonColors(containerColor = VoltGreen, contentColor = ObsidianBg),
                modifier = Modifier.testTag("open_minecraft_button")
            ) {
                Text("Open Minecraft", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
internal fun HostWorldDialog(
    onDismiss: () -> Unit,
    onHost: (name: String, game: String, mode: String, maxPlayers: Long) -> Unit
) {
    var worldName by remember { mutableStateOf("Diamond Sky Realm") }
    var gameName by remember { mutableStateOf("Minecraft") }
    var gameMode by remember { mutableStateOf("Survival") }
    var multiplayerChecked by remember { mutableStateOf(true) }
    var lanVisibleChecked by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Host Multiplayer LAN World") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(12.dp), color = VoltGreenContainer) {
                    Text(
                        text = stringResource(R.string.lan_hosting_instruction),
                        style = MaterialTheme.typography.bodySmall,
                        color = VoltGreen,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = multiplayerChecked, onCheckedChange = { multiplayerChecked = it })
                    Text("Multiplayer Game turned ON", style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = lanVisibleChecked, onCheckedChange = { lanVisibleChecked = it })
                    Text("Visible to LAN Players turned ON", style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = worldName,
                    onValueChange = { worldName = it.take(80) },
                    label = { Text("World Name") },
                    modifier = Modifier.fillMaxWidth().testTag("host_world_name_input")
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("Survival", "Creative", "Adventure", "Minigame"), key = { it }) { m ->
                        FilterChip(selected = gameMode == m, onClick = { gameMode = m }, label = { Text(m) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onHost(worldName, gameName, gameMode, 8L) },
                enabled = worldName.isNotBlank() && multiplayerChecked && lanVisibleChecked,
                colors = ButtonDefaults.buttonColors(containerColor = VoltGreen, contentColor = ObsidianBg),
                modifier = Modifier.testTag("confirm_host_world_button")
            ) {
                Text("Open World", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CreatePartyDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, game: String, slots: Long) -> Unit
) {
    var name by remember { mutableStateOf("Ranked Squad Voice") }
    var game by remember { mutableStateOf(ArcadeCatalog.popularGames.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Party Voice Room") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it.take(60) }, label = { Text("Party Name") }, modifier = Modifier.fillMaxWidth())
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ArcadeCatalog.popularGames, key = { it }) { g ->
                        FilterChip(selected = game == g, onClick = { game = g }, label = { Text(g) })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(name, game, 4L) }, enabled = name.isNotBlank()) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CreateCommunityDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, category: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf("Minecraft LAN Builders") }
    var category by remember { mutableStateOf("Minecraft") }
    var desc by remember { mutableStateOf("Share worlds, Redstone tutorials, and co-op raids.") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Game Community") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it.take(60) }, label = { Text("Community Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = category, onValueChange = { category = it.take(40) }, label = { Text("Game Category") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it.take(250) }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(name, category, desc) }, enabled = name.length >= 2) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
