package net.thunderbird.core.preference.display

import net.thunderbird.core.preference.display.coreSettings.DisplayCoreSettings
import net.thunderbird.core.preference.display.inboxSettings.DisplayInboxSettings
import net.thunderbird.core.preference.display.visualSettings.DisplayVisualSettings

const val DISPLAY_SETTINGS_DEFAULT_SHOW_RECENT_CHANGES = true
const val DISPLAY_SETTINGS_DEFAULT_SHOULD_SHOW_SETUP_ARCHIVE_FOLDER_DIALOG = true

data class DisplaySettings(
    val coreSettings: DisplayCoreSettings = DisplayCoreSettings(),
    val inboxSettings: DisplayInboxSettings = DisplayInboxSettings(),
    val visualSettings: DisplayVisualSettings = DisplayVisualSettings(),
    val showRecentChanges: Boolean = DISPLAY_SETTINGS_DEFAULT_SHOW_RECENT_CHANGES,
    val shouldShowSetupArchiveFolderDialog: Boolean = DISPLAY_SETTINGS_DEFAULT_SHOULD_SHOW_SETUP_ARCHIVE_FOLDER_DIALOG,
)
