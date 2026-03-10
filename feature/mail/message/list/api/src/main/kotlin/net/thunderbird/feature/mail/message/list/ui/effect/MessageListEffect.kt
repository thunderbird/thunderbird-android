package net.thunderbird.feature.mail.message.list.ui.effect

import net.thunderbird.feature.account.AccountId

/**
 * Represents one-time side effects that can be triggered from the message list screen.
 * These effects are intended to be consumed by the UI to perform actions like navigation
 * or showing transient messages.
 */
sealed interface MessageListEffect {
    /**
     * Effect to navigate back from the current screen.
     */
    data object NavigateBack : MessageListEffect

    /**
     * Effect to navigate to the "Move To" screen, allowing the user to select a destination
     * folder for the given messages.
     *
     * @param messagesIds The unique identifiers of the messages to be moved.
     * @param accountId The identifier of the account to which the messages belong.
     */
    data class NavigateToMoveToScreen(val messagesIds: List<Long>, val accountId: AccountId) : MessageListEffect

    /**
     * Represents the effect of messages being successfully archived.
     * This is typically used to show a notification or a snackbar to the user.
     *
     * @param messagesIdByAccountId A map where keys are account IDs and values are lists of message IDs
     * that have been archived for that account.
     */
    data class ArchivedMessages(val messagesIdByAccountId: Map<AccountId, List<Long>>) : MessageListEffect

    /**
     * Represents an effect for when pending (outbox) messages have been successfully sent.
     *
     * This effect is triggered after messages residing in the outbox are processed and sent,
     * typically resulting in them being moved to the Sent folder, if any configured.
     *
     * @param messagesIdByAccountId A map where each key is an [AccountId] and the value is a list of
     * message IDs that have been sent for that account.
     */
    data class PendingMessagesSent(val messagesIdByAccountId: Map<AccountId, List<Long>>) : MessageListEffect

    /**
     * Represents an effect for when messages have been permanently removed (expunged) from a folder.
     *
     * This typically happens after messages marked for deletion are purged from the server,
     * particularly with IMAP accounts. The UI should reflect this by removing these messages
     * or updating its state accordingly.
     *
     * @param messagesIdByAccountId A map where keys are account IDs and values are lists of message IDs
     * that have been expunged. This structure supports handling expunged messages from multiple accounts
     * in a single operation.
     */
    data class ExpungedMessages(val messagesIdByAccountId: Map<AccountId, List<Long>>) : MessageListEffect

    /**
     * Represents the side effect that messages have been marked for deletion.
     *
     * This effect is typically triggered after a user performs a delete action. The UI can use this
     * to show a confirmation, such as a snackbar, indicating the successful deletion.
     *
     * @param messagesIdByAccountId A map where each key is an [AccountId] and the corresponding value
     * is a list of message IDs that have been deleted for that account.
     */
    data class DeletedMessages(val messagesIdByAccountId: Map<AccountId, List<Long>>) : MessageListEffect

    /**
     * Represents the effect of one or more draft messages being discarded (deleted).
     *
     * @param messagesIdByAccountId A map where each key is an [AccountId] and the value is a list of message IDs
     * that have been discarded for that account.
     */
    data class DraftsDiscarded(val messagesIdByAccountId: Map<AccountId, List<Long>>) : MessageListEffect
}
