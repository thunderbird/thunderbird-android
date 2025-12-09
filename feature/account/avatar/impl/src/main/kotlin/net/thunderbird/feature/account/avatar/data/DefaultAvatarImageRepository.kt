package net.thunderbird.feature.account.avatar.data

import com.eygraber.uri.Uri
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.avatar.AvatarImageRepository
import net.thunderbird.feature.account.avatar.data.AvatarDataContract.DataSource

/**
 * Default implementation of [AvatarImageRepository].
 *
 * Uses a local data source to manage avatar images.
 *
 * @param localDataSource The local data source for avatar images.
 */
internal class DefaultAvatarImageRepository(
    private val localDataSource: DataSource.LocalAvatarImage,
) : AvatarImageRepository {

    override suspend fun update(id: AccountId, imageUri: Uri): Uri = localDataSource.update(id, imageUri)

    override suspend fun delete(id: AccountId) = localDataSource.delete(id)
}
