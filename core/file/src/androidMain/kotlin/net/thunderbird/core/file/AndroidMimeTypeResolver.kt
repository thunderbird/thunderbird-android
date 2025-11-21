package net.thunderbird.core.file

import com.eygraber.uri.Uri

class AndroidMimeTypeResolver(
    private val mimeTypeProvider: AndroidMimeTypeProvider,
) : MimeTypeResolver {

    override fun getMimeType(uri: Uri): MimeType? {
        return when (mimeTypeProvider.getType(uri)) {
            "image/jpeg" -> MimeType.JPEG
            "image/png" -> MimeType.PNG
            "application/pdf" -> MimeType.PDF
            else -> null
        }
    }
}
