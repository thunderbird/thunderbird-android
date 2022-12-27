package com.fsck.k9.message.extractors

import com.fsck.k9.mail.Multipart
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.internet.MimeUtility.isSameMimeType

class TextPartFinder {
    fun findFirstTextPart(part: Part): Part? {
        val mimeType = part.mimeType
        val body = part.body

        return if (body is Multipart) {
            if (isSameMimeType(mimeType, "multipart/alternative")) {
                findTextPartInMultipartAlternative(body)
            } else {
                findTextPartInMultipart(body)
            }
        } else if (isSameMimeType(mimeType, "text/plain") || isSameMimeType(mimeType, "text/html")) {
            part
        } else {
            null
        }
    }

    private fun findTextPartInMultipartAlternative(multipart: Multipart): Part? {
        var htmlPart: Part? = null

        for (bodyPart in multipart.bodyParts) {
            val mimeType = bodyPart.mimeType
            val body = bodyPart.body

            if (body is Multipart) {
                val candidatePart = findFirstTextPart(bodyPart)
                if (candidatePart != null) {
                    htmlPart = if (isSameMimeType(candidatePart.mimeType, "text/html")) {
                        candidatePart
                    } else {
                        return candidatePart
                    }
                }
            } else if (isSameMimeType(mimeType, "text/plain")) {
                return bodyPart
            } else if (isSameMimeType(mimeType, "text/html") && htmlPart == null) {
                htmlPart = bodyPart
            }
        }

        return htmlPart
    }

    private fun findTextPartInMultipart(multipart: Multipart): Part? {
        for (bodyPart in multipart.bodyParts) {
            val mimeType = bodyPart.mimeType
            val body = bodyPart.body

            if (body is Multipart) {
                val candidatePart = findFirstTextPart(bodyPart)
                if (candidatePart != null) {
                    return candidatePart
                }
            } else if (isSameMimeType(mimeType, "text/plain") || isSameMimeType(mimeType, "text/html")) {
                return bodyPart
            }
        }

        return null
    }
}
