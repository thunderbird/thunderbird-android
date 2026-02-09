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
        // Detect desired extension from input (simple heuristic)
        val isPng = imageUri.toString().endsWith(".png", ignoreCase = true)
        val targetExtension = if (isPng) EXTENSION_PNG else EXTENSION_JPG

        // 1. Clean up any existing avatars (jpg or png) for this account
        //    to avoid having "123.jpg" and "123.png" existing simultaneously.
        delete(id)

        // 2. Generate the new target URI
        val avatarImageUri = getAvatarImageUri(id, targetExtension)

        // 3. Copy the file
        fileManager.copy(imageUri, avatarImageUri)

        return avatarImageUri
    }

    override suspend fun delete(id: AccountId) {
        // Try to delete both extensions to ensure clean up completely
        SUPPORTED_EXTENSIONS.forEach { extension ->
            val avatarImageUri = getAvatarImageUri(id, extension)
            fileManager.delete(avatarImageUri)
        }
    }

    private suspend fun getAvatarImageUri(id: AccountId, extension: String): Uri = getAvatarDirUri().buildUpon()
        .appendPath("$id.$extension")
        .build()

    private suspend fun getAvatarDirUri(): Uri {
        return directoryProvider.getFilesDir().buildUpon()
            .appendPath(LocalAvatarImage.DIRECTORY_NAME)
            .build()
            .also { fileManager.createDirectories(it) }
    }

    private companion object {
        const val EXTENSION_JPG = "jpg"
        const val EXTENSION_PNG = "png"
        val SUPPORTED_EXTENSIONS = setOf(EXTENSION_JPG, EXTENSION_PNG)
    }
}
