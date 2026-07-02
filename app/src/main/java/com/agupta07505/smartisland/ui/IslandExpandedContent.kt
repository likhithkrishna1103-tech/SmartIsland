package com.agupta07505.smartisland.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.IslandNotificationAction

@Composable
fun IslandExpandedContent(
    mode: IslandMode,
    notification: IslandNotification?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (mode) {
            IslandMode.Notification -> NotificationExpanded(notification)
            IslandMode.IncomingCall -> IncomingCallExpanded(notification)
            IslandMode.Music -> MusicExpanded(notification)
            IslandMode.Empty -> EmptyExpanded()
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.Black)
        )
    }
}

@Composable
private fun EmptyExpanded() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 18.dp, top = 40.dp, end = 18.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Smart Island", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text("Ready for notifications", color = Color(0xFFB7C0CA), fontSize = 13.sp)
    }
}

@Composable
private fun NotificationExpanded(notification: IslandNotification?) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 18.dp, top = 40.dp, end = 18.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = notification?.icon
        if (icon != null) {
            Image(
                bitmap = icon.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2563EB)),
                contentAlignment = Alignment.Center
            ) {
                Text(notification?.appName?.firstOrNull()?.uppercase() ?: "S", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notification?.title?.takeIf { it.isNotBlank() } ?: notification?.appName ?: "Notification",
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = notification?.text?.takeIf { it.isNotBlank() } ?: "New activity",
                color = Color(0xFFD5DAE0),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp
            )
            if (!notification?.actions.isNullOrEmpty()) {
                Text(
                    text = notification.actions.take(2).joinToString("  |  "),
                    color = Color(0xFF9CC7FF),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = notification?.let { formatNotificationTime(it.timeMillis) } ?: "",
            color = Color(0xFFB7C0CA),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun IncomingCallExpanded(notification: IslandNotification?) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 18.dp, top = 40.dp, end = 12.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = notification?.title?.takeIf { it.isNotBlank() }
                ?: notification?.text?.takeIf { it.isNotBlank() }
                ?: "Incoming call",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        CircleActionButton(
            color = Color(0xFFE11D48),
            icon = Icons.Rounded.Close,
            onClick = { notification.sendFirstAction("decline", "reject", "hang", "end") }
        )
        CircleActionButton(
            color = Color(0xFF79C943),
            icon = Icons.Rounded.Call,
            onClick = { notification.sendFirstAction("answer", "accept") }
        )
    }
}

@Composable
private fun MusicExpanded(notification: IslandNotification?) {
    val positionMs = notification?.mediaPositionMs
    val durationMs = notification?.mediaDurationMs
    
    var livePositionMs by remember(positionMs, notification?.mediaIsPlaying) {
        mutableStateOf(positionMs)
    }

    if (notification?.mediaIsPlaying == true && positionMs != null) {
        LaunchedEffect(notification) {
            val startTime = android.os.SystemClock.elapsedRealtime()
            val startPosition = positionMs
            while (true) {
                val elapsed = android.os.SystemClock.elapsedRealtime() - startTime
                livePositionMs = startPosition + elapsed
                kotlinx.coroutines.delay(500)
            }
        }
    }

    val progress = when {
        durationMs != null && durationMs > 0 && livePositionMs != null ->
            (livePositionMs!!.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        notification?.progressMax?.let { it > 0 } == true ->
            (notification.progress.toFloat() / notification.progressMax.toFloat()).coerceIn(0f, 1f)
        else -> 0f
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 18.dp, top = 40.dp, end = 18.dp, bottom = 10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            val artwork = notification?.largeIcon ?: notification?.icon
            if (artwork != null) {
                Image(
                    bitmap = artwork.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFF6B9A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    notification?.title?.takeIf { it.isNotBlank() } ?: "Song",
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    notification?.text?.takeIf { it.isNotBlank() } ?: notification?.appName ?: "Artist",
                    color = Color(0xFFD5DAE0),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(formatDuration(livePositionMs), color = Color.White, fontSize = 10.sp)
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.weight(1f).height(3.dp), color = Color.White, trackColor = Color(0xFF667085))
            Text(formatDuration(durationMs), color = Color.White, fontSize = 10.sp)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { notification.sendFirstAction("previous", "prev", "rewind") }) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = null, tint = Color.White)
            }
            IconButton(onClick = { notification.sendFirstAction("play", "pause", "resume") }) {
                Icon(
                    if (notification?.mediaIsPlaying == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
            IconButton(onClick = { notification.sendFirstAction("next", "skip", "forward") }) {
                Icon(Icons.Rounded.SkipNext, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun CircleActionButton(
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
    }
}

private fun IslandNotification?.sendFirstAction(vararg keywords: String) {
    val action = this?.actionIntents?.firstOrNull { action ->
        keywords.any { keyword -> action.title.contains(keyword, ignoreCase = true) }
    } ?: return
    runCatching { action.pendingIntent?.send() }
}

private fun formatDuration(valueMs: Long?): String {
    val totalSeconds = valueMs?.takeIf { it >= 0 }?.div(1000) ?: return "--:--"
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
