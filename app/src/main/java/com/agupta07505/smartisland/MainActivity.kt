/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import com.agupta07505.smartisland.ui.SmartIslandHomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = androidx.compose.ui.graphics.Color(0xFF111827),
                    secondary = androidx.compose.ui.graphics.Color(0xFF2563EB),
                    background = androidx.compose.ui.graphics.Color(0xFFF7F8FA),
                    surface = androidx.compose.ui.graphics.Color.White
                )
            ) {
                SmartIslandHomeScreen()
            }
        }
    }
}
