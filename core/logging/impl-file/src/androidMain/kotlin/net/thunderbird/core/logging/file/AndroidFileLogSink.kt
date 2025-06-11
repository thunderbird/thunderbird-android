package net.thunderbird.core.logging.file

import android.content.ContentResolver
import android.net.Uri
import androidx.core.net.toUri
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.thunderbird.core.logging.LogEvent
import net.thunderbird.core.logging.LogLevel

private const val ANDROID_LOG_TIME_FORMAT = "MM-dd-yy hh:mm:ss.SSS"

open class AndroidFileLogSink(
    override val level: LogLevel,
    fileName: String,
    fileLocation: String,
    val contentResolver: ContentResolver,
    coroutineContext: CoroutineContext = Dispatchers.IO,
) : FileLogSink {

    private val coroutineScope = CoroutineScope(coroutineContext + SupervisorJob())
    private val writeFile = File(fileLocation, "$fileName.txt")
    private val accumulatedLogs = ConcurrentHashMap<String, String>()

    override fun log(event: LogEvent) {
        try {
            accumulatedLogs[convertLongToTime(event.timestamp)] = "priority = ${event.level}, ${event.message}"
            createLogFile()
        } catch (e: FileSystemException) {
            throw e
        }
    }
    private fun createLogFile() =
        coroutineScope.launch {
            writeToLogFile()
        }
    private fun convertLongToTime(long: Long): String {
        val date = Date(long)
        val format = SimpleDateFormat(ANDROID_LOG_TIME_FORMAT, Locale.US)
        return format.format(date)
    }

    private fun writeToLogFile() {
        val result = runCatching {
            writeFile.bufferedWriter().use {
                it.write(accumulatedLogs.entries.joinToString("\n") { it2 -> it2.key + " " + it2.value })
            }
        }
        if (result.isFailure) {
            result.exceptionOrNull()?.printStackTrace()
        }
    }

    override fun flushAndCloseBuffer() {
        writeFile.bufferedWriter().close()
    }

    override fun export(uriString: String) {
        coroutineScope.launch {
            try {
                val uri: Uri = uriString.toUri()
                val outputStream =
                    contentResolver.openOutputStream(uri, "wt") ?: error("Error opening contentUri for writing")
                copyInternalFileToExternal(outputStream)
                writeFile.bufferedWriter().write("")
            } catch (e: Exception) {
                throw e
            }
        }
    }

    private fun copyInternalFileToExternal(outputStream: OutputStream) {
        outputStream.use {
            try {
                writeFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } catch (e: FileSystemException) {
                throw e
            }
        }
    }
}
