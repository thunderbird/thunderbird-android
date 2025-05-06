package com.fsck.k9.mail.store.imap

internal class UidCopyResponse private constructor(val uidMapping: Map<String, String>) {

    companion object {
        fun parse(imapResponses: List<ImapResponse>, allowUntaggedResponse: Boolean = false): UidCopyResponse? {
            val uidMapping = mutableMapOf<String, String>()
            for (imapResponse in imapResponses) {
                parseUidCopyResponse(imapResponse, allowUntaggedResponse, uidMapping)
            }

            return if (uidMapping.isNotEmpty()) {
                UidCopyResponse(uidMapping)
            } else {
                null
            }
        }

        @Suppress("ReturnCount", "ComplexCondition", "MagicNumber")
        private fun parseUidCopyResponse(
            response: ImapResponse,
            allowUntaggedResponse: Boolean,
            uidMappingOutput: MutableMap<String, String>,
        ) {
            if (!(allowUntaggedResponse || response.isTagged) ||
                response.size < 2 ||
                !ImapResponseParser.equalsIgnoreCase(response[0], Responses.OK) ||
                !response.isList(1)
            ) {
                return
            }

            val responseTextList = response.getList(1)
            if (responseTextList.size < 4 ||
                !ImapResponseParser.equalsIgnoreCase(responseTextList[0], Responses.COPYUID) ||
                !responseTextList.isString(1) ||
                !responseTextList.isString(2) ||
                !responseTextList.isString(3)
            ) {
                return
            }

            val sourceUids = ImapUtility.getImapSequenceValues(responseTextList.getString(2))
            val destinationUids = ImapUtility.getImapSequenceValues(responseTextList.getString(3))

            val size = sourceUids.size
            if (size == 0 || size != destinationUids.size) {
                return
            }

            for (i in 0 until size) {
                val sourceUid = sourceUids[i]
                val destinationUid = destinationUids[i]
                uidMappingOutput[sourceUid] = destinationUid
            }
        }
    }
}
