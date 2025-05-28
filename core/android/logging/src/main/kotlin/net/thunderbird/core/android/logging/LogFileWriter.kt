package net.thunderbird.core.android.logging

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.IOUtils
import timber.log.Timber

interface LogFileWriter {
    suspend fun writeLogTo(contentUri: Uri)
}

class MultiLogFileWriter(
    private val contentResolver: ContentResolver,
    private val processExecutor: ProcessExecutor,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val context: Context?,
) : LogFileWriter {
    override suspend fun writeLogTo(contentUri: Uri) {
        return withContext(coroutineDispatcher) {
            Timber.v("Writing output to content URI: %s", contentUri)
            var uriString = ""
            contentResolver.query(contentUri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                uriString = cursor.getString(nameIndex)
            }
            val outputStream = contentResolver.openOutputStream(contentUri, "wt")
                ?: error("Error opening contentUri for writing")
            if (uriString.contains("thunderbird-sync-logs")) {
                outputStream.use {
                    try {
                        context?.openFileInput("thunderbird-sync-logs.txt").use { inputStream ->
                            IOUtils.copy(inputStream, outputStream)
                        }
                    } catch (e: FileSystemException) {
                        println(e)
                    }
                }
                context?.openFileOutput("thunderbird-sync-logs.txt", Context.MODE_PRIVATE)?.bufferedWriter()?.write("")
            } else {
                outputStream.use {
                    processExecutor.exec("logcat -d").use { inputStream ->
                        IOUtils.copy(inputStream, outputStream)
                    }
                }
            }
        }
    }
}
