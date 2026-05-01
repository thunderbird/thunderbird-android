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
}

interface DisplayVisualSettingsPreferenceManager : PreferenceManager<DisplayVisualSettings>
