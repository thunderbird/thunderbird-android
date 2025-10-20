package net.thunderbird.core.file.command

import com.eygraber.uri.Uri
import kotlinx.io.Buffer
import net.thunderbird.core.file.FileOperationError
import net.thunderbird.core.file.FileSystemManager
import net.thunderbird.core.outcome.Outcome

/**
 * Copies data from [sourceUri] to [destinationUri] using buffered I/O.
 */
internal class CopyCommand(
    private val sourceUri: Uri,
    private val destinationUri: Uri,
) : FileCommand<Unit> {
    override suspend fun invoke(fs: FileSystemManager): Outcome<Unit, FileOperationError> {
        // Open endpoints
        val source = fs.openSource(sourceUri)
            ?: return Outcome.Failure(
                FileOperationError.Unavailable(sourceUri, "Unable to open source: $sourceUri"),
            )
        val sink = fs.openSink(destinationUri)
            ?: return Outcome.Failure(
                FileOperationError.Unavailable(destinationUri, "Unable to open destination: $destinationUri"),
            )

        return try {
            val buffer = Buffer()
            while (true) {
                val read = try {
                    source.readAtMostTo(buffer, BUFFER_SIZE)
                } catch (e: Exception) {
                    return Outcome.Failure(FileOperationError.ReadFailed(sourceUri, e.message), cause = e)
                }
                if (read <= 0L) break
                try {
                    sink.write(buffer, read)
                } catch (e: Exception) {
                    return Outcome.Failure(FileOperationError.WriteFailed(destinationUri, e.message), cause = e)
                }
            }
            try {
                sink.flush()
            } catch (e: Exception) {
                return Outcome.Failure(FileOperationError.WriteFailed(destinationUri, e.message), cause = e)
            }
            Outcome.Success(Unit)
        } catch (e: Exception) {
            Outcome.Failure(FileOperationError.Unknown(e.message), cause = e)
        } finally {
            try {
                source.close()
            } catch (_: Exception) {}
            try {
                sink.close()
            } catch (_: Exception) {}
        }
    }

    private companion object {
        const val BUFFER_SIZE = 8_192L
    }
}
