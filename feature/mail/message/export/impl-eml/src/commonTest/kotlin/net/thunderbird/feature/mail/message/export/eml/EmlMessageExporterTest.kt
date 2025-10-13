package net.thunderbird.feature.mail.message.export.eml

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.eygraber.uri.toKmpUri
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.file.FileOperationError
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.mail.message.export.MessageExportError

class EmlMessageExporterTest {

    private val fakeFileManager = FakeFileManager()
    private val testSubject = EmlMessageExporter(fileManager = fakeFileManager)

    @Test
    fun `export should delegate to FileManager and return success`() = runTest {
        // Arrange
        val source = "mem://source.eml".toKmpUri()
        val dest = "mem://dest.eml".toKmpUri()
        fakeFileManager.nextResult = Outcome.Success(Unit)

        // Act
        val result = testSubject.export(sourceUri = source, destinationUri = dest)

        // Assert
        assertThat(result.isSuccess).isEqualTo(true)
        assertThat(fakeFileManager.lastSource).isEqualTo(source)
        assertThat(fakeFileManager.lastDestination).isEqualTo(dest)
    }

    @Test
    fun `export should map Unavailable error`() = runTest {
        // Arrange
        val source = "mem://source.eml".toKmpUri()
        val dest = "mem://dest.eml".toKmpUri()
        fakeFileManager.nextResult = Outcome.Failure(
            FileOperationError.Unavailable(uri = source, message = "cannot open"),
        )

        // Act
        val result = testSubject.export(sourceUri = source, destinationUri = dest)

        // Assert
        assertThat(result.isFailure).isEqualTo(true)
        val failure = result as Outcome.Failure
        assertThat(failure.error).isInstanceOf(MessageExportError.Unavailable::class)
        assertThat((failure.error as MessageExportError.Unavailable).uri).isEqualTo(source)
    }

    @Test
    fun `export should map ReadFailed to Io`() = runTest {
        // Arrange
        val source = "mem://source.eml".toKmpUri()
        val dest = "mem://dest.eml".toKmpUri()
        fakeFileManager.nextResult = Outcome.Failure(
            FileOperationError.ReadFailed(uri = source, message = "read err"),
        )

        // Act
        val result = testSubject.export(sourceUri = source, destinationUri = dest)

        // Assert
        assertThat(result.isFailure).isEqualTo(true)
        val failure = result as Outcome.Failure
        assertThat(failure.error).isInstanceOf(MessageExportError.Io::class)
    }

    @Test
    fun `export should map WriteFailed to Io`() = runTest {
        // Arrange
        val source = "mem://source.eml".toKmpUri()
        val dest = "mem://dest.eml".toKmpUri()
        fakeFileManager.nextResult = Outcome.Failure(
            FileOperationError.WriteFailed(uri = dest, message = "write err"),
        )

        // Act
        val result = testSubject.export(sourceUri = source, destinationUri = dest)

        // Assert
        assertThat(result.isFailure).isEqualTo(true)
        val failure = result as Outcome.Failure
        assertThat(failure.error).isInstanceOf(MessageExportError.Io::class)
    }

    @Test
    fun `export should map Unknown error`() = runTest {
        // Arrange
        val source = "mem://source.eml".toKmpUri()
        val dest = "mem://dest.eml".toKmpUri()
        fakeFileManager.nextResult = Outcome.Failure(
            FileOperationError.Unknown(message = "mystery"),
        )

        // Act
        val result = testSubject.export(sourceUri = source, destinationUri = dest)

        // Assert
        assertThat(result.isFailure).isEqualTo(true)
        val failure = result as Outcome.Failure
        assertThat(failure.error).isInstanceOf(MessageExportError.Unknown::class)
    }
}
