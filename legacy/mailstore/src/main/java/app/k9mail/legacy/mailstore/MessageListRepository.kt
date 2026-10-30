package app.k9mail.legacy.mailstore

import net.thunderbird.feature.account.AccountId

interface MessageListRepository {
    fun addListener(listener: MessageListChangedListener)
    fun addListener(accountId: AccountId, listener: MessageListChangedListener)
    fun removeListener(listener: MessageListChangedListener)
    fun notifyMessageListChanged(accountId: AccountId)

    fun <T> getMessages(
        accountId: AccountId,
        selection: String,
        selectionArgs: Array<String>,
        sortOrder: String,
        messageMapper: MessageMapper<T>,
    ): List<T>

    fun <T> getThreadedMessages(
        accountId: AccountId,
        selection: String,
        selectionArgs: Array<String>,
        sortOrder: String,
        messageMapper: MessageMapper<T>,
    ): List<T>

    fun <T> getThread(
        accountId: AccountId,
        threadId: Long,
        sortOrder: String,
        messageMapper: MessageMapper<T>,
    ): List<T>
}
