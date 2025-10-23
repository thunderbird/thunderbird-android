package net.thunderbird.core.file

import com.eygraber.uri.Uri
import net.thunderbird.core.file.command.CopyCommand
import net.thunderbird.core.outcome.Outcome

/**
 * Default implementation that delegates to internal commands.
 */
class DefaultFileManager(
    private val fileSystemManager: FileSystemManager,
) : FileManager {
    override suspend fun copy(sourceUri: Uri, destinationUri: Uri): Outcome<Unit, FileOperationError> =
        CopyCommand(sourceUri, destinationUri).invoke(fileSystemManager)
}
