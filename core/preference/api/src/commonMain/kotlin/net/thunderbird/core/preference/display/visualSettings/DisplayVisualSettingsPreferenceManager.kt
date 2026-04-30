package net.thunderbird.core.preference.display.visualSettings

import net.thunderbird.core.preference.PreferenceManager

enum class DisplayVisualSettingKey(val value: String) {

    Animation("animations"),
    MessageViewFixedWidthFont("messageViewFixedWidthFont"),
    AutoFitWidth("autofitWidth"),
    MessageViewBodyContentType("messageViewBodyContentType"),
    DrawerExpandAllFolder("drawerExpandAllFolder"),
    MessageViewArchiveActionVisible("messageViewArchiveActionVisible"),
    MessageViewDeleteActionVisible("messageViewDeleteActionVisible"),
    MessageViewMoveActionVisible("messageViewMoveActionVisible"),
    MessageViewCopyActionVisible("messageViewCopyActionVisible"),
    MessageViewSpamActionVisible("messageViewSpamActionVisible"),
    ;

    companion object {

        fun isValid(value: String): Boolean {
            return entries.any { it.value == value }
        }
    }
}

interface DisplayVisualSettingsPreferenceManager : PreferenceManager<DisplayVisualSettings>
