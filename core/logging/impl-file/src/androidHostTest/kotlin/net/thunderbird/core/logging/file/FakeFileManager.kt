package net.thunderbird.core.logging.file

import com.eygraber.uri.Uri
import com.eygraber.uri.toAndroidUri
import java.io.File
import net.thunderbird.core.file.FileManager
import net.thunderbird.core.file.FileOperationError
import net.thunderbird.core.outcome.Outcome

/**
 * Fake FileManager that captures content copied from a local file source URI.
 */
class FakeFileManager : FileManager {
    var exportedContent: String? = null

    override suspend fun copy(
        sourceUri: Uri,
        destinationUri: Uri,
    ): Outcome<Unit, FileOperationError> {
        return try {
            val androidUri = sourceUri.toAndroidUri()
            val content = when (androidUri.scheme) {
                "file" -> {
                    val path = requireNotNull(androidUri.path) { "File URI without path: $androidUri" }
                    File(path).readText(Charsets.UTF_8)
                }
                else -> error("Unsupported scheme for FakeFileManager source: ${androidUri.scheme}")
            }
            exportedContent = content
            Outcome.Success(Unit)
        } catch (t: Throwable) {
            Outcome.Failure(FileOperationError.Unknown(t.message), cause = t)
        }
    }

    override suspend fun delete(uri: Uri): Outcome<Unit, FileOperationError> {
        return Outcome.Success(Unit)
    }

    override suspend fun createDirectories(uri: Uri): Outcome<Unit, FileOperationError> {
        return Outcome.Success(Unit)
    }
}
