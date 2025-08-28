package net.thunderbird.core.preference.display

import net.thunderbird.core.preference.PreferenceManager

const val KEY_SHOW_RECENT_CHANGES = "showRecentChanges"
const val KEY_SHOULD_SHOW_SETUP_ARCHIVE_FOLDER_DIALOG = "shouldShowSetupArchiveFolderDialog"

interface DisplaySettingsPreferenceManager : PreferenceManager<DisplaySettings>
