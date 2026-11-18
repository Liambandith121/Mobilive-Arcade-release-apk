package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun TournamentCountdownStrip(
    countdown: TournamentCountdown,
    modifier: Modifier = Modifier,
    compact: Boolean = true,
    tagPrefix: String = "tournament"
) {
    val accent = when {
        countdown.phase == TournamentPhase.COMPLETED -> TextMuted
        countdown.phase == TournamentPhase.LIVE -> CoralLive
        countdown.isStartingSoon -> CoralLive
        else -> GoldCoin
    }
    val container = when {
        countdown.phase == TournamentPhase.COMPLETED -> ObsidianSurfaceElevated
        countdown.phase == TournamentPhase.LIVE -> CoralContainer.copy(alpha = 0.7f)
        countdown.isStartingSoon -> CoralContainer.copy(alpha = 0.45f)
        else -> GoldContainer.copy(alpha = 0.45f)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("${tagPrefix}_countdown"),
        shape = RoundedCornerShape(14.dp),
        color = container,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = if (compact) 10.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    Text(
                        text = when (countdown.phase) {
                            TournamentPhase.COMPLETED -> "EVENT ENDED"
                            TournamentPhase.LIVE -> "EVENT LIVE NOW"
                            TournamentPhase.UPCOMING -> if (countdown.isStartingSoon) "STARTING SOON" else "STARTS IN"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = countdown.formattedCompact,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("${tagPrefix}_countdown_compact")
                )
            }

            if (countdown.phase == TournamentPhase.UPCOMING && countdown.targetMillis > 0L) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("${tagPrefix}_countdown_tiles"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (countdown.days > 0L) {
                        CountdownDigitTile(
                            value = String.format(Locale.US, "%02d", countdown.days),
                            unit = "DAYS",
                            accent = accent,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    CountdownDigitTile(
                        value = String.format(Locale.US, "%02d", countdown.hours),
                        unit = "HRS",
                        accent = accent,
                        modifier = Modifier.weight(1f)
                    )
                    CountdownDigitTile(
                        value = String.format(Locale.US, "%02d", countdown.minutes),
                        unit = "MIN",
                        accent = accent,
                        modifier = Modifier.weight(1f)
                    )
                    CountdownDigitTile(
                        value = String.format(Locale.US, "%02d", countdown.seconds),
                        unit = "SEC",
                        accent = accent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CountdownDigitTile(
    value: String,
    unit: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = ObsidianBg.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = accent
            )
        }
    }
}

@Composable
fun TournamentSpotlightCard(
    tournament: Tournament,
    nowMillis: Long,
    isRegistered: Boolean,
    onOpenRegistration: () -> Unit,
    modifier: Modifier = Modifier
) {
    val countdown = remember(tournament, nowMillis) { tournament.countdownStatus(nowMillis) }
    val gradient = Brush.linearGradient(
        listOf(ObsidianSurfaceElevated, GoldContainer.copy(alpha = 0.45f), ObsidianSurface)
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tournament_spotlight_card"),
        shape = RoundedCornerShape(22.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.5.dp, GoldCoin.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier
                .background(gradient)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(50), color = GoldContainer) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = GoldCoin, modifier = Modifier.size(15.dp))
                        Text(
                            text = "NEXT FEATURED EVENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldCoin,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                CoinPill(coins = tournament.prizeCoins)
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = tournament.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${tournament.gameTitle} • ${tournament.formatLabel.ifBlank { "Squad 4v4" }} • ${tournament.registeredIds.size}/${tournament.maxTeams} Teams",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyanNeon
                )
            }

            TournamentCountdownStrip(
                countdown = countdown,
                compact = false,
                tagPrefix = "spotlight_tournament"
            )

            Button(
                onClick = onOpenRegistration,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRegistered) VoltGreenContainer else GoldCoin,
                    contentColor = if (isRegistered) VoltGreen else ObsidianBg
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("spotlight_register_button")
            ) {
                Icon(
                    imageVector = if (isRegistered) Icons.Rounded.ConfirmationNumber else Icons.Rounded.HowToReg,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        isRegistered -> "Registered • View Event Pass"
                        countdown.isFull -> "Bracket Full • View Roster"
                        countdown.phase == TournamentPhase.LIVE -> "Event Live • View Bracket"
                        else -> "Sign Up & Reserve Spot"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TournamentCard(
    tournament: Tournament,
    nowMillis: Long,
    isRegistered: Boolean,
    onOpenRegistration: () -> Unit,
    modifier: Modifier = Modifier
) {
    val countdown = remember(tournament, nowMillis) { tournament.countdownStatus(nowMillis) }
    val fillRatio = (tournament.registeredIds.size.toFloat() / tournament.maxTeams.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onOpenRegistration)
            .testTag("tournament_card_${tournament.id}"),
        shape = RoundedCornerShape(20.dp),
        color = ObsidianSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isRegistered) VoltGreen.copy(alpha = 0.65f) else GoldCoin.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    val stateColor = when (countdown.phase) {
                        TournamentPhase.LIVE -> CoralLive
                        TournamentPhase.COMPLETED -> TextMuted
                        TournamentPhase.UPCOMING -> GoldCoin
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = stateColor.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = when (countdown.phase) {
                                TournamentPhase.LIVE -> "LIVE"
                                TournamentPhase.COMPLETED -> "ENDED"
                                TournamentPhase.UPCOMING -> "UPCOMING"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = stateColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (isRegistered) {
                        Surface(shape = RoundedCornerShape(50), color = VoltGreenContainer) {
                            Text(
                                text = "REGISTERED",
                                style = MaterialTheme.typography.labelSmall,
                                color = VoltGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                CoinPill(coins = tournament.prizeCoins)
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = tournament.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${tournament.gameTitle} • ${tournament.formatLabel.ifBlank { "Squad 4v4" }} • ${tournament.scheduleLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            TournamentCountdownStrip(
                countdown = countdown,
                compact = true,
                tagPrefix = "tournament_${tournament.id}"
            )

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${tournament.registeredIds.size} / ${tournament.maxTeams} Teams Registered",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanNeon
                    )
                    Text(
                        text = if (tournament.entryCoins > 0L) "Entry: ${tournament.entryCoins} Coins" else "Free Entry",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (tournament.entryCoins > 0L) GoldCoin else VoltGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                LinearProgressIndicator(
                    progress = { fillRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                    color = if (isRegistered) VoltGreen else CyanNeon,
                    trackColor = ObsidianSurfaceElevated
                )
            }

            Button(
                onClick = onOpenRegistration,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        isRegistered -> VoltGreenContainer
                        countdown.isFull || countdown.phase != TournamentPhase.UPCOMING -> ObsidianSurfaceElevated
                        else -> GoldCoin
                    },
                    contentColor = when {
                        isRegistered -> VoltGreen
                        countdown.isFull || countdown.phase != TournamentPhase.UPCOMING -> TextSecondary
                        else -> ObsidianBg
                    }
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("tournament_register_button_${tournament.id}")
            ) {
                Icon(
                    imageVector = if (isRegistered) Icons.Rounded.ConfirmationNumber else Icons.Rounded.HowToReg,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when {
                        isRegistered -> "View Registration Pass"
                        countdown.phase == TournamentPhase.COMPLETED -> "View Results"
                        countdown.phase == TournamentPhase.LIVE -> "Event Live • View Details"
                        countdown.isFull -> "Event Full • View Roster"
                        else -> "Register Now"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentRegistrationSheet(
    tournament: Tournament,
    nowMillis: Long,
    currentUserId: String,
    userPrivate: UserPrivate?,
    myProfile: UserProfile?,
    registrations: List<TournamentRegistration>,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirmRegister: (teamName: String, gamerTag: String, division: String) -> Unit,
    onWithdrawRegistration: (entryPaidCoins: Long) -> Unit,
    onUpdateTournamentState: (String) -> Unit
) {
    val countdown = remember(tournament, nowMillis) { tournament.countdownStatus(nowMillis) }
    val isRegistered = tournament.registeredIds.contains(currentUserId)
    val myRegistration = remember(registrations, currentUserId) {
        registrations.firstOrNull { it.userId == currentUserId }
    }
    val isOrganizer = tournament.organizerId == currentUserId
    val canRegister = countdown.phase == TournamentPhase.UPCOMING && !isRegistered && !countdown.isFull
    val entryFee = tournament.entryCoins.coerceAtLeast(0L)
    val userCoins = userPrivate?.coins ?: 0L
    val hasEnoughCoins = entryFee == 0L || userCoins >= entryFee

    var teamName by remember(tournament.id, myProfile) {
        mutableStateOf(
            myProfile?.displayName?.ifBlank { "Neon" }?.let { "$it Squad" } ?: "Neon Squad"
        )
    }
    var gamerTag by remember(tournament.id, myProfile) {
        mutableStateOf(
            myProfile?.handle?.ifBlank { "arcade_pro" }?.let { "@$it" } ?: "@arcade_pro"
        )
    }
    var division by remember(tournament.id) {
        mutableStateOf(
            when {
                tournament.formatLabel.contains("Solo", ignoreCase = true) -> "Solo"
                tournament.formatLabel.contains("Duo", ignoreCase = true) -> "Duo"
                else -> "Squad"
            }
        )
    }
    var agreedRules by remember(tournament.id) { mutableStateOf(true) }

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
                .padding(bottom = 28.dp)
                .testTag("tournament_registration_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = tournament.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${tournament.gameTitle} • ${tournament.formatLabel.ifBlank { "Squad 4v4" }} • ${tournament.scheduleLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyanNeon
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close tournament registration", tint = TextSecondary)
                }
            }

            TournamentCountdownStrip(
                countdown = countdown,
                compact = false,
                tagPrefix = "sheet_tournament"
            )

            // Prize, Entry & Rules Summary
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ObsidianSurfaceElevated,
                border = BorderStroke(1.dp, ObsidianOutline)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Prize Pool", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("${tournament.prizeCoins} Coins", style = MaterialTheme.typography.titleSmall, color = GoldCoin, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Entry Fee", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                text = if (entryFee > 0L) "$entryFee Coins" else "Free",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (entryFee > 0L) CyanNeon else VoltGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Slots Filled", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                text = "${tournament.registeredIds.size} / ${tournament.maxTeams}",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (tournament.rulesSummary.isNotBlank()) {
                        HorizontalDivider(color = ObsidianOutline)
                        Text(
                            text = tournament.rulesSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            if (isRegistered) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = VoltGreenContainer.copy(alpha = 0.55f),
                    border = BorderStroke(1.5.dp, VoltGreen.copy(alpha = 0.7f)),
                    modifier = Modifier.testTag("tournament_registered_pass_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Verified, contentDescription = null, tint = VoltGreen)
                            Text(
                                text = "Confirmed Contender Pass",
                                style = MaterialTheme.typography.titleMedium,
                                color = VoltGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (myRegistration != null) {
                            Text(
                                text = "Team: ${myRegistration.teamName} (${myRegistration.division})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "In-Game Tag: ${myRegistration.gamerTag}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        } else {
                            Text(
                                text = "Your slot is locked in for kickoff (${countdown.formattedCompact}).",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                        }

                        if (tournament.tournamentState == "upcoming") {
                            OutlinedButton(
                                onClick = {
                                    onWithdrawRegistration(myRegistration?.entryPaidCoins ?: 0L)
                                },
                                enabled = !isSubmitting,
                                border = BorderStroke(1.dp, CoralLive.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("withdraw_tournament_registration_button")
                            ) {
                                Text("Withdraw Registration", color = CoralLive, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else if (canRegister) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianSurfaceElevated,
                    border = BorderStroke(1.dp, GoldCoin.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Player / Team Sign-Up",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = teamName,
                            onValueChange = { teamName = it.take(50) },
                            label = { Text("Team or Squad Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("registration_team_name_input")
                        )

                        OutlinedTextField(
                            value = gamerTag,
                            onValueChange = { gamerTag = it.take(40) },
                            label = { Text("In-Game Tag / Player ID") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("registration_gamer_tag_input")
                        )

                        Text("Division", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Solo", "Duo", "Squad").forEach { div ->
                                FilterChip(
                                    selected = division == div,
                                    onClick = { division = div },
                                    label = { Text(div) },
                                    modifier = Modifier.testTag("registration_division_${div.lowercase()}")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { agreedRules = !agreedRules }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = agreedRules,
                                onCheckedChange = { agreedRules = it },
                                modifier = Modifier.testTag("registration_rules_checkbox")
                            )
                            Text(
                                text = "I agree to fair-play rules & 15-min pre-event check-in (+25 XP bonus)",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        if (!hasEnoughCoins) {
                            Text(
                                text = "Insufficient Gift Coins: need $entryFee Coins (you have $userCoins).",
                                style = MaterialTheme.typography.bodySmall,
                                color = CoralLive,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = { onConfirmRegister(teamName, gamerTag, division) },
                            enabled = !isSubmitting &&
                                teamName.isNotBlank() &&
                                gamerTag.isNotBlank() &&
                                agreedRules &&
                                hasEnoughCoins,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VoltGreen,
                                contentColor = ObsidianBg
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("confirm_tournament_registration_button")
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = ObsidianBg,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Signing Up...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Rounded.HowToReg, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (entryFee > 0L) "Confirm Sign-Up ($entryFee Coins)" else "Confirm Free Sign-Up",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ObsidianSurfaceElevated
                ) {
                    Text(
                        text = when {
                            countdown.phase == TournamentPhase.COMPLETED -> "This tournament has finished."
                            countdown.phase == TournamentPhase.LIVE -> "Registration closed — the tournament is currently live."
                            else -> "Registration is full (${tournament.registeredIds.size}/${tournament.maxTeams} teams)."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            if (isOrganizer && tournament.tournamentState == "upcoming") {
                FilledTonalButton(
                    onClick = { onUpdateTournamentState("active") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("start_tournament_live_button")
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Launch Event Live Now")
                }
            }

            // Registered Roster
            Column(
                modifier = Modifier.testTag("tournament_contenders_section"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Registered Contenders (${tournament.registeredIds.size}/${tournament.maxTeams})",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                if (registrations.isEmpty()) {
                    Text(
                        text = "${tournament.registeredIds.size} team(s) entered so far.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                } else {
                    registrations.forEach { entry ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianSurfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (entry.userId == currentUserId) VoltGreen.copy(alpha = 0.5f) else ObsidianOutline
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.teamName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                    Text("${entry.playerName} • ${entry.gamerTag}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                Surface(shape = RoundedCornerShape(8.dp), color = CyanContainer) {
                                    Text(
                                        text = entry.division,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyanNeon,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateTournamentDialog(
    onDismiss: () -> Unit,
    onCreate: (
        title: String,
        game: String,
        prize: Long,
        entry: Long,
        teams: Long,
        schedule: String,
        startsAtMillis: Long,
        formatLabel: String,
        rulesSummary: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("Mobilive Weekend Cup") }
    var game by remember { mutableStateOf("Minecraft") }
    var prize by remember { mutableStateOf("1000") }
    var entryFee by remember { mutableLongStateOf(0L) }
    var maxTeams by remember { mutableLongStateOf(16L) }
    var formatLabel by remember { mutableStateOf("Squad 4v4") }
    var selectedPreset by remember { mutableStateOf(ArcadeCatalog.tournamentStartPresets[1]) }
    var rulesSummary by remember { mutableStateOf("Fair play required • Check in 15 min before kickoff.") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Host Gaming Tournament") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
                    label = { Text("Tournament Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("create_tournament_title_input")
                )
                Text("Game", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ArcadeCatalog.popularGames, key = { it }) { g ->
                        FilterChip(selected = game == g, onClick = { game = g }, label = { Text(g) })
                    }
                }
                Text("Event Starts", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ArcadeCatalog.tournamentStartPresets, key = { it.id }) { preset ->
                        FilterChip(
                            selected = selectedPreset.id == preset.id,
                            onClick = { selectedPreset = preset },
                            label = { Text(preset.label) }
                        )
                    }
                }
                Text("Format", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ArcadeCatalog.tournamentFormats, key = { it }) { fmt ->
                        FilterChip(selected = formatLabel == fmt, onClick = { formatLabel = fmt }, label = { Text(fmt) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = prize,
                        onValueChange = { prize = it.filter(Char::isDigit).take(6) },
                        label = { Text("Prize Coins") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text("Entry Fee (Coins)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf(0L, 50L, 100L, 250L), key = { it }) { fee ->
                        FilterChip(
                            selected = entryFee == fee,
                            onClick = { entryFee = fee },
                            label = { Text(if (fee == 0L) "Free" else "$fee") }
                        )
                    }
                }
                Text("Max Teams", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(8L, 16L, 32L).forEach { count ->
                        FilterChip(selected = maxTeams == count, onClick = { maxTeams = count }, label = { Text("$count") })
                    }
                }
                OutlinedTextField(
                    value = rulesSummary,
                    onValueChange = { rulesSummary = it.take(240) },
                    label = { Text("Rules & Check-in Note") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val startsAt = System.currentTimeMillis() + selectedPreset.offsetMillis
                    onCreate(
                        title,
                        game,
                        prize.toLongOrNull() ?: 500L,
                        entryFee,
                        maxTeams,
                        selectedPreset.label,
                        startsAt,
                        formatLabel,
                        rulesSummary
                    )
                },
                enabled = title.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = GoldCoin, contentColor = ObsidianBg),
                modifier = Modifier.testTag("confirm_create_tournament_button")
            ) {
                Text("Launch Event", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
