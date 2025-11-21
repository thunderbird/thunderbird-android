package net.thunderbird.feature.account.settings.impl.domain.usecase

import com.eygraber.uri.Uri
import net.thunderbird.core.file.MimeType
import net.thunderbird.core.file.MimeTypeResolver

class FakeMimeTypeResolver(
    private val mimeTypeMap: Map<Uri, MimeType?> = emptyMap(),
) : MimeTypeResolver {
    override fun getMimeType(uri: Uri): MimeType? = mimeTypeMap[uri]
}
