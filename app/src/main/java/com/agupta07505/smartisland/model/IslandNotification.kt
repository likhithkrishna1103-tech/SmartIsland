package com.agupta07505.smartisland.model

import android.app.PendingIntent
import android.graphics.Bitmap

data class IslandNotification(
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timeMillis: Long,
    val icon: Bitmap? = null,
    val largeIcon: Bitmap? = null,
    val actions: List<String> = emptyList(),
    val actionIntents: List<IslandNotificationAction> = emptyList(),
    val category: String? = null,
    val progress: Int = 0,
    val progressMax: Int = 0,
    val mediaPositionMs: Long? = null,
    val mediaDurationMs: Long? = null,
    val mediaIsPlaying: Boolean = false
)

data class IslandNotificationAction(
    val title: String,
    val pendingIntent: PendingIntent?
)
