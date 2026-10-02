package net.thunderbird.core.logging.internal

import com.eygraber.uri.Uri
import net.thunderbird.components.core.logging.file.FileLogSink
import net.thunderbird.core.logging.SyncDebugLogExporter

internal class DefaultSyncDebugLogExporter(
    private val fileLogSink: FileLogSink,
    private val destinationFile: (Uri) -> io.github.vinceglb.filekit.PlatformFile,
) : SyncDebugLogExporter {
    override suspend fun export(destination: Uri) {
        fileLogSink.export(destinationFile(destination))
    }
}
