package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.filter.FixedLengthInputStream
import java.io.IOException

internal class FetchBodyCallback(private val mMessageMap: Map<String, ImapMessage>) : ImapResponseCallback {
    @Throws(MessagingException::class, IOException::class)
    override fun foundLiteral(
        response: ImapResponse,
        literal: FixedLengthInputStream,
    ): Any? {
        if (response.tag == null &&
            ImapResponseParser.equalsIgnoreCase(response[1], "FETCH")
        ) {
            val fetchList = response.getKeyedValue("FETCH") as ImapList
            val uid = fetchList.getKeyedString("UID")

            val message = mMessageMap[uid]
            message?.parse(literal)

            // Return placeholder object
            return 1
        }
        return null
    }
}
