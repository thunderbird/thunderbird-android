package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.store.imap.ImapResponseParser.equalsIgnoreCase

internal class UidValidityResponse private constructor(val uidValidity: Long) {
    companion object {
        @JvmStatic
        fun parse(response: ImapResponse): UidValidityResponse? {
            if (response.isTagged || !equalsIgnoreCase(response[0], Responses.OK) || !response.isList(1)) return null

            val responseTextList = response.getList(1)
            if (responseTextList.size < 2 || !equalsIgnoreCase(responseTextList[0], Responses.UIDVALIDITY) ||
                !responseTextList.isLong(1)
            ) {
                return null
            }

            val uidValidity = responseTextList.getLong(1)
            if (uidValidity !in 0L..0xFFFFFFFFL) return null

            return UidValidityResponse(uidValidity)
        }
    }
}
