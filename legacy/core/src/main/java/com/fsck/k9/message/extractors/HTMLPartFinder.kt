package com.fsck.k9.message.extractors

import com.fsck.k9.mail.MimeType.Companion.toMimeType
import com.fsck.k9.mail.MimeType.Companion.toMimeTypeOrNull
import com.fsck.k9.mail.Multipart
import com.fsck.k9.mail.Part

private val TEXT_HTML = "text/html".toMimeType()

class HTMLPartFinder {
    fun findFirstHTMLPart(part: Part): Part? {
        val mimeType = part.mimeType.toMimeTypeOrNull()
        val body = part.body

        return if (body is Multipart) {
            findHTMLPartInMultipart(body)
        } else if (mimeType == TEXT_HTML) {
            part
        } else {
            null
        }
    }

    private fun findHTMLPartInMultipart(multipart: Multipart): Part? {
        for (bodyPart in multipart.bodyParts) {
            val mimeType = bodyPart.mimeType.toMimeTypeOrNull()
            val body = bodyPart.body

            if (body is Multipart) {
                return findFirstHTMLPart(bodyPart) ?: continue
            } else if (mimeType == TEXT_HTML) {
                return bodyPart
            }
        }

        return null
    }
}
