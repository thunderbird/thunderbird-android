package net.thunderbird.core.file

import com.eygraber.uri.Uri
import com.eygraber.uri.toURI
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class JvmMimeTypeResolver : MimeTypeResolver {

    override fun getMimeType(uri: Uri): MimeType? {
        return try {
            getMimeTypeFromContentType(uri)
        } catch (_: Exception) {
            getMimeTypeFromExtension(uri)
        }
    }

    private fun getMimeTypeFromContentType(uri: Uri): MimeType? {
        val path = Paths.get(uri.toURI())
        val extension = Files.probeContentType(path)
        return when (extension) {
            "image/jpeg" -> MimeType.JPEG
            "image/png" -> MimeType.PNG
            "application/pdf" -> MimeType.PDF
            else -> null
        }
    }

    private fun getMimeTypeFromExtension(uri: Uri): MimeType? {
        val path = uri.path ?: uri.toString()
        val extension = File(path).extension.lowercase()
        return when (extension) {
            "jpeg", "jpg" -> MimeType.JPEG
            "png" -> MimeType.PNG
            "pdf" -> MimeType.PDF
            else -> null
        }
    }
}
