package net.thunderbird.core.file

import com.eygraber.uri.Uri

/**
 * Common file operation errors.
 */
sealed interface FileOperationError {
    /** Endpoint couldn't be opened or accessed. */
    data class Unavailable(val uri: Uri, val message: String? = null) : FileOperationError

    /** Failed while reading from the source. */
    data class ReadFailed(val uri: Uri, val message: String? = null) : FileOperationError

    /** Failed while writing to the destination. */
    data class WriteFailed(val uri: Uri, val message: String? = null) : FileOperationError

    /** Fallback when the error type can't be determined. */
    data class Unknown(val message: String? = null) : FileOperationError
}
