package net.thunderbird.core.file.command

import com.eygraber.uri.Uri
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import net.thunderbird.core.file.FileOperationError
import net.thunderbird.core.file.FileSystemManager
import net.thunderbird.core.file.WriteMode
import net.thunderbird.core.outcome.Outcome

/**
 * Copies data from [sourceUri] to [destinationUri] using buffered I/O.
 */
internal class CopyCommand(
    private val sourceUri: Uri,
    private val destinationUri: Uri,
) : FileCommand<Unit> {
    override suspend fun invoke(fs: FileSystemManager): Outcome<Unit, FileOperationError> {
        val source = fs.openSource(sourceUri)
        val sink = fs.openSink(destinationUri, WriteMode.Truncate)

        return if (source == null) {
            Outcome.Failure(
                FileOperationError.Unavailable(sourceUri, "Unable to open source: $sourceUri"),
            )
        } else if (sink == null) {
            Outcome.Failure(
                FileOperationError.Unavailable(destinationUri, "Unable to open destination: $destinationUri"),
            )
        } else {
            copy(source, sink)
        }
    }

    private fun copy(source: RawSource, sink: RawSink): Outcome<Unit, FileOperationError> {
        val buffer = Buffer()

        return try {
            copyToSink(source, sink, buffer)
            flushSink(sink)
            Outcome.Success(Unit)
        } catch (e: FileOperationException) {
            Outcome.Failure(e.error, cause = e.cause)
        } catch (e: IOException) {
            Outcome.Failure(FileOperationError.Unknown(e.message), cause = e)
        } finally {
            closeQuietly(source, sink)
        }
    }

    private fun copyToSink(source: RawSource, sink: RawSink, buffer: Buffer) {
        while (true) {
            val read = try {
                source.readAtMostTo(buffer, BUFFER_SIZE)
            } catch (e: IOException) {
                throw FileOperationException(FileOperationError.ReadFailed(sourceUri, e.message), e)
            }

            if (read <= 0L) break

            try {
                sink.write(buffer, read)
            } catch (e: IOException) {
                throw FileOperationException(FileOperationError.WriteFailed(destinationUri, e.message), e)
            }
        }
    }

    private fun flushSink(sink: RawSink) {
        try {
            sink.flush()
        } catch (e: IOException) {
            throw FileOperationException(FileOperationError.WriteFailed(destinationUri, e.message), e)
        }
    }

    private class FileOperationException(
        val error: FileOperationError,
        override val cause: Throwable?,
    ) : Exception(error.toString(), cause)

    private fun closeQuietly(source: AutoCloseable?, sink: AutoCloseable?) {
        try {
            source?.close()
        } catch (_: Exception) {
        }
        try {
            sink?.close()
        } catch (_: Exception) {
        }
    }

    private companion object {
        const val BUFFER_SIZE = 8_192L
    }
}
