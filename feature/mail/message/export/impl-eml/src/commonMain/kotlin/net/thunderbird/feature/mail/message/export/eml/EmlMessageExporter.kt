package net.thunderbird.feature.mail.message.export.eml

import com.eygraber.uri.Uri
import net.thunderbird.core.file.FileManager
import net.thunderbird.core.file.FileOperationError
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.mail.message.export.MessageExportError
import net.thunderbird.feature.mail.message.export.MessageExportResult
import net.thunderbird.feature.mail.message.export.MessageExporter

class EmlMessageExporter(
    private val fileManager: FileManager,
) : MessageExporter {
    override suspend fun export(
        sourceUri: Uri,
        destinationUri: Uri,
    ): MessageExportResult {
        val outcome = fileManager.copy(sourceUri = sourceUri, destinationUri = destinationUri)
        return when (outcome) {
            is Outcome.Success -> Outcome.Success(Unit)
            is Outcome.Failure -> Outcome.Failure(
                error = mapError(outcome.error),
                cause = outcome.cause,
            )
        }
    }

    private fun mapError(
        error: FileOperationError,
    ): MessageExportError {
        return when (error) {
            is FileOperationError.Unavailable -> MessageExportError.Unavailable(
                error.uri,
                error.message,
            )
            is FileOperationError.ReadFailed -> MessageExportError.Io(error.message)
            is FileOperationError.WriteFailed -> MessageExportError.Io(error.message)
            is FileOperationError.Unknown -> MessageExportError.Unknown(error.message)
        }
    }
}
