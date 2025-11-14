package net.thunderbird.feature.mail.message.export

import com.eygraber.uri.Uri

/**
 * API for exporting messages in a format-agnostic way, decoupled from platform specifics.
 *
 * The exporter operates on URIs so the UI/platform can select a source and destination.
 * Implementations handle the I/O using platform-provided file system access.
 */
interface MessageExporter {
    /**
     * Export a message from the given source URI to the destination URI.
     *
     * @param sourceUri Uri of the source message
     * @param destinationUri Uri of the destination for the exported message
     * @return [MessageExportResult] indicating success or failure of the export operation
     */
    suspend fun export(
        sourceUri: Uri,
        destinationUri: Uri,
    ): MessageExportResult
}
