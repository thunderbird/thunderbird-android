package net.thunderbird.feature.account.avatar.data.datasource

import com.eygraber.uri.Uri
import net.thunderbird.core.file.DirectoryProvider
import net.thunderbird.core.file.FileManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.avatar.data.AvatarDataContract.DataSource.LocalAvatarImage

/**
 * Local data source implementation for managing avatar images.
 *
 * Uses [DirectoryProvider] to resolve the app's files directory and stores
 * avatar images under `${LocalAvatarImage.DIRECTORY_NAME}`.
 *
 * @param fileManager The [FileManager] for file operations.
 * @param directoryProvider The [DirectoryProvider] to get app directories.
 */
internal class LocalAvatarImageDataSource(
    private val fileManager: FileManager,
    private val directoryProvider: DirectoryProvider,
) : LocalAvatarImage {

    override suspend fun update(id: AccountId, imageUri: Uri): Uri {
        val avatarImageUri = getAvatarImageUri(id)

        fileManager.copy(imageUri, avatarImageUri)

        return avatarImageUri
    }

    override suspend fun delete(id: AccountId) {
        val avatarImageUri = getAvatarImageUri(id)
        fileManager.delete(avatarImageUri)
    }

    private suspend fun getAvatarImageUri(id: AccountId): Uri = getAvatarDirUri().buildUpon()
        .appendPath("${id.asRaw()}.$AVATAR_IMAGE_FILE_EXTENSION")
        .build()

    private suspend fun getAvatarDirUri(): Uri {
        return directoryProvider.getFilesDir().buildUpon()
            .appendPath(LocalAvatarImage.DIRECTORY_NAME)
            .build()
            .also { fileManager.createDirectories(it) }
    }

    private companion object {
        const val AVATAR_IMAGE_FILE_EXTENSION = "jpg"
    }
}
