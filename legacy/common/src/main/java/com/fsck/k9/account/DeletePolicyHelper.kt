package com.fsck.k9.account

import app.k9mail.core.common.mail.Protocols
import app.k9mail.legacy.account.Account

object DeletePolicyHelper {
    fun getDefaultDeletePolicy(type: String): Account.DeletePolicy {
        return when (type) {
            Protocols.IMAP -> Account.DeletePolicy.ON_DELETE
            Protocols.POP3 -> Account.DeletePolicy.NEVER
            "demo" -> Account.DeletePolicy.ON_DELETE
            else -> throw AssertionError("Unhandled case: $type")
        }
    }
}
