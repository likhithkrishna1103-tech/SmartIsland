/*
 * Smart Island (2026)
 * Copyright Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.ui.sections

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.AppShortcutProvider
import com.agupta07505.smartisland.data.LaunchableApp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppShortcutsSection(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    val installedApps by produceState(initialValue = emptyList<LaunchableApp>(), context) {
        value = withContext(Dispatchers.IO) { AppShortcutProvider.installedApps(context) }
    }
    val usageAccess = AppShortcutProvider.hasUsageAccess(context)
    val filteredApps = remember(installedApps, query) {
        if (query.isBlank()) installedApps
        else installedApps.filter {
            it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Expanded island launcher", fontWeight = FontWeight.Bold)
            Text(
                "When there is no notification, opening the pill shows your shortcuts. Select up to 8 apps.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Recently used apps", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (usageAccess) "Fill free slots with apps you used recently"
                        else "Usage access is required for recent apps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.showRecentApps,
                    onCheckedChange = { enabled ->
                        if (enabled && !usageAccess) {
                            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        }
                        scope.launch { repository.setShowRecentApps(enabled) }
                    }
                )
            }
            if (!usageAccess) {
                OutlinedButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }) {
                    Text("Grant usage access")
                }
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    Text(
        "Selected ${settings.shortcutPackages.size}/8",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        label = { Text("Search apps") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))

    filteredApps.forEach { app ->
        val selected = app.packageName in settings.shortcutPackages
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = selected || settings.shortcutPackages.size < 8) {
                    val updated = if (selected) {
                        settings.shortcutPackages - app.packageName
                    } else {
                        settings.shortcutPackages + app.packageName
                    }
                    scope.launch { repository.setShortcutPackages(updated) }
                }
                .padding(vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = selected,
                enabled = selected || settings.shortcutPackages.size < 8,
                onCheckedChange = { checked ->
                    val updated = if (checked) {
                        settings.shortcutPackages + app.packageName
                    } else {
                        settings.shortcutPackages - app.packageName
                    }
                    scope.launch { repository.setShortcutPackages(updated) }
                }
            )
            Column {
                Text(app.label, fontWeight = FontWeight.Medium)
                Text(
                    app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
