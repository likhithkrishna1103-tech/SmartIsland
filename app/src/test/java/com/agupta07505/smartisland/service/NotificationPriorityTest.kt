/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.app.Notification
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.service.notification.StatusBarNotification
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPriorityTest {

    @Test
    fun testShouldIgnoreForSmartIslandLowPriority() {
        val service = spyk<SmartIslandNotificationListenerService>()
        val sbn = mockk<StatusBarNotification>()
        val notification = mockk<Notification>()
        every { sbn.notification } returns notification
        
        // Mock isHighPriorityNotification helper to return false
        every { service.isHighPriorityNotification(sbn, notification) } returns false

        // Low priority notification should NOT be ignored (it returns false early)
        assertFalse(service.shouldIgnoreForSmartIsland(sbn))
    }

    @Test
    fun testShouldIgnoreForSmartIslandSystemCategory() {
        val service = spyk<SmartIslandNotificationListenerService>()
        val sbn = mockk<StatusBarNotification>()
        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_SYSTEM
        every { sbn.notification } returns notification
        every { sbn.packageName } returns "com.example.app"

        // Mock isHighPriorityNotification helper to return true
        every { service.isHighPriorityNotification(sbn, notification) } returns true

        // Notification with high priority and system category should be ignored
        assertTrue(service.shouldIgnoreForSmartIsland(sbn))
    }

    @Test
    fun testShouldIgnoreForSmartIslandSystemPackage() {
        val service = spyk<SmartIslandNotificationListenerService>()
        val sbn = mockk<StatusBarNotification>()
        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_MESSAGE
        every { sbn.notification } returns notification
        every { sbn.packageName } returns "com.android.systemui" // in SYSTEM_LEVEL_PACKAGES

        // Mock isHighPriorityNotification helper to return true
        every { service.isHighPriorityNotification(sbn, notification) } returns true

        // Notification with high priority and system level package name should be ignored
        assertTrue(service.shouldIgnoreForSmartIsland(sbn))
    }

    @Test
    fun testShouldIgnoreForSmartIslandSystemFlagPackage() {
        val service = spyk<SmartIslandNotificationListenerService>()
        val sbn = mockk<StatusBarNotification>()
        val notification = mockk<Notification>()
        notification.category = Notification.CATEGORY_MESSAGE
        every { sbn.notification } returns notification
        every { sbn.packageName } returns "com.android.settings" // also in SYSTEM_LEVEL_PACKAGES

        // Mock isHighPriorityNotification helper to return true
        every { service.isHighPriorityNotification(sbn, notification) } returns true

        assertTrue(service.shouldIgnoreForSmartIsland(sbn))
    }
}
