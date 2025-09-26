package net.thunderbird.feature.mail.folder.api

import androidx.annotation.Discouraged
import kotlinx.coroutines.runBlocking
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory

/**
 * Manages outbox folders for accounts.
 *
 * An outbox folder is a special folder used to store messages that are waiting to be sent.
 * This interface provides methods for getting and creating outbox folders.
 */
interface OutboxFolderManager {
    /**
     * Gets the folder ID of the outbox folder for the given account.
     *
     * @param uuid The ID of the account.
     * @param createIfMissing If true, the outbox folder will be created if it does not exist.
     * @return The folder ID of the outbox folder.
     * @throws IllegalStateException If the outbox folder could not be found.
     */
    suspend fun getOutboxFolderId(uuid: AccountId, createIfMissing: Boolean = true): Long

    /**
     * Gets the outbox folder ID for the given account.
     *
     * This is a blocking call and should not be used on the main thread.
     *
     * @param uuid The account ID.
     * @return The outbox folder ID.
     */
    @Discouraged(message = "Avoid blocking calls from the main thread. Use getOutboxFolderId instead.")
    fun getOutboxFolderIdSync(uuid: AccountId, createIfMissing: Boolean = true): Long = runBlocking {
        getOutboxFolderId(uuid, createIfMissing)
    }

    /**
     * Creates an outbox folder for the given account.
     *
     * @param uuid The ID of the account for which to create the outbox folder.
     * @return An [Outcome] that resolves to the ID of the created outbox folder on success,
     * or an [Exception] on failure.
     */
    suspend fun createOutboxFolder(uuid: AccountId): Outcome<Long, Exception>

    /**
     * Checks if there are any pending messages in the outbox for the given account.
     *
     * @param uuid The ID of the account.
     * @return `true` if there are pending messages, `false` otherwise.
     */
    suspend fun hasPendingMessages(uuid: AccountId): Boolean
}

/**
 * Gets the folder ID of the outbox folder for the given account.
 *
 * @param uuid The ID of the account.
 * @return The folder ID of the outbox folder.
 * @throws IllegalStateException If the outbox folder could not be found.
 */
@Discouraged(
    message = "This is a wrapper for Java compatibility. " +
        "Always use getOutboxFolderIdSync(uuid: AccountId) instead on Kotlin files.",
)
@JvmOverloads
fun OutboxFolderManager.getOutboxFolderIdSync(uuid: String, createIfMissing: Boolean = true): Long {
    return getOutboxFolderIdSync(uuid = AccountIdFactory.of(uuid), createIfMissing = createIfMissing)
}

/**
 * Checks if there are pending messages in the outbox folder for the given account.
 *
 * This is a blocking call and should not be used on the main thread.
 * This is a wrapper for Java compatibility. Always use `hasPendingMessages(uuid: AccountId): Boolean`
 * instead on Kotlin files.
 *
 * @param uuid The ID of the account.
 * @return True if there are pending messages, false otherwise.
 */
@Discouraged(
    message = "This is a wrapper for Java compatibility. " +
        "Always use hasPendingMessages(uuid: AccountId): Boolean instead on Kotlin files.",
)
fun OutboxFolderManager.hasPendingMessagesSync(uuid: String): Boolean = runBlocking {
    hasPendingMessages(uuid = AccountIdFactory.of(uuid))
}
