package net.thunderbird.core.logging.file

import java.io.File
import java.io.FileInputStream
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
import kotlinx.io.RawSink
import kotlinx.io.asSink
import net.thunderbird.core.file.FileSystemManager
import net.thunderbird.core.logging.LogEvent
import net.thunderbird.core.logging.LogLevel

private const val BUFFER_SIZE = 8192 // 8KB buffer size
private const val LOG_BUFFER_COUNT = 4

open class AndroidFileLogSink(
    override val level: LogLevel,
    fileName: String,
    fileLocation: String,
    private val fileSystemManager: FileSystemManager,
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

    override suspend fun export(uriString: String) {
        if (accumulatedLogs.isNotEmpty()) {
            writeToLogFile()
        }
        val sink = fileSystemManager.openSink(uriString, "wt")
            ?: error("Error opening contentUri for writing")

        copyInternalFileToExternal(sink)

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

    private fun copyInternalFileToExternal(sink: RawSink) {
        val inputStream = FileInputStream(logFile)

        try {
            val buffer = Buffer()
            val byteArray = ByteArray(BUFFER_SIZE)
            var bytesRead: Int

            while (inputStream.read(byteArray).also { bytesRead = it } != -1) {
                buffer.write(byteArray, 0, bytesRead)
                sink.write(buffer, buffer.size)
                buffer.clear()
            }

            sink.flush()
        } finally {
            inputStream.close()
            sink.close()
        }
    }
}
