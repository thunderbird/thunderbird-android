package net.thunderbird.core.file

import com.eygraber.uri.Uri

class FakeMimeTypeProvider(
    private val mimeTypeMap: Map<Uri, String?> = emptyMap(),
) : AndroidMimeTypeProvider {

    override fun getType(uri: Uri): String? = mimeTypeMap[uri]
}
