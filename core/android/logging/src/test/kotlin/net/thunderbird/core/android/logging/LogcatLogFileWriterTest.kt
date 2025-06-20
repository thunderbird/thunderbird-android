package net.thunderbird.core.android.logging

import android.content.ContentResolver
import android.net.Uri
import assertk.assertThat
import assertk.assertions.isEqualTo
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class LogcatLogFileWriterTest {
    private val contentUri = mock<Uri>()
    private val outputStream = ByteArrayOutputStream()

    @Test
    fun `write log to contentUri`() = runBlocking {
        val logData = "a".repeat(10_000)
        val logFileWriter = LogcatLogFileWriter(
            contentResolver = createContentResolver(),
            processExecutor = createProcessExecutor(logData),
            coroutineDispatcher = Dispatchers.Unconfined,
        )

        logFileWriter.writeLogTo(contentUri)

        assertThat(outputStream.toByteArray().decodeToString()).isEqualTo(logData)
    }

    @Test(expected = FileNotFoundException::class)
    fun `contentResolver throws`() = runBlocking {
        val logFileWriter = LogcatLogFileWriter(
            contentResolver = createThrowingContentResolver(FileNotFoundException()),
            processExecutor = createProcessExecutor("irrelevant"),
            coroutineDispatcher = Dispatchers.Unconfined,
        )

        logFileWriter.writeLogTo(contentUri)
    }

    @Test(expected = IOException::class)
    fun `processExecutor throws`() = runBlocking {
        val logFileWriter = LogcatLogFileWriter(
            contentResolver = createContentResolver(),
            processExecutor = ThrowingProcessExecutor(IOException()),
            coroutineDispatcher = Dispatchers.Unconfined,
        )

        logFileWriter.writeLogTo(contentUri)
    }

    private fun createContentResolver(): ContentResolver {
        return mock {
            on { openOutputStream(contentUri, "wt") } doReturn outputStream
        }
    }

    private fun createThrowingContentResolver(exception: Exception): ContentResolver {
        return mock {
            on { openOutputStream(contentUri, "wt") } doAnswer { throw exception }
        }
    }

    private fun createProcessExecutor(logData: String): DataProcessExecutor {
        return DataProcessExecutor(logData.toByteArray(charset = Charsets.US_ASCII))
    }
}

private class DataProcessExecutor(val data: ByteArray) : ProcessExecutor {
    override fun exec(command: String): InputStream {
        return ByteArrayInputStream(data)
    }
}

private class ThrowingProcessExecutor(val exception: Exception) : ProcessExecutor {
    override fun exec(command: String): InputStream {
        throw exception
    }
}
