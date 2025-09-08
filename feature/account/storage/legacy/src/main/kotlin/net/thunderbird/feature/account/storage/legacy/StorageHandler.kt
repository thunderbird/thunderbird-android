package net.thunderbird.feature.account.storage.legacy

import androidx.annotation.Discouraged
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor

/**
 * Represents a storage handler for a specific data type.
 *
 * @param T The type of data that this handler can handle.
 */
@Discouraged(
    message = "This interface is only used to encapsulate the [LegacyAccount] storage handling.",
)
interface StorageHandler<T> {

    /**
     * Loads the data from the storage into the provided object.
     *
     * @param data The object to load the data into.
     * @param storage The storage to load the data from.
     */
    fun load(data: T, storage: Storage)

    /**
     * Saves the data from the provided object to the storage.
     *
     * @param data The object to save the data from.
     * @param storage The storage to save the data to.
     * @param editor The storage editor to use for saving the data.
     */
    fun save(data: T, storage: Storage, editor: StorageEditor)

    /**
     * Deletes the data from the storage.
     *
     * @param data The data to delete.
     * @param storage The storage to delete the data from.
     * @param editor The storage editor to use for deleting the data.
     */
    fun delete(data: T, storage: Storage, editor: StorageEditor)
}

interface AccountDtoStorageHandler : StorageHandler<LegacyAccountDto>

interface ProfileDtoStorageHandler : StorageHandler<LegacyAccountDto>

interface AvatarDtoStorageHandler : StorageHandler<LegacyAccountDto>
