package net.thunderbird.core.file

import com.eygraber.uri.Uri
import net.thunderbird.core.file.command.CopyCommand
import net.thunderbird.core.file.command.CreateDirectoriesCommand
import net.thunderbird.core.file.command.DeleteCommand
import net.thunderbird.core.outcome.Outcome

/**
 * Default implementation that delegates to internal commands.
 */
class DefaultFileManager(
    private val fileSystem: FileSystemManager,
) : FileManager {
    override suspend fun copy(sourceUri: Uri, destinationUri: Uri): Outcome<Unit, FileOperationError> =
        CopyCommand(sourceUri, destinationUri).invoke(fileSystem)

    override suspend fun delete(uri: Uri): Outcome<Unit, FileOperationError> =
        DeleteCommand(uri).invoke(fileSystem)

    override suspend fun createDirectories(uri: Uri): Outcome<Unit, FileOperationError> =
        CreateDirectoriesCommand(uri).invoke(fileSystem)
}
