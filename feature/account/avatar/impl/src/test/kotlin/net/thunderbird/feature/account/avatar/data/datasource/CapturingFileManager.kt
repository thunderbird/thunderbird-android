package net.thunderbird.feature.account.avatar.data.datasource

import com.eygraber.uri.Uri
import net.thunderbird.core.file.FileManager
import net.thunderbird.core.file.FileOperationError
import net.thunderbird.core.outcome.Outcome

internal class CapturingFileManager : FileManager {
    var lastCopySource = null as Uri?
    var lastCopyDestination = null as Uri?
    var lastDeleted = null as Uri?
    var lastCreatedDir = null as Uri?

    override suspend fun copy(
        sourceUri: Uri,
        destinationUri: Uri,
    ): Outcome<Unit, FileOperationError> {
        lastCopySource = sourceUri
        lastCopyDestination = destinationUri
        return Outcome.Success(Unit)
    }

    override suspend fun delete(uri: Uri): Outcome<Unit, FileOperationError> {
        lastDeleted = uri
        return Outcome.Success(Unit)
    }

    override suspend fun createDirectories(uri: Uri): Outcome<Unit, FileOperationError> {
        lastCreatedDir = uri
        return Outcome.Success(Unit)
    }
}
