package net.thunderbird.feature.account.avatar.data.datasource

import com.eygraber.uri.Uri
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import net.thunderbird.core.file.DirectoryProvider
import net.thunderbird.core.file.FileManager
import net.thunderbird.core.file.MimeType
import net.thunderbird.core.file.MimeTypeResolver
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
 * @param mimeTypeResolver The [MimeTypeResolver] to determine image formats.
 * @param clock The [Clock] used for cache invalidation.
 */

@OptIn(ExperimentalTime::class)
internal class LocalAvatarImageDataSource(
    private val fileManager: FileManager,
    private val directoryProvider: DirectoryProvider,
    private val mimeTypeResolver: MimeTypeResolver,
    private val clock: Clock,
) : LocalAvatarImage {

    override suspend fun update(id: AccountId, imageUri: Uri): Uri {
        val mimeType = mimeTypeResolver.getMimeType(imageUri)
        val targetExtension = if (mimeType == MimeType.PNG) EXTENSION_PNG else EXTENSION_JPG

        delete(id)

        val avatarImageUri = getAvatarImageUri(id, targetExtension)

        fileManager.copy(imageUri, avatarImageUri)

        return avatarImageUri.buildUpon()
            .appendQueryParameter(PARAMETER_VERSION, clock.now().toEpochMilliseconds().toString())
            .build()
    }

    override suspend fun delete(id: AccountId) {
        val now = clock.now().toEpochMilliseconds().toString()

        fileManager.delete(
            getAvatarImageUri(id, EXTENSION_JPG).buildUpon()
                .appendQueryParameter(PARAMETER_VERSION, now)
                .build(),
        )
        fileManager.delete(
            getAvatarImageUri(id, EXTENSION_PNG).buildUpon()
                .appendQueryParameter(PARAMETER_VERSION, now)
                .build(),
        )
    }

    private suspend fun getAvatarImageUri(id: AccountId, extension: String): Uri {
        val directory = directoryProvider.getFilesDir().buildUpon()
            .appendPath(LocalAvatarImage.DIRECTORY_NAME)
            .build()
            .also { fileManager.createDirectories(it) }

        return directory.buildUpon()
            .appendPath("$id.$extension")
            .build()
    }

    companion object {
        private const val EXTENSION_JPG = "jpg"
        private const val EXTENSION_PNG = "png"
        private const val PARAMETER_VERSION = "v"
    }
}
