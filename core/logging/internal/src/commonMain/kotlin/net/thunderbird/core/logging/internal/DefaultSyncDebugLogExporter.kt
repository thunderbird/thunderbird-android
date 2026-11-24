package net.thunderbird.core.logging.internal

import com.eygraber.uri.Uri
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.writeString
import net.thunderbird.components.core.logging.file.FileLogSink
import net.thunderbird.core.logging.SyncDebugLogExporter

internal class DefaultSyncDebugLogExporter(
    private val fileLogSink: FileLogSink,
    private val logFile: PlatformFile,
    private val destinationFile: (Uri) -> PlatformFile,
) : SyncDebugLogExporter {
    override suspend fun export(destination: Uri) {
        // FileLogSink only creates its file when flushing events, but an empty log can still be exported.
        if (!logFile.exists()) {
            logFile.writeString("")
        }

        fileLogSink.exportAndClear(destinationFile(destination))
    }
}
