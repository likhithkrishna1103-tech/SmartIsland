# 🔍 SmartIsland — Code Audit & Fixes Report

> **Repository:** [agupta07505/SmartIsland](https://github.com/agupta07505/SmartIsland)  
> **Branch reviewed:** `dev` (merged into `main`)  
> **Date:** 2026-07-08  
> **Auditor:** Arena.ai Agent Mode

---

## Table of Contents

1. [Critical Bugs](#1-critical-bugs)
2. [Dark Mode / Theme Issues](#2-dark-mode--theme-issues)
3. [Deprecated API Usage](#3-deprecated-api-usage)
4. [Reflection & Private API Risks](#4-reflection--private-api-risks)
5. [Memory & Performance Issues](#5-memory--performance-issues)
6. [Code Duplication](#6-code-duplication)
7. [Architecture & Design Issues](#7-architecture--design-issues)
8. [CI / Build Pipeline Issues](#8-ci--build-pipeline-issues)
9. [Testing Gaps](#9-testing-gaps)
10. [Code Quality & Maintainability](#10-code-quality--maintainability)
11. [Summary & Priority Matrix](#11-summary--priority-matrix)

---

## 1. Critical Bugs

### 1.1 `shouldIgnoreForSmartIsland` — Misleading Name + Logic Gap

**File:** `SmartIslandNotificationListenerService.kt` (line ~209)

```kotlin
internal fun shouldIgnoreForSmartIsland(sbn: StatusBarNotification): Boolean {
    val notification = sbn.notification
    if (!isHighPriorityNotification(sbn, notification)) return false
    return notification.isSystemLevelCategory() || sbn.packageName.isSystemLevelPackage()
}
```

**Problem:**  
The method name says "should ignore" but it's actually used to filter system-level high-priority notifications (to suppress heads-up popups). Low-priority system notifications are **never** filtered, which could lead to unexpected system status notifications appearing in the island. The naming is confusing and the logic could miss edge cases.

**Fix:**  
Rename to `shouldSuppressSystemHeadsUp` and add a low-priority system notification filter:

```kotlin
internal fun shouldSuppressFromIsland(sbn: StatusBarNotification): Boolean {
    val notification = sbn.notification
    // Always suppress system-level categories regardless of priority
    if (notification.isSystemLevelCategory()) return true
    // Suppress high-priority notifications from known system packages
    if (isHighPriorityNotification(sbn, notification) && sbn.packageName.isSystemLevelPackage()) return true
    return false
}
```

---

### 1.2 Battery Charging — No "Full" or "Not Charging" State Handling

**File:** `SystemEventReceiver.kt` (line ~43)

```kotlin
Intent.ACTION_BATTERY_CHANGED -> {
    if (isCharging(intent) && isCurrentlyCharging) {
        updateBatteryIsland(context, autoExpand = false)
    }
}
```

**Problem:**  
When the battery reaches 100%, `BATTERY_STATUS_FULL` is returned, but the UI always shows `"Charging"` as the title regardless. There is no distinction between actively charging, fully charged, or charging-slowly states.

**Fix:**

```kotlin
private fun updateBatteryIsland(context: Context, autoExpand: Boolean) {
    val batteryStatus: Intent? =
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
    val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
    val batteryPct = (level * 100 / scale.toFloat()).toInt()
    val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

    if (!autoExpand && batteryPct == lastBatteryPct) return
    lastBatteryPct = batteryPct

    val title = when (status) {
        BatteryManager.BATTERY_STATUS_FULL -> "Fully Charged"
        else -> "Charging"
    }

    notificationRepository.postNotification(
        IslandNotification(
            key = "system_battery",
            packageName = "com.android.systemui",
            appName = "System",
            title = title,
            text = "$batteryPct%",
            mode = IslandMode.Battery,
            icon = null,
            timeMillis = System.currentTimeMillis()
        ),
        autoExpand = autoExpand
    )
}
```

---

### 1.3 `openCurrentNotificationInFloatingWindow` — Silent Failure on Unsupported Devices

**File:** `SmartIslandOverlayService.kt` (line ~403)

**Problem:**  
When `setLaunchWindowingMode` fails via reflection, the code shows a Toast but **still proceeds** to create an `ActivityOptions` with `setLaunchBounds()`, which may cause a crash or unexpected behavior on some devices. The early return is missing.

**Fix:**

```kotlin
val setModeResult = runCatchingLogged(TAG, "Failed to invoke setLaunchWindowingMode") {
    val method = options.javaClass.getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType)
    method.invoke(options, 5)
}
if (setModeResult == null) {
    Toast.makeText(this, "Freeform windowing mode is not supported on this device.", Toast.LENGTH_SHORT).show()
    collapse()  // ← ADD THIS
    return      // ← ADD THIS: bail out early
}
```

---

### 1.4 `sendFirstAction` — Music Actions Remove Notification Prematurely

**File:** `IslandExpandedContent.kt` (line ~813)

```kotlin
if (this.mode != IslandMode.Music) {
    app.notificationRepository.removeNotification(this.key)
    app.notificationRepository.sendCommand(...)
}
```

**Problem:**  
When the user taps Previous/Next on a music notification, the notification is **not** removed (correct), but the action still calls `app.notificationRepository.resetTimer()`. If the action triggers a new notification with a different key (some music apps do this), the old notification lingers in the list. There's no cleanup for stale music notifications.

**Fix:** Add stale notification cleanup in `SmartIslandNotificationListenerService`:

```kotlin
// In handleNotificationPosted, after posting the new notification:
if (mode == IslandMode.Music) {
    val existing = notificationRepository.notifications.value
    existing.filter { it.packageName == sbn.packageName && it.key != sbn.key }
        .forEach { notificationRepository.removeNotification(it.key) }
}
```

---

## 2. Dark Mode / Theme Issues

### 2.1 Hardcoded `Color.White` Card Backgrounds Break Dark Mode

Multiple section composables hardcode `Color.White` for card backgrounds, ignoring the `MaterialTheme.colorScheme.surface` defined in `Theme.kt`.

**Affected Files & Lines:**

| File | Line(s) | Issue |
|------|---------|-------|
| `PermissionCard.kt` | `containerColor = Color.White` | Cards invisible in dark mode |
| `PermissionsSection.kt` | `containerColor = Color.White` | System warning card invisible |
| `PositionsSection.kt` | `containerColor = Color.White` | Position controls card invisible |
| `SupportSection.kt` | `containerColor = Color.White` | All support items invisible |
| `AboutSection.kt` | `containerColor = Color.White` (×2) | About & contact cards invisible |

**Fix:** Replace all instances:

```kotlin
// FROM:
colors = CardDefaults.cardColors(containerColor = Color.White)

// TO:
colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
```

Also update the text colors in these sections to use `MaterialTheme.colorScheme.onSurface`:

```kotlin
// FROM:
color = Color(0xFF344054)

// TO:
color = MaterialTheme.colorScheme.onSurface
```

---

### 2.2 Header Section — Hardcoded Dark Text on Transparent Background

**File:** `HeaderSection.kt`

```kotlin
color = Color(0xFF101828)  // "Smart Island" title
color = Color(0xFF667085)  // subtitle
```

**Fix:**

```kotlin
color = MaterialTheme.colorScheme.primary   // title
color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)  // subtitle
```

---

## 3. Deprecated API Usage

### 3.1 `getParcelable<T>(key)` — Deprecated on API 33+

**File:** `SmartIslandNotificationListenerService.kt` (lines ~184, ~264)

```kotlin
extras.getParcelable<MediaSession.Token>(Notification.EXTRA_MEDIA_SESSION)
```

**Fix:**

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java)
} else {
    @Suppress("DEPRECATION")
    extras.getParcelable(Notification.EXTRA_MEDIA_SESSION)
}
```

---

### 3.2 `LocalLifecycleOwner` — Deprecated in Compose 1.7+

**File:** `SmartIslandHomeScreen.kt` (line ~94)

```kotlin
val lifecycleOwner = LocalLifecycleOwner.current
```

**Fix:** Use `androidx.lifecycle.compose.LocalLifecycleOwner`:

```kotlin
import androidx.lifecycle.compose.LocalLifecycleOwner
```

---

## 4. Reflection & Private API Risks

### 4.1 `OnComputeInternalInsetsListener` — Private Android API

**File:** `SmartIslandOverlayService.kt` (line ~171)

**Problem:** Uses reflection to access `android.view.ViewTreeObserver$OnComputeInternalInsetsListener`, which is a hidden/internal API. This will break on future Android versions when the API surface changes.

**Fix:**
- Add a try-catch with a graceful fallback (already partially done)
- Consider using `WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL` with a custom touchable region as an alternative
- Add a version check and fallback for Android 15+:

```kotlin
if (Build.VERSION.SDK_INT >= 35) {
    // Use alternative touch passthrough mechanism
    // Consider FLAG_NOT_TOUCH_MODAL with explicit touch delegation
}
```

---

### 4.2 `setLaunchWindowingMode` — Private API

**File:** `SmartIslandOverlayService.kt` (line ~407)

**Problem:** Relies on reflection to call `ActivityOptions.setLaunchWindowingMode(5)`, which is a hidden API. On Android 14+ this may be blocked by the hidden API blacklist.

**Fix:** Add the method to ProGuard keep rules (already done) but also add runtime version gating:

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
    // Try freeform mode
} else {
    Toast.makeText(this, "Floating window requires Android 7+", Toast.LENGTH_SHORT).show()
    return
}
```

---

## 5. Memory & Performance Issues

### 5.1 App Icon Bitmap Not Cached

**File:** `SmartIslandNotificationListenerService.kt` (line ~226)

```kotlin
private fun loadAppIconBitmap(packageName: String): Bitmap? {
    return runCatching {
        val drawable = packageManager.getApplicationIcon(packageName)
        drawable.toBitmap(width = 96, height = 96)
    }.getOrNull()
}
```

**Problem:** Creates a **new Bitmap** for every notification received. If WhatsApp sends 10 notifications, 10 identical bitmaps are allocated.

**Fix:** Add an LRU cache:

```kotlin
private val iconCache = android.util.LruCache<String, Bitmap>(50)

private fun loadAppIconBitmap(packageName: String): Bitmap? {
    iconCache.get(packageName)?.let { return it }
    return runCatching {
        val drawable = packageManager.getApplicationIcon(packageName)
        drawable.toBitmap(width = 96, height = 96).also { iconCache.put(packageName, it) }
    }.getOrNull()
}
```

---

### 5.2 Infinite Animations Run Unconditionally

**File:** `IslandCollapsedContent.kt` (lines ~75–90)

```kotlin
val infiniteTransition = rememberInfiniteTransition(label = "batteryPulse")
val pulseScale by infiniteTransition.animateFloat(...)
val rotationAngle by infiniteTransition.animateFloat(...)
```

**Problem:** These infinite animations run even when the mode is NOT `Battery`, wasting CPU/GPU cycles.

**Fix:** Only create the transition when in Battery mode:

```kotlin
val infiniteTransition = if (mode == IslandMode.Battery) {
    rememberInfiniteTransition(label = "batteryPulse")
} else null

val pulseScale = infiniteTransition?.animateFloat(...) ?: mutableStateOf(1f)
val rotationAngle = infiniteTransition?.animateFloat(...) ?: mutableStateOf(0f)
```

---

### 5.3 `WavyMusicSeekBar` Phase Animation Runs When Not Visible

**File:** `WavyMusicSeekBar.kt` (line ~50)

**Problem:** The `LaunchedEffect` that animates `phase` runs in an infinite loop even when the composable is off-screen or not in Music mode.

**Fix:** Tie the animation to visibility:

```kotlin
if (isPlaying) {
    LaunchedEffect(Unit) {
        // ... existing loop
    }
}  // ← Already guarded, but the Canvas still runs the path computation
// Consider adding: if progress == 0f && !isPlaying return@Canvas early
```

---

## 6. Code Duplication

### 6.1 `DottedRing` Composable — Duplicated in Two Files

**Files:**
- `IslandCollapsedContent.kt` (line ~241)
- `IslandExpandedContent.kt` (line ~874)

Both files contain an **identical** private `DottedRing` composable function.

**Fix:** Extract to a shared file `ui/components/DottedRing.kt`:

```kotlin
// ui/components/DottedRing.kt
package com.agupta07505.smartisland.ui.components

@Composable
fun DottedRing(
    progress: Float,
    rotationAngle: Float,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF10B981)
) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val dotRadius = 1.2.dp.toPx()
        val numDots = 16
        val activeDotsCount = (numDots * progress).toInt()
        for (i in 0 until numDots) {
            val angle = (-90f + rotationAngle + i * 360f / numDots) * (Math.PI / 180f)
            val x = (center.x + radius * Math.cos(angle)).toFloat()
            val y = (center.y + radius * Math.sin(angle)).toFloat()
            val isActive = i < activeDotsCount
            val dotColor = if (isActive) color else Color(0x33FFFFFF)
            drawCircle(color = dotColor, radius = dotRadius, center = Offset(x, y))
        }
    }
}
```

---

### 6.2 `AboutItem` Composable — Duplicated in SupportSection

**Files:**
- `SupportSection.kt` (line ~105) — private `AboutItem`
- `AboutSection.kt` (line ~150) — private `AboutItem`

Both have nearly identical `AboutItem` composables.

**Fix:** Extract to `ui/components/ClickableRowItem.kt` and share.

---

## 7. Architecture & Design Issues

### 7.1 No ViewModel Layer

**Problem:** All state management lives in `SmartIslandOverlayService` using raw `MutableStateFlow`. There's no `ViewModel` intermediary, making the code hard to test and violating MVVM.

**Fix:** Create an `IslandViewModel`:

```kotlin
class IslandViewModel(
    private val settingsRepository: SmartIslandSettingsRepository,
    private val notificationRepository: SmartIslandNotificationRepository
) : ViewModel() {
    val settings = settingsRepository.settings.stateIn(...)
    val notifications = notificationRepository.notifications.stateIn(...)
    val expanded = MutableStateFlow(false)
    // ...
}
```

---

### 7.2 NotificationRepository is a Concrete Class

**Problem:** `SmartIslandNotificationRepository` is instantiated directly in `SmartIslandApp`, making it impossible to swap for testing.

**Fix:** Define an interface:

```kotlin
interface INotificationRepository {
    val notifications: StateFlow<List<IslandNotification>>
    fun postNotification(notification: IslandNotification, autoExpand: Boolean)
    fun removeNotification(key: String)
    // ...
}
```

---

### 7.3 Silent Exception Swallowing with `runCatching`

**Problem:** Over 30 uses of `runCatching` silently swallow exceptions across the codebase. This makes debugging production issues nearly impossible.

**Fix:** Use `runCatchingLogged` (already exists in `LogUtils.kt`) consistently, or add a centralized error reporter:

```kotlin
// Replace bare runCatching with:
runCatchingLogged(TAG, "Failed to handle notification") {
    // risky operation
}
```

---

## 8. CI / Build Pipeline Issues

### 8.1 Tests Not Run in CI

**File:** `.github/workflows/android.yml`

**Problem:** The CI pipeline runs `assembleDebug` and `assembleRelease` but **never runs unit tests**.

**Fix:** Add a test step:

```yaml
- name: Run Unit Tests
  run: ./gradlew testDebugUnitTest
```

---

### 8.2 No Dependency Caching Verification

**Problem:** While `cache: gradle` is set in `setup-java`, there's no verification that the cache is actually being used, and the Gradle wrapper JAR is committed to the repo (65KB) — if it changes, the cache may become stale.

---

### 8.3 No Lint Check in CI

**Problem:** `lint { abortOnError = true; warningsAsErrors = true }` is configured in `build.gradle.kts`, but `./gradlew lint` is never explicitly run in CI. Lint only runs as part of `assembleRelease` if configured.

**Fix:** Add explicit lint step:

```yaml
- name: Run Lint
  run: ./gradlew lintDebug
```

---

## 9. Testing Gaps

### 9.1 No Tests for `SmartIslandNotificationRepository`

The core notification repository (posting, removing, auto-expand events, demo mode) has **zero test coverage**.

**Fix:** Add tests:

```kotlin
class SmartIslandNotificationRepositoryTest {
    @Test
    fun `postNotification adds new notification`() = runTest {
        val repo = SmartIslandNotificationRepository()
        val notif = IslandNotification(key = "test", packageName = "com.test", ...)
        repo.postNotification(notif)
        assertEquals(1, repo.notifications.value.size)
    }

    @Test
    fun `removeNotification removes by key`() = runTest {
        val repo = SmartIslandNotificationRepository()
        repo.postNotification(IslandNotification(key = "k1", ...))
        repo.postNotification(IslandNotification(key = "k2", ...))
        repo.removeNotification("k1")
        assertEquals(1, repo.notifications.value.size)
        assertEquals("k2", repo.notifications.value[0].key)
    }
}
```

---

### 9.2 No Instrumented/UI Tests

There are no `androidTest` source sets at all. The overlay UI, home screen, and notification handling have zero instrumented test coverage.

---

### 9.3 `NotificationPriorityTest` — Fragile `spyk` Usage

**File:** `NotificationPriorityTest.kt`

Using `spyk<SmartIslandNotificationListenerService>()` creates a real service instance, which may fail on device-less unit test environments.

**Fix:** Extract the filtering logic into a pure utility class that doesn't depend on Android framework:

```kotlin
object NotificationFilter {
    fun shouldSuppress(notification: Notification, packageName: String, isHighPriority: Boolean): Boolean { ... }
}
```

---

## 10. Code Quality & Maintainability

### 10.1 Magic Numbers Scattered Throughout

| File | Value | Meaning |
|------|-------|---------|
| `SmartIslandOverlayService.kt` | `5000` | Auto-collapse timer (ms) |
| `SmartIslandOverlayService.kt` | `8105` | Foreground notification ID |
| `SmartIslandNotificationListenerService.kt` | `96`, `128` | Bitmap sizes |
| `IslandOverlayView.kt` | `0.95f` | Expanded width ratio |
| `IslandOverlayView.kt` | `0.92f` | Collapsed content scale |
| `IslandOverlayView.kt` | `35f` | Swipe threshold |
| `SmartIslandOverlayService.kt` | `5` | Freeform windowing mode |

**Fix:** Extract to named constants:

```kotlin
companion object {
    const val AUTO_COLLAPSE_DELAY_MS = 5000L
    const val NOTIFICATION_ID = 8105
    const val ICON_BITMAP_SIZE = 96
    const val LARGE_ICON_BITMAP_SIZE = 128
    const val EXPANDED_WIDTH_RATIO = 0.95f
    const val SWIPE_THRESHOLD_DP = 35f
}
```

---

### 10.2 Oversized Files

| File | Lines | Recommendation |
|------|-------|----------------|
| `IslandExpandedContent.kt` | **1074** | Extract `MusicExpanded`, `BatteryExpanded`, `IncomingCallExpanded` to separate files |
| `SmartIslandOverlayService.kt` | **486** | Extract `OverlayIsland` composable and window management to separate classes |
| `SmartIslandNotificationListenerService.kt` | **359** | Extract media info extraction and system filtering to utility classes |
| `SmartIslandHomeScreen.kt` | **549** | Extract `SectionRow`, `SectionDetailScreen` to `ui/components/` |

---

### 10.3 Inconsistent Import Style

Some files use explicit imports (`import androidx.compose.foundation.layout.Box`), while others would benefit from grouping. The `DottedRing` Canvas uses fully qualified `Offset` references inline rather than importing them.

---

### 10.4 Missing `@OptIn` Annotations

`HorizontalPager` from Compose Foundation may require `@OptIn(ExperimentalFoundationApi::class)` on older Compose BOM versions. While BOM `2026.02.01` may have stabilized it, it's safer to annotate explicitly.

---

## 11. Summary & Priority Matrix

| Priority | Issue | Impact | Effort |
|----------|-------|--------|--------|
| 🔴 **P0** | Freeform window crash (§1.3) | Crash on unsupported devices | Low |
| 🔴 **P0** | No tests in CI (§8.1) | Bugs ship undetected | Low |
| 🟠 **P1** | Dark mode broken (§2.1) | Half the screens invisible in dark mode | Low |
| 🟠 **P1** | Deprecated `getParcelable` (§3.1) | Crash on API 33+ | Low |
| 🟠 **P1** | Icon bitmap memory leak (§5.1) | OOM on heavy notification traffic | Medium |
| 🟠 **P1** | Infinite animations waste CPU (§5.2) | Battery drain | Medium |
| 🟡 **P2** | Battery no "Full" state (§1.2) | UX confusion | Low |
| 🟡 **P2** | Code duplication `DottedRing` (§6.1) | Maintenance burden | Low |
| 🟡 **P2** | Silent exception swallowing (§7.3) | Hard to debug production | Medium |
| 🟡 **P2** | Missing repository tests (§9.1) | Regression risk | Medium |
| 🟢 **P3** | No ViewModel layer (§7.1) | Testability | High |
| 🟢 **P3** | Magic numbers (§10.1) | Readability | Low |
| 🟢 **P3** | Oversized files (§10.2) | Maintainability | Medium |

---

## Quick-Fix Checklist

```
☐ Fix freeform window early return (§1.3)
☐ Add unit test step to CI (§8.1)
☐ Replace Color.White with MaterialTheme.colorScheme.surface (§2.1)
☐ Fix deprecated getParcelable calls (§3.1)
☐ Add icon bitmap LRU cache (§5.1)
☐ Gate infinite animations behind mode check (§5.2)
☐ Extract DottedRing to shared component (§6.1)
☐ Add battery "Fully Charged" state (§1.2)
☐ Replace bare runCatching with runCatchingLogged (§7.3)
☐ Add SmartIslandNotificationRepository tests (§9.1)
☐ Add lint step to CI (§8.3)
☐ Extract magic numbers to constants (§10.1)
```

---

*Report generated by Arena.ai Agent Mode — [SmartIsland](https://github.com/agupta07505/SmartIsland)*
