package net.thunderbird.core.file.command

import com.eygraber.uri.Uri
import net.thunderbird.core.file.FileOperationError
import net.thunderbird.core.file.FileSystemManager
import net.thunderbird.core.outcome.Outcome

/**
 * Creates the directory at [dirUri], including any missing parent directories.
 */
internal class CreateDirectoriesCommand(
    private val dirUri: Uri,
) : FileCommand<Unit> {
    override suspend fun invoke(fs: FileSystemManager): Outcome<Unit, FileOperationError> {
        return try {
            fs.createDirectories(dirUri)
            Outcome.Success(Unit)
        } catch (e: Exception) {
            Outcome.Failure(
                error = FileOperationError.Unavailable(dirUri, e.message ?: "Unable to create directory: $dirUri"),
                cause = e,
            )
        }
    }
}
