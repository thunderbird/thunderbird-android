package net.thunderbird.core.preference.display.miscSettings

import net.thunderbird.core.preference.PreferenceManager

enum class DisplayMiscSettingKey(val value: String) {

    ShowRecentChanges("showRecentChanges"),
    ShouldShowSetupArchiveFolderDialog("shouldShowSetupArchiveFolderDialog"),
}

interface DisplayMiscSettingsPreferenceManager : PreferenceManager<DisplayMiscSettings>
