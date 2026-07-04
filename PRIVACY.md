# Privacy Policy

Last updated: July 4, 2026

Smart Island is designed to run locally on your Android device. This document explains what the app can access and how that information is used.

## Information The App Processes

When you enable notification listener access, Smart Island can process notification metadata from other apps so it can display the floating island experience. This may include:

- App name and package name
- Notification title and text
- Notification icons or large images
- Notification action labels and action intents
- Call and media notification categories
- Media metadata such as artwork, playback state, duration, and position when available

The app also stores local island settings, such as enabled state, size, position, and corner radius.

## How Information Is Used

Notification data is used to render the island UI, show demo or real notification states, open notification content, dismiss notifications, and update media/call presentation.

Settings are used to remember your preferred island appearance and position.

## Data Sharing

At the time of this policy, Smart Island:

- Does not request the Android `INTERNET` permission.
- Does not include analytics or advertising SDKs.
- Does not send notification content to a remote server.
- Does not sell or share user data.

External links in the app may open GitHub, Google Play, email, or social/profile pages in another app or browser. Those external apps and services have their own privacy practices.

## Data Retention

Notification details are kept in app memory while Smart Island is running and while the notification is active in the island. Local settings are stored on your device using AndroidX DataStore Preferences until you clear app data or uninstall the app.

## Your Controls

You can:

- Turn off Smart Island in the app.
- Revoke overlay permission in Android settings.
- Revoke notification listener access in Android settings.
- Clear app storage or uninstall the app to remove local settings.

## Changes

If the app adds network features, analytics, crash reporting, accounts, cloud sync, or any other data sharing behavior, this policy should be updated before release.

## Contact

For privacy questions, open an issue in this repository without including private notification content.
