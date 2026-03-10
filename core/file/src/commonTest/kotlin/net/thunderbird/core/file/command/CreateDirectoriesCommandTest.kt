package net.thunderbird.core.file.command

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.eygraber.uri.toKmpUri
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.file.FakeFileSystemManager
import net.thunderbird.core.file.FileOperationError
import net.thunderbird.core.outcome.Outcome

class CreateDirectoriesCommandTest {

    private val fs = FakeFileSystemManager()
    private val dirUri = "mem://some/dir".toKmpUri()

    @Test
    fun `execute should return success when directories are created`() = runTest {
        // Arrange
        val cmd = CreateDirectoriesCommand(dirUri)

        // Act
        val result = cmd(fs)

        // Assert
        assertThat(result.isSuccess).isEqualTo(true)
    }

    @Test
    fun `execute should map IOException to FileOperationError Unavailable`() = runTest {
        // Arrange
        fs.nextCreateDirectoriesThrowsIOException = true
        val cmd = CreateDirectoriesCommand(dirUri)

        // Act
        val result = cmd(fs)

        // Assert
        assertThat(result is Outcome.Failure<FileOperationError>).isEqualTo(true)
        val failure = result as Outcome.Failure<FileOperationError>
        assertThat(failure.error is FileOperationError.Unavailable).isEqualTo(true)
    }
}
