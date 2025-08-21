package net.thunderbird.core.preference

import net.thunderbird.core.preference.debugging.DebuggingSettings
import net.thunderbird.core.preference.display.DisplaySettings
import net.thunderbird.core.preference.network.NetworkSettings
import net.thunderbird.core.preference.notification.NotificationPreference
import net.thunderbird.core.preference.privacy.PrivacySettings

/**
 * Stores a snapshot of the app's general settings.
 *
 * When adding a setting here, make sure to also add it in these places:
 * - [GeneralSettingsManager] (write function)
 * - [GeneralSettingsDescriptions]
 */
// TODO: Move over settings from K9
data class GeneralSettings(
    val network: NetworkSettings = NetworkSettings(),
    val notification: NotificationPreference = NotificationPreference(),
    val display: DisplaySettings = DisplaySettings(),
    val privacy: PrivacySettings = PrivacySettings(),
    val debugging: DebuggingSettings = DebuggingSettings(),
)

enum class BackgroundSync {
    ALWAYS,
    NEVER,
    FOLLOW_SYSTEM_AUTO_SYNC,
}

enum class AppTheme {
    LIGHT,
    DARK,
    FOLLOW_SYSTEM,
}

enum class SubTheme {
    LIGHT,
    DARK,
    USE_GLOBAL,
}

enum class BackgroundOps {
    ALWAYS,
    NEVER,
    WHEN_CHECKED_AUTO_SYNC,
}

/**
 * Controls when to use the message list split view.
 */
enum class SplitViewMode {
    ALWAYS,
    NEVER,
    WHEN_IN_LANDSCAPE,
}
