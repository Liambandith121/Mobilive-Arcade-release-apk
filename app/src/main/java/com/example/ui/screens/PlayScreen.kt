package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayScreen(
    viewModel: ArcadeViewModel,
    worldsState: UiState<List<MultiplayerWorld>>,
    partiesState: UiState<List<PartyRoom>>,
    communitiesState: UiState<List<GameCommunity>>,
    tournamentsState: UiState<List<Tournament>>,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val worlds = remember(worldsState) { (worldsState as? UiState.Success)?.data.orEmpty() }
    val parties = remember(partiesState) { (partiesState as? UiState.Success)?.data.orEmpty() }
    val communities = remember(communitiesState) { (communitiesState as? UiState.Success)?.data.orEmpty() }
    val tournaments = remember(tournamentsState) { (tournamentsState as? UiState.Success)?.data.orEmpty() }

    var selectedSection by remember { mutableIntStateOf(0) } // 0=LAN Lobby, 1=Party & Voice, 2=Tournaments & Clubs
    var showHostWorldDialog by remember { mutableStateOf(false) }
    var joiningWorldModal by remember { mutableStateOf<MultiplayerWorld?>(null) }
    var showCreatePartyDialog by remember { mutableStateOf(false) }
    var showCreateTournamentDialog by remember { mutableStateOf(false) }
    var showCreateCommunityDialog by remember { mutableStateOf(false) }
    var pendingVoiceParty by remember { mutableStateOf<PartyRoom?>(null) }

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
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Party System & Voice Chat", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text("Squad voice rooms + instant matchmaking queue", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
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
                                    subtitle = "Create a party room to chat over voice and queue into matchmaking.",
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
                                    onToggleMatchmaking = { viewModel.toggleMatchmaking(party) }
                                )
                            }
                        }
                    }
                }

                else -> {
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
                                Text("Tournaments", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                TextButton(onClick = { showCreateTournamentDialog = true }) {
                                    Text("+ Host Tournament", color = GoldCoin)
                                }
                            }
                        }

                        if (tournaments.isEmpty()) {
                            item {
                                EmptyArcadeState(
                                    icon = Icons.Rounded.EmojiEvents,
                                    title = "No tournaments scheduled",
                                    subtitle = "Launch an arcade tournament with Gift Coin prize pools!",
                                    actionLabel = "Create Tournament",
                                    onAction = { showCreateTournamentDialog = true }
                                )
                            }
                        } else {
                            items(tournaments, key = { it.id }) { t ->
                                TournamentCard(
                                    tournament = t,
                                    isRegistered = t.registeredIds.contains(viewModel.currentUserId),
                                    onToggleRegister = { viewModel.toggleRegisterTournament(t) }
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
            loadDefaultLan = { viewModel.detectLocalLanIpAsync() },
            onDismiss = { showHostWorldDialog = false },
            onHost = { name, game, mode, lan, max ->
                viewModel.hostWorld(name, game, mode, lan, max)
                showHostWorldDialog = false
            }
        )
    }

    if (joiningWorldModal != null) {
        val world = joiningWorldModal!!
        AlertDialog(
            onDismissRequest = { joiningWorldModal = null },
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
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("LAN Address", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(world.lanAddress, style = MaterialTheme.typography.titleSmall, color = CyanNeon)
                            }
                            IconButton(onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("LAN Address", world.lanAddress))
                                onShowMessage("Copied ${world.lanAddress}")
                            }) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy LAN IP", tint = CyanNeon)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.mojang.minecraftpe")
                        if (launchIntent != null) {
                            context.startActivity(launchIntent)
                        } else {
                            onShowMessage("Minecraft app not detected on this device. Open Minecraft > Play > Worlds manually!")
                        }
                        joiningWorldModal = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VoltGreen, contentColor = ObsidianBg)
                ) {
                    Text("Open Minecraft", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { joiningWorldModal = null }) { Text("Done") }
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
            onCreate = { title, game, prize, entry, teams, schedule ->
                viewModel.createTournament(title, game, prize, entry, teams, schedule)
                showCreateTournamentDialog = false
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
}

@Composable
private fun WorldLobbyCard(
    world: MultiplayerWorld,
    isHost: Boolean,
    onJoinHere: () -> Unit,
    onCloseWorld: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
                AssistChip(onClick = {}, label = { Text(world.gameMode) })
                AssistChip(onClick = {}, label = { Text("LAN: ${world.lanAddress}") })
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
    onToggleMatchmaking: () -> Unit
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
        }
    }
}

@Composable
private fun TournamentCard(
    tournament: Tournament,
    isRegistered: Boolean,
    onToggleRegister: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, GoldCoin.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tournament.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                CoinPill(coins = tournament.prizeCoins)
            }
            Text(
                "${tournament.gameTitle} • ${tournament.scheduleLabel} • ${tournament.registeredIds.size}/${tournament.maxTeams} Teams",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = onToggleRegister,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRegistered) VoltGreenContainer else GoldCoin,
                        contentColor = if (isRegistered) VoltGreen else ObsidianBg
                    )
                ) {
                    Text(if (isRegistered) "Registered" else "Enter Tournament", fontWeight = FontWeight.Bold)
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
private fun HostWorldDialog(
    loadDefaultLan: suspend () -> String,
    onDismiss: () -> Unit,
    onHost: (name: String, game: String, mode: String, lan: String, maxPlayers: Long) -> Unit
) {
    var worldName by remember { mutableStateOf("Diamond Sky Realm") }
    var gameName by remember { mutableStateOf("Minecraft") }
    var gameMode by remember { mutableStateOf("Survival") }
    var lanAddress by remember { mutableStateOf("") }
    var lanEdited by remember { mutableStateOf(false) }
    var multiplayerChecked by remember { mutableStateOf(true) }
    var lanVisibleChecked by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val detected = loadDefaultLan()
        if (!lanEdited && lanAddress.isBlank()) {
            lanAddress = detected
        }
    }

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
                OutlinedTextField(
                    value = lanAddress,
                    onValueChange = {
                        lanEdited = true
                        lanAddress = it.take(80)
                    },
                    label = { Text("LAN IP : Port") },
                    placeholder = { Text("Detecting local LAN IP...") },
                    modifier = Modifier.fillMaxWidth()
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
                onClick = { onHost(worldName, gameName, gameMode, lanAddress, 8L) },
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
private fun CreateTournamentDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, game: String, prize: Long, entry: Long, teams: Long, schedule: String) -> Unit
) {
    var title by remember { mutableStateOf("Mobilive Weekend Cup") }
    var game by remember { mutableStateOf("Minecraft") }
    var prize by remember { mutableStateOf("1000") }
    var schedule by remember { mutableStateOf("Sat 7:00 PM") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Tournament") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it.take(80) }, label = { Text("Tournament Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = game, onValueChange = { game = it.take(50) }, label = { Text("Game") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = prize, onValueChange = { prize = it.filter(Char::isDigit).take(6) }, label = { Text("Prize Pool (Coins)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = schedule, onValueChange = { schedule = it.take(40) }, label = { Text("Schedule") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(title, game, prize.toLongOrNull() ?: 500L, 0L, 16L, schedule) }, enabled = title.isNotBlank()) {
                Text("Launch")
            }
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
