# Changelog

All notable changes to Smart Island should be documented in this file.

The format is inspired by Keep a Changelog, and this project uses the GNU General Public License v3.0.

## [2.1.1] - 2026-07-07

### Added
- **Battery Demo Button**: Added a dedicated "Battery" button to the Quick Test controls on the home screen to preview and test the charging island mode.
- **Unit Tests**: Added unit tests to verify `shouldIgnoreForSmartIsland` rules for high-priority non-system apps and to check `Notification.toIslandMode()` mapping with mixed-case media actions (e.g. `"PAUSE"`, `"Next Track"`).

### Changed
- **Battery Charging Updates**: Refactored `SystemEventReceiver` to only auto-expand the battery island on charger connection (`ACTION_POWER_CONNECTED`), and to update battery percentages silently without re-triggering expand and auto-collapse cycles.
- **Media Controller Resolution**: Improved media playback robustness by prioritizing active playing sessions (`PlaybackState.STATE_PLAYING`) when resolving media controllers by package name.
- **Better reflection diagnostics**: Switched key reflection-based API hooks (pass-through touch insets and freeform window launching) to use `runCatchingLogged` utility for easier debugging.

### Fixed
- **License Header Typo**: Corrected `GNU GPL v3License` to `GNU GPL v3 License` in workflow, configuration, ignore, and helper files.
- **Architecture Doc Tracked Status**: Removed `analysis.md` from `.gitignore` list to ensure the codebase analysis documentation remains fully tracked in Git.

## [2.1.0] - 2026-07-07

### Added

- **Centralized Notification Repository (`SmartIslandNotificationRepository`)**: Created to manage notification streams and commands flow reactively.
- **Dependency Injection**: Registered `SmartIslandApp` Application class to hold repositories singletons.
- **Material 3 Unified Theme**: Introduced day/night theme structure with DayNight Material3 scheme support.
- **Automated Tests**: Created JUnit/MockK test suites: `IslandModeMappingTest`, `NotificationPriorityTest`, `SmartIslandSettingsTest`.
- **Utility Modules**: Created isolated helper scripts `TimeUtils` and `LogUtils`.

### Changed

- **Modularized UI Structure**: Split monolithic screen `SmartIslandHomeScreen.kt` into clean components: `HeaderSection`, `PermissionsSection`, `PositionsSection`, `SupportSection`, and `AboutSection`.
- **Decoupled Architecture**: Migrated views `IslandOverlayView.kt` and `IslandExpandedContent.kt` from calling static service instances to reactively communicating via repositories.
- **Resource Maintainability**: Moved hardcoded screen UI strings into standard `strings.xml` resource tags.
- **Build Configurations**: Enabled ProGuard/R8 minification, resource shrinking, and strict lint checks in `build.gradle.kts`.

### Fixed

- **License Header Typo**: Corrected `GNU GPL v3License` to `GNU GPL v3 License` globally.
- **Theme Parent Reference**: Migrated parent configuration in `styles.xml` to `Theme.DeviceDefault.NoActionBar` to prevent XML resource linking compilation errors.

## [2.0.0] - 2026-07-05

### Added

- **Custom Wavy Music Seek Bar (`WavyMusicSeekBar`)**: Renders playing progress as a smooth, filled organic wave with custom wave animations that automatically freeze when paused, and flat damping at layout boundaries. Includes a circular thumb for direct drag-to-seek support.
- **Enhanced Player Controls**: Integrated a Song Like button (with outline/fill favorite heart status) on the left side of the skip-back button, and a Repeat/Loop button on the right side of the skip-forward button.
- **Home Screen Dashboard Reorganization**: Redesigned the main menu into neat topic cards: Permissions, Positions, Support & Feedback, and About.
- **Dashboard Slide Transitions**: Implemented slide-in/slide-out horizontal enter/exit animations using Compose `AnimatedContent` for menu subtopics, complete with a system back handler (`BackHandler`) to reverse the slides.

### Changed

- **App Logo Header**: Replaced the visual pill visual placeholder in the home screen header with the actual application icon embedded inside a sleek black background card, adjusted to a filled 60dp icon size.
- **Visual Spacing**: Shifted dashboard and category headers down by applying spacious 36dp top paddings to achieve a modern, relaxed design.
- **Active Session Seeker**: Refactored the notification listener to correctly pass its listener component name, enabling accurate fallback media controllers querying when token actions are restricted.

### Fixed

- **App Startup Crash**: Resolved startup crash on launch due to Compose `painterResource` trying to parse the adaptive vector launcher icon xml. Fixed by dynamically extracting the launcher icon drawable and rendering it to a Canvas-backed bitmap.
- **Overlay "Non-Touchable" Dead Zone**: Corrected height calculation of the collapsed overlay window by removing the status bar height offset and extra padding, keeping the window size tight to the visual bounds.
- **Pill Gestures and Swipe Interceptions**: Fixed direct tap gesture failures in the collapsed pill and swipe-up/down failures by capturing reactive state delegates to prevent stale state retention.
- **Controls State Synchronization**: Fixed state mismatch issues where loop state changes made inside external media apps (e.g., Spotify) did not reflect in the Smart Island overlay. Added custom extras query prioritization and strict toggling logic to avoid double-activation cycles.
- **Live Progress Catchup**: Fixed seek bar staying at its previous position on re-expand by querying live playback state offsets immediately upon window expansion.

## [1.0.0] - 2026-07-04

### Added

- Initial open-source baseline for the Smart Island Android app.
- Floating overlay service with animated collapsed and expanded island states.
- Notification listener integration for notification, incoming-call, and media modes.
- Local customization for island size, position, and corner radius.
- Demo states for notification, call, and music previews.
- App screenshots added to the README to showcase UI features.

### Changed

- Replaced default launcher icon with custom adaptive and legacy icons generated from the transparent logo (`logo.png`) on a pure black background.

### Fixed

- Fixed support/feedback links in the app to correctly load GitHub issue templates (`bug_report.md` and `feature_request.md`).
