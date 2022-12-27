package com.fsck.k9.message.extractors

import com.fsck.k9.mail.MimeType.Companion.toMimeType
import com.fsck.k9.mail.MimeType.Companion.toMimeTypeOrNull
import com.fsck.k9.mail.Multipart
import com.fsck.k9.mail.Part

private val TEXT_PLAIN = "text/plain".toMimeType()
private val TEXT_HTML = "text/html".toMimeType()
private val MULTIPART_ALTERNATIVE = "multipart/alternative".toMimeType()

class TextPartFinder {
    fun findFirstTextPart(part: Part): Part? {
        val mimeType = part.mimeType.toMimeTypeOrNull()
        val body = part.body

        return if (body is Multipart) {
            if (mimeType == MULTIPART_ALTERNATIVE) {
                findTextPartInMultipartAlternative(body)
            } else {
                findTextPartInMultipart(body)
            }
        } else if (mimeType == TEXT_PLAIN || mimeType == TEXT_HTML) {
            part
        } else {
            null
        }
    }

    private fun findTextPartInMultipartAlternative(multipart: Multipart): Part? {
        var htmlPart: Part? = null

        for (bodyPart in multipart.bodyParts) {
            val mimeType = bodyPart.mimeType.toMimeTypeOrNull()
            val body = bodyPart.body

            if (body is Multipart) {
                val candidatePart = findFirstTextPart(bodyPart) ?: continue
                if (mimeType == TEXT_PLAIN) {
                    return candidatePart
                }

                htmlPart = candidatePart
            } else if (mimeType == TEXT_PLAIN) {
                return bodyPart
            } else if (mimeType == TEXT_HTML && htmlPart == null) {
                htmlPart = bodyPart
            }
        }

        return htmlPart
    }

    private fun findTextPartInMultipart(multipart: Multipart): Part? {
        for (bodyPart in multipart.bodyParts) {
            val mimeType = bodyPart.mimeType.toMimeTypeOrNull()
            val body = bodyPart.body

            if (body is Multipart) {
                return findFirstTextPart(bodyPart) ?: continue
            } else if (mimeType == TEXT_PLAIN || mimeType == TEXT_HTML) {
                return bodyPart
            }
        }

        return null
    }
}
