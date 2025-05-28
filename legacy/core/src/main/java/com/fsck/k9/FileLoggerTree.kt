package com.fsck.k9

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

class FileLoggerTree(private val context: Context) : Timber.Tree() {
    private val coroutineContext: CoroutineContext = Dispatchers.IO
    private val coroutineScope = CoroutineScope(coroutineContext + SupervisorJob())

    private val writeFile = AtomicReference<File>()
    private val accumulatedLogs = ConcurrentHashMap<String, String>()

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (message.contains("sync")) {
            try {
                accumulatedLogs[convertLongToTime(System.currentTimeMillis())] = "priority = $priority, $message"
                createLogFile()
            } catch (e: FileSystemException) {
                Timber.e(" Error while logging into file: $e")
            }
        }
    }

    private fun createLogFile() =
        coroutineScope.launch {
            writeFile.lazySet(
                context.createFile(fileName = "$DEFAULT_SYNC_FILENAME.txt"),
            )
            writeToLogFile()
        }

    private suspend fun writeToLogFile() {
        val result = runCatching {
            writeFile.get().bufferedWriter().use { it.write(accumulatedLogs.toString()) }
        }
        if (result.isFailure) {
            result.exceptionOrNull()?.printStackTrace()
        }
    }

    private fun convertLongToTime(long: Long): String {
        val date = Date(long)
        val format = SimpleDateFormat(ANDROID_LOG_TIME_FORMAT, Locale.US)
        return format.format(date)
    }
    companion object {
        private const val ANDROID_LOG_TIME_FORMAT = "MM-dd-yy kk:mm:ss.SSS"
        const val DEFAULT_SYNC_FILENAME = "thunderbird-sync-logs"
    }

    private fun Context.createFile(fileName: String): File {
        return File(filesDir, fileName)
    }
}
