package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.PostComment
import com.example.data.SocialPost
import com.example.ui.theme.*

private val LiveRingBrush = Brush.linearGradient(listOf(CoralLive, GoldCoin, CyanNeon))
private val DefaultRingBrush = Brush.linearGradient(listOf(CyanNeon.copy(alpha = 0.7f), VioletArcade.copy(alpha = 0.7f)))

@Composable
fun AvatarFace(
    avatarStyle: String,
    photoUri: String = "",
    size: Dp = 44.dp,
    isLive: Boolean = false,
    showVerified: Boolean = false,
    modifier: Modifier = Modifier
) {
    val ringBrush = if (isLive) LiveRingBrush else DefaultRingBrush

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(width = if (isLive) 2.5.dp else 1.5.dp, brush = ringBrush, shape = CircleShape)
                .padding(3.dp)
                .clip(CircleShape)
                .background(ObsidianSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            if (photoUri.isNotBlank()) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = "Profile avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (avatarStyle == "cyber_visor") {
                AsyncImage(
                    model = R.drawable.img_avatar_neon,
                    contentDescription = "Cyber visor avatar face",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Expressive custom Canvas Profile Face presets
                val (bgColor, accentColor) = when (avatarStyle) {
                    "pixel_knight" -> Color(0xFF153B30) to VoltGreen
                    "coral_ninja" -> Color(0xFF47162A) to CoralLive
                    "volt_bot" -> Color(0xFF162A4D) to CyanNeon
                    "gold_crown" -> Color(0xFF40300A) to GoldCoin
                    else -> Color(0xFF231942) to VioletArcade
                }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(color = bgColor)
                    val cx = this.size.width / 2f
                    val cy = this.size.height / 2f
                    // Head silhouette
                    drawCircle(
                        color = accentColor.copy(alpha = 0.28f),
                        radius = this.size.minDimension * 0.36f,
                        center = Offset(cx, cy)
                    )
                    // Visor / Eyes
                    drawRoundRect(
                        color = accentColor,
                        topLeft = Offset(cx - this.size.width * 0.24f, cy - this.size.height * 0.09f),
                        size = Size(this.size.width * 0.48f, this.size.height * 0.14f)
                    )
                    // Smile / Mouth accent
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.85f),
                        topLeft = Offset(cx - this.size.width * 0.12f, cy + this.size.height * 0.14f),
                        size = Size(this.size.width * 0.24f, this.size.height * 0.06f)
                    )
                }
            }
        }

        if (showVerified) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(16.dp),
                shape = CircleShape,
                color = CyanNeon
            ) {
                Icon(
                    imageVector = Icons.Rounded.Verified,
                    contentDescription = "Verified badge",
                    tint = ObsidianBg,
                    modifier = Modifier.padding(2.dp)
                )
            }
        }
    }
}

@Composable
fun VerifiedBadgeChip(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = CyanContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Verified,
                contentDescription = "Verified creator",
                tint = CyanNeon,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Verified",
                style = MaterialTheme.typography.labelSmall,
                color = CyanNeon
            )
        }
    }
}

@Composable
fun CoinPill(coins: Long, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(50),
        color = GoldContainer,
        border = BorderStroke(1.dp, GoldCoin.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MonetizationOn,
                contentDescription = "Gift Coins",
                tint = GoldCoin,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = coins.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = GoldCoin,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LevelBadge(level: Long, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = VoltGreenContainer,
        border = BorderStroke(1.dp, VoltGreen.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = "Level",
                tint = VoltGreen,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = "LV $level",
                style = MaterialTheme.typography.labelSmall,
                color = VoltGreen,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LivePulseTag(label: String = "LIVE", modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = CoralLive
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LanInstructionBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = ObsidianSurfaceElevated,
        border = BorderStroke(1.dp, VoltGreen.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.WifiTethering,
                    contentDescription = "LAN Guide",
                    tint = VoltGreen
                )
                Text(
                    text = "LAN & Minecraft Multiplayer Guide",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ObsidianBg.copy(alpha = 0.65f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Rounded.CellTower, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
                    Text(
                        text = stringResource(R.string.lan_hosting_instruction),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ObsidianBg.copy(alpha = 0.65f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = VoltGreen, modifier = Modifier.size(18.dp))
                    Text(
                        text = stringResource(R.string.lan_joining_instruction),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyArcadeState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ObsidianOutline)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CyanNeon,
                modifier = Modifier.size(40.dp)
            )
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            if (actionLabel != null && onAction != null) {
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = ObsidianBg)
                ) {
                    Text(actionLabel, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ReportUserDialog(
    targetHandle: String,
    onDismiss: () -> Unit,
    onSubmitReport: (reason: String, details: String) -> Unit
) {
    val reasons = listOf("Harassment or Toxicity", "Cheating / Exploits", "Inappropriate Stream", "Spam / Scam")
    var selectedReason by remember { mutableStateOf(reasons.first()) }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report @$targetHandle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedReason = reason }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedReason == reason, onClick = { selectedReason = reason })
                        Text(reason, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it.take(300) },
                    label = { Text("Additional context (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmitReport(selectedReason, details) },
                colors = ButtonDefaults.buttonColors(containerColor = CoralLive, contentColor = Color.White),
                modifier = Modifier.testTag("submit_report_button")
            ) {
                Text("Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    post: SocialPost,
    comments: List<PostComment>,
    onDismiss: () -> Unit,
    onSendComment: (String) -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Comments on @${post.authorHandle}",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            if (comments.isEmpty()) {
                Text(
                    text = "No comments yet. Be the first to reply!",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(comments, key = { it.id }) { comment ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianSurfaceElevated,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = comment.authorName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CyanNeon,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = comment.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
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
                    value = commentText,
                    onValueChange = { commentText = it.take(300) },
                    placeholder = { Text("Write a comment...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input"),
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            onSendComment(commentText)
                            commentText = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CyanNeon)
                        .testTag("send_comment_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Send comment",
                        tint = ObsidianBg
                    )
                }
            }
        }
    }
}
