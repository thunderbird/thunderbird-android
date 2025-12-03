package net.thunderbird.feature.mail.message.list.internal.domain.usecase

import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import net.thunderbird.core.preference.display.DisplaySettingsPreferenceManager
import net.thunderbird.core.preference.interaction.InteractionSettings
import net.thunderbird.core.preference.interaction.InteractionSettingsPreferenceManager
import net.thunderbird.feature.mail.message.list.domain.DomainContract
import net.thunderbird.feature.mail.message.list.preferences.ActionRequiringUserConfirmation
import net.thunderbird.feature.mail.message.list.preferences.MessageListDateTimeFormat
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences

class GetMessageListPreferences(
    private val displayPreferenceManager: DisplaySettingsPreferenceManager,
    private val interactionPreferenceManager: InteractionSettingsPreferenceManager,
) : DomainContract.UseCase.GetMessageListPreferences {
    override fun invoke(): Flow<MessageListPreferences> = displayPreferenceManager
        .getConfigFlow()
        .combine(interactionPreferenceManager.getConfigFlow()) { displaySettings, interactionSettings ->
            val inboxSettings = displaySettings.inboxSettings
            val messageListSettings = displaySettings.visualSettings.messageListSettings

            MessageListPreferences(
                density = messageListSettings.uiDensity,
                groupConversations = inboxSettings.isThreadedViewEnabled,
                showCorrespondentNames = messageListSettings.isShowCorrespondentNames,
                showMessageAvatar = messageListSettings.isShowContactPicture,
                showFavouriteButton = inboxSettings.isShowMessageListStars,
                excerptLines = messageListSettings.previewLines,
                // TODO(#10202): update to fetch dateTimeFormat from preferences
                dateTimeFormat = MessageListDateTimeFormat.Auto,
                useVolumeKeyNavigation = interactionSettings.useVolumeKeysForNavigation,
                serverSearchLimit = -1,
                actionRequiringUserConfirmation = interactionSettings.actionRequiringUserConfirmation.toImmutableSet(),
            )
        }

    private val InteractionSettings.actionRequiringUserConfirmation: Set<ActionRequiringUserConfirmation>
        get() = buildSet {
            if (isConfirmDelete) {
                add(ActionRequiringUserConfirmation.Delete)
            }
            if (isConfirmDeleteStarred) {
                add(ActionRequiringUserConfirmation.DeleteStarred)
            }
            if (isConfirmDeleteFromNotification) {
                add(ActionRequiringUserConfirmation.DeleteFromNotification)
            }
            if (isConfirmSpam) {
                add(ActionRequiringUserConfirmation.Spam)
            }
            if (isConfirmDiscardMessage) {
                add(ActionRequiringUserConfirmation.DiscardMessage)
            }
            if (isConfirmMarkAllRead) {
                add(ActionRequiringUserConfirmation.MarkAllRead)
            }
        }
}
