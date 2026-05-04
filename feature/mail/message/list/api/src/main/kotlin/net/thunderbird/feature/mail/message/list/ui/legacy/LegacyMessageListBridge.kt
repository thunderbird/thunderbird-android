package net.thunderbird.feature.mail.message.list.ui.legacy

import kotlinx.coroutines.flow.Flow
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences
import net.thunderbird.feature.mail.message.list.ui.state.MessageItemUi
import net.thunderbird.feature.mail.message.list.ui.state.MessageListMetadata

/**
 * Bridge interface for accessing functionalities implemented in the legacy message list system.
 *
 * @see MessageListPreferences for available display and behavior customization options
 * @see MessageListMetadata for contextual information about the message list state
 * @see MessageItemUi for the structure of individual message items in the returned list
 */
interface LegacyMessageListBridge {
    /**
     * Loads messages for the current mailbox stored in the database.
     *
     * @param preferences Display and behaviour settings that influence how messages are loaded.
     * @param metadata Contextual information about the message list (e.g. folder, account).
     * @returns a [Flow] that emits updated [MessageItemUi] lists as the underlying data changes.
     */
    fun loadMessages(
        preferences: MessageListPreferences,
        metadata: MessageListMetadata,
    ): Flow<List<MessageItemUi>>
}
