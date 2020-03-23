package com.fsck.k9.mail.internet

fun getMimeTypes(): List<String> {
    return MimeUtility.MIME_TYPE_BY_EXTENSION_MAP.map { it[1] }
}
