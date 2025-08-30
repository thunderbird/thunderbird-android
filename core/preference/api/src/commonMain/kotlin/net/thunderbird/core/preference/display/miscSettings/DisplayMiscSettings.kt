package net.thunderbird.core.preference.display.miscSettings

const val DISPLAY_SETTINGS_DEFAULT_SHOW_RECENT_CHANGES = true
const val DISPLAY_SETTINGS_DEFAULT_SHOULD_SHOW_SETUP_ARCHIVE_FOLDER_DIALOG = true

data class DisplayMiscSettings(
    val showRecentChanges: Boolean = DISPLAY_SETTINGS_DEFAULT_SHOW_RECENT_CHANGES,
    val shouldShowSetupArchiveFolderDialog: Boolean = DISPLAY_SETTINGS_DEFAULT_SHOULD_SHOW_SETUP_ARCHIVE_FOLDER_DIALOG,
)
