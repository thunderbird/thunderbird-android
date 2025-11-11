package net.thunderbird.core.preference.display

import net.thunderbird.core.preference.display.coreSettings.DisplayCoreSettings
import net.thunderbird.core.preference.display.inboxSettings.DisplayInboxSettings
import net.thunderbird.core.preference.display.miscSettings.DisplayMiscSettings
import net.thunderbird.core.preference.display.visualSettings.DisplayVisualSettings

data class DisplaySettings(
    val coreSettings: DisplayCoreSettings = DisplayCoreSettings(),
    val inboxSettings: DisplayInboxSettings = DisplayInboxSettings(),
    val visualSettings: DisplayVisualSettings = DisplayVisualSettings(),
    val miscSettings: DisplayMiscSettings = DisplayMiscSettings(),
)
