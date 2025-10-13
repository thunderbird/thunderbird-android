package net.thunderbird.core.file.command

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.eygraber.uri.toKmpUri
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.file.FakeFileSystemManager
import net.thunderbird.core.file.FileOperationError
import net.thunderbird.core.outcome.Outcome

class CopyCommandTest {

    private val fs = FakeFileSystemManager()
    private val testSubject = CopyCommand(
        sourceUri = "mem://source".toKmpUri(),
        destinationUri = "mem://dest".toKmpUri(),
    )

    @Test
    fun `execute should copy bytes from source to destination`() = runTest {
        // Arrange
        val content = "Thunderbird common copy test".encodeToByteArray()
        fs.put("mem://source", content)

        // Act
        val result = testSubject(fs)

        // Assert
        assertThat(result.isSuccess).isEqualTo(true)
        assertThat(fs.get("mem://dest")?.decodeToString()).isEqualTo("Thunderbird common copy test")
    }

    @Test
    fun `execute should fail when source cannot be opened`() = runTest {
        // Arrange - no source preloaded

        // Act
        val result = testSubject(fs)

        // Assert
        assertThat(result is Outcome.Failure<FileOperationError>).isEqualTo(true)
    }
}
