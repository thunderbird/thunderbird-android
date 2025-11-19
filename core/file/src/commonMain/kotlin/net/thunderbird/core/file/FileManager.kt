package net.thunderbird.core.file

import com.eygraber.uri.Uri
import net.thunderbird.core.outcome.Outcome

/**
 * File manager for common file operations.
 */
interface FileManager {
    /**
     * Copy data from [sourceUri] to [destinationUri].
     *
     * @param sourceUri The [Uri] of the source file.
     * @param destinationUri The [Uri] of the destination file.
     * @return [Outcome] with [Unit] on success or [FileOperationError] on failure.
     */
    suspend fun copy(sourceUri: Uri, destinationUri: Uri): Outcome<Unit, FileOperationError>

    /**
     * Delete the file at the given [uri].
     *
     * @param uri The [Uri] of the file to delete.
     * @return [Outcome] with [Unit] on success or [FileOperationError] on failure.
     */
    suspend fun delete(uri: Uri): Outcome<Unit, FileOperationError>
}
