package net.thunderbird.core.file.command

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.eygraber.uri.toKmpUri
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.file.FakeFileSystemManager
import net.thunderbird.core.file.FileOperationError
import net.thunderbird.core.outcome.Outcome

class DeleteCommandTest {

    private val fs = FakeFileSystemManager()
    private val targetUri = "mem://target".toKmpUri()

    @Test
    fun `execute should delete existing file and return success`() = runTest {
        // Arrange
        fs.put("mem://target", "content".encodeToByteArray())
        val cmd = DeleteCommand(targetUri)

        // Act
        val result = cmd(fs)

        // Assert
        assertThat(result.isSuccess).isEqualTo(true)
        assertThat(fs.get("mem://target") == null).isEqualTo(true)
    }

    @Test
    fun `execute should succeed when file does not exist`() = runTest {
        // Arrange - no file preloaded
        val cmd = DeleteCommand(targetUri)

        // Act
        val result = cmd(fs)

        // Assert
        assertThat(result.isSuccess).isEqualTo(true)
    }

    @Test
    fun `execute should map IOException to Outcome_Failure_Unavailable`() = runTest {
        // Arrange
        fs.put("mem://target", "content".encodeToByteArray())
        fs.nextDeleteThrowsIOException = true
        val cmd = DeleteCommand(targetUri)

        // Act
        val result = cmd(fs)

        // Assert
        assertThat(result is Outcome.Failure<FileOperationError>).isEqualTo(true)
        val failure = result as Outcome.Failure<FileOperationError>
        assertThat(failure.error is FileOperationError.Unavailable).isEqualTo(true)
    }
}
