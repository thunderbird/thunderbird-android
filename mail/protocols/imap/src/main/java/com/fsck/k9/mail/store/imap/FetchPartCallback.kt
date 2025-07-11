package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.BodyFactory
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.filter.FixedLengthInputStream
import com.fsck.k9.mail.internet.MimeHeader
import java.io.IOException

internal class FetchPartCallback(private val part: Part, private val bodyFactory: BodyFactory) :
    ImapResponseCallback {
    @Throws(IOException::class)
    override fun foundLiteral(response: ImapResponse, literal: FixedLengthInputStream): Any? {
        if (response.tag == null && ImapResponseParser.equalsIgnoreCase(response[1], "FETCH")) {
            // TODO: check for correct UID

            val contentTransferEncoding = part.getHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING)[0]
            val contentType = part.getHeader(MimeHeader.HEADER_CONTENT_TYPE)[0]

            return bodyFactory.createBody(contentTransferEncoding, contentType, literal)
        }
        return null
    }
}
