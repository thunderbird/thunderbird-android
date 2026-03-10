package net.thunderbird.core.file

import com.eygraber.uri.Uri

/**
 * Resolver for MIME types.
 */
interface MimeTypeResolver {
    fun getMimeType(uri: Uri): MimeType?
}
