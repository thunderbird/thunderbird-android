package net.thunderbird.core.preference.display.miscSettings

import net.thunderbird.core.preference.PreferenceManager

enum class DisplayMiscSettingKey(val value: String) {

    ShowRecentChanges("showRecentChanges"),
    ShouldShowSetupArchiveFolderDialog("shouldShowSetupArchiveFolderDialog"),
    ;

    companion object {

        fun isValid(value: String): Boolean {
            return entries.any { it.value == value }
        }
    }
}

interface DisplayMiscSettingsPreferenceManager : PreferenceManager<DisplayMiscSettings>
