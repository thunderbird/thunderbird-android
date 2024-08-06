package app.k9mail.legacy.mailstore

interface MessageListRepository {
    fun addListener(listener: MessageListChangedListener)
    fun addListener(accountUuid: String, listener: MessageListChangedListener)
    fun removeListener(listener: MessageListChangedListener)
    fun notifyMessageListChanged(accountUuid: String)

    fun <T> getMessages(
        accountUuid: String,
        selection: String,
        selectionArgs: Array<String>,
        sortOrder: String,
        messageMapper: MessageMapper<T>,
    ): List<T>

    fun <T> getThreadedMessages(
        accountUuid: String,
        selection: String,
        selectionArgs: Array<String>,
        sortOrder: String,
        messageMapper: MessageMapper<T>,
    ): List<T>

    fun <T> getThread(
        accountUuid: String,
        threadId: Long,
        sortOrder: String,
        messageMapper: MessageMapper<T>,
    ): List<T>
}
