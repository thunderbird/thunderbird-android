package net.thunderbird.feature.mail.message.list.internal.ui.preview

import net.thunderbird.core.preference.display.visualSettings.message.list.MessageListDateTimeFormat
import net.thunderbird.core.preference.display.visualSettings.message.list.UiDensity
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences

internal object MessagePreferencesPreviewHelper {
    val defaultPreferences = MessageListPreferences(
        density = UiDensity.Default,
        groupConversations = true,
        showCorrespondentNames = true,
        showMessageAvatar = true,
        showFavouriteButton = true,
        senderAboveSubject = false,
        excerptLines = 2,
        dateTimeFormat = MessageListDateTimeFormat.Contextual,
    )

    val compactPreferences = defaultPreferences.copy(
        density = UiDensity.Compact,
        excerptLines = 1,
    )

    val relaxedPreferences = defaultPreferences.copy(
        density = UiDensity.Relaxed,
        excerptLines = 3,
    )

    val senderAboveSubjectPreferences = defaultPreferences.copy(
        senderAboveSubject = true,
    )

    val noAvatarNoFavouritePreferences = defaultPreferences.copy(
        showMessageAvatar = false,
        showFavouriteButton = false,
    )

    val fullDatePreferences = defaultPreferences.copy(
        dateTimeFormat = MessageListDateTimeFormat.Full,
    )
}
