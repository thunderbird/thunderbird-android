package net.thunderbird.core.logging.file

import java.io.File
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
import net.thunderbird.core.logging.LogSink

private const val ANDROID_LOG_TIME_FORMAT = "MM-dd-yy hh:mm:ss.SSS"

open class AndroidFileLogSink(
    override val level: LogLevel,
    private val tagFilters: Array<String>?,
    private val messageFilter: String?,
    fileName: String,
    fileLocation: String,
    coroutineContext: CoroutineContext = Dispatchers.IO,
) : LogSink {

    private val coroutineScope = CoroutineScope(coroutineContext + SupervisorJob())
    private val writeFile = File(fileLocation, "$fileName.txt")
    private val accumulatedLogs = ConcurrentHashMap<String, String>()

    override fun log(event: LogEvent) {
        try {
            if (tagFilters.isNullOrEmpty() || tagFilters.contains(event.tag) || messageFilter?.let {event.message.contains(it)} == true){
                accumulatedLogs[convertLongToTime(event.timestamp)] = "priority = ${event.level}, ${event.message}"
                createLogFile()
            }
        } catch (e: FileSystemException) {
           //do Something
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
}
