package com.fsck.k9.mail.store.imap

import java.util.Locale

internal class EnabledResponse private constructor(val capabilities: Set<String>) {

    companion object {
        fun parse(responses: List<ImapResponse>): EnabledResponse? {
            var result: EnabledResponse? = null
            for (response in responses) {
                if (result == null && response.tag == null) {
                    result = parse(response)
                }
            }
            return result
        }

        private fun parse(capabilityList: ImapList): EnabledResponse? {
            if (capabilityList.isEmpty() || !equalsIgnoreCase(capabilityList[0], Responses.ENABLED)) {
                return null
            }
            val capabilities = mutableSetOf<String>()
            for (i in 1 until capabilityList.size) {
                if (!capabilityList.isString(i)) {
                    return null
                }
                capabilities.add(capabilityList.getString(i).uppercase(Locale.US))
            }
            return EnabledResponse(capabilities)
        }
    }
}
