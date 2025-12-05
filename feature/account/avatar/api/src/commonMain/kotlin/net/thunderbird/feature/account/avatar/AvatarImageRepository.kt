package net.thunderbird.feature.account.avatar

import com.eygraber.uri.Uri
import net.thunderbird.feature.account.AccountId

/**
 * Repository for managing avatar images.
 */
interface AvatarImageRepository {

    /**
     * Updates the avatar image for the specified account.
     *
     * @param id The ID of the account.
     * @param imageUri The new avatar image uri.
     * @return The URI of the updated avatar image as a string.
     */
    suspend fun update(id: AccountId, imageUri: Uri): Uri

    /**
     * Deletes the avatar image for the specified account.
     */
    suspend fun delete(id: AccountId)
}
