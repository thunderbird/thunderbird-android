package net.thunderbird.core.logging.file

import androidx.core.net.toUri
import com.eygraber.uri.Uri
import com.eygraber.uri.toKmpUri
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.CoroutineContext
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.io.Buffer
import kotlinx.io.asSink
import net.thunderbird.core.file.FileManager
import net.thunderbird.core.logging.LogEvent
import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.outcome.Outcome

private const val LOG_BUFFER_COUNT = 4

open class AndroidFileLogSink(
    override val level: LogLevel,
    fileName: String,
    fileLocation: String,
    private val fileManager: FileManager,
    coroutineContext: CoroutineContext = Dispatchers.IO,
) : FileLogSink {

    private val coroutineScope = CoroutineScope(coroutineContext + SupervisorJob())
    private val logFile = File(fileLocation, "$fileName.txt")
    private val accumulatedLogs = ArrayList<String>()
    private val mutex: Mutex = Mutex()

    // Make sure the directory exists
    init {
        val directory = File(fileLocation)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        logFile.createNewFile()
    }

    override fun log(event: LogEvent) {
        coroutineScope.launch {
            mutex.withLock {
                accumulatedLogs.add(
                    "${convertLongToTime(event.timestamp)} priority = ${event.level}, ${event.message}",
                )
            }
            if (accumulatedLogs.size > LOG_BUFFER_COUNT) {
                writeToLogFile()
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun convertLongToTime(long: Long): String {
        val instant = Instant.fromEpochMilliseconds(long)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return LocalDateTime.Formats.ISO.format(dateTime)
    }

    private suspend fun writeToLogFile() {
        val outputStream = FileOutputStream(logFile, true)
        val sink = outputStream.asSink()
        var content: String
        try {
            mutex.withLock {
                content = accumulatedLogs.joinToString("\n", postfix = "\n")
                accumulatedLogs.clear()
            }
            val buffer = Buffer()
            val contentBytes = content.toByteArray(Charsets.UTF_8)
            buffer.write(contentBytes)
            sink.write(buffer, buffer.size)

            sink.flush()
        } finally {
            sink.close()
            outputStream.close()
        }
    }

    override suspend fun flushAndCloseBuffer() {
        if (accumulatedLogs.isNotEmpty()) {
            writeToLogFile()
        }
    }

    override suspend fun export(uri: Uri) {
        if (accumulatedLogs.isNotEmpty()) {
            writeToLogFile()
        }

        val sourceUri = logFile.toUri().toKmpUri()
        val result = fileManager.copy(sourceUri = sourceUri, destinationUri = uri)
        if (result is Outcome.Failure) {
            error(
                "Error copying log to destination: ${result.error}",
            )
        }

        // Clear the log file after export
        val outputStream = FileOutputStream(logFile)
        val clearSink = outputStream.asSink()

        try {
            // Write empty string to clear the file
            val buffer = Buffer()
            clearSink.write(buffer, 0)
            clearSink.flush()
        } finally {
            clearSink.close()
            outputStream.close()
        }
    }
}
