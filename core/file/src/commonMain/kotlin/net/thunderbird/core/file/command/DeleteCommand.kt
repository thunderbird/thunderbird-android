package net.thunderbird.core.file.command

import com.eygraber.uri.Uri
import kotlinx.io.IOException
import kotlinx.io.files.FileNotFoundException
import net.thunderbird.core.file.FileOperationError
import net.thunderbird.core.file.FileSystemManager
import net.thunderbird.core.outcome.Outcome

/**
 * Deletes the file at the given [uri].
 */
class DeleteCommand(
    private val uri: Uri,
) : FileCommand<Unit> {
    override suspend fun invoke(fs: FileSystemManager): Outcome<Unit, FileOperationError> {
        return try {
            fs.delete(uri)
            Outcome.Success(Unit)
        } catch (_: FileNotFoundException) {
            Outcome.Success(Unit)
        } catch (e: IOException) {
            Outcome.Failure(
                error = FileOperationError.Unavailable(uri, e.message ?: "Unable to delete file at: $uri"),
                cause = e,
            )
        }
    }
}
