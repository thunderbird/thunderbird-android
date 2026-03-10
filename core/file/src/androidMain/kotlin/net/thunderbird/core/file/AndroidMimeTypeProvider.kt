package net.thunderbird.core.file

import com.eygraber.uri.Uri

fun interface AndroidMimeTypeProvider {
    fun getType(uri: Uri): String?
}
