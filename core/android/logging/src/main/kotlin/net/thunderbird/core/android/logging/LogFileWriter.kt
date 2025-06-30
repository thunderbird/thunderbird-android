package net.thunderbird.core.android.logging

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.IOUtils
import timber.log.Timber

interface LogFileWriter {
    suspend fun writeLogTo(contentUri: Uri)
}

class LogcatLogFileWriter(
    private val contentResolver: ContentResolver,
    private val processExecutor: ProcessExecutor,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : LogFileWriter {
    override suspend fun writeLogTo(contentUri: Uri) {
        return withContext(coroutineDispatcher) {
            writeLogBlocking(contentUri)
        }
    }

    private fun writeLogBlocking(contentUri: Uri) {
        Timber.v("Writing logcat output to content URI: %s", contentUri)

        val outputStream = contentResolver.openOutputStream(contentUri, "wt")
            ?: error("Error opening contentUri for writing")

        outputStream.use {
            processExecutor.exec("logcat -d").use { inputStream ->
                IOUtils.copy(inputStream, outputStream)
            }
        }
    }
}
