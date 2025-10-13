package net.thunderbird.core.file.command

import net.thunderbird.core.file.FileOperationError
import net.thunderbird.core.file.FileSystemManager
import net.thunderbird.core.outcome.Outcome

/**
 * A command that performs a file operation using the provided [FileSystemManager].
 *
 * @param T The type of the result produced by the command.
 */
internal fun interface FileCommand<T> {
    suspend operator fun invoke(fs: FileSystemManager): Outcome<T, FileOperationError>
}
