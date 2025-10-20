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
        val source = fs.openSource(sourceUri.toString())
            ?: return Outcome.Failure(
                FileOperationError.Unavailable(sourceUri, "Unable to open source: $sourceUri"),
            )
        val sink = fs.openSink(destinationUri.toString())
            ?: return Outcome.Failure(
                FileOperationError.Unavailable(destinationUri, "Unable to open destination: $destinationUri"),
            )

        return try {
            val buffer = Buffer()
            while (true) {
                val read = try {
                    source.readAtMostTo(buffer, BUFFER_SIZE)
                } catch (t: Throwable) {
                    return Outcome.Failure(FileOperationError.ReadFailed(sourceUri, t.message), cause = t)
                }
                if (read <= 0L) break
                try {
                    sink.write(buffer, read)
                } catch (t: Throwable) {
                    return Outcome.Failure(FileOperationError.WriteFailed(destinationUri, t.message), cause = t)
                }
            }
            try {
                sink.flush()
            } catch (t: Throwable) {
                return Outcome.Failure(FileOperationError.WriteFailed(destinationUri, t.message), cause = t)
            }
            Outcome.Success(Unit)
        } catch (t: Throwable) {
            Outcome.Failure(FileOperationError.Unknown(t.message), cause = t)
        } finally {
            try {
                source.close()
            } catch (_: Throwable) {}
            try {
                sink.close()
            } catch (_: Throwable) {}
        }
    }

    private companion object {
        const val BUFFER_SIZE = 8_192L
    }
}
