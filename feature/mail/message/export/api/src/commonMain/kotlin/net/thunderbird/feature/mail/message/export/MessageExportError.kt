package net.thunderbird.feature.mail.message.export

import com.eygraber.uri.Uri

/**
 * Error type for message export failures.
 *
 * Keep this minimal and format-agnostic. Additional cases can be added later if needed.
 */
sealed interface MessageExportError {
    /** Source or destination couldn't be opened or accessed. */
    data class Unavailable(val uri: Uri, val message: String? = null) : MessageExportError

    /** Generic I/O error while copying/exporting. */
    data class Io(val message: String? = null) : MessageExportError

    /** Fallback when the error type can't be determined. */
    data class Unknown(val message: String? = null) : MessageExportError
}
