package com.fsck.k9.account

import app.k9mail.core.common.mail.Protocols
import com.fsck.k9.Account.DeletePolicy

/**
 * Deals with logic surrounding account creation.
 */
class AccountCreatorHelper {

    fun getDefaultDeletePolicy(type: String): DeletePolicy {
        return when (type) {
            Protocols.IMAP -> DeletePolicy.ON_DELETE
            Protocols.POP3 -> DeletePolicy.NEVER
            "demo" -> DeletePolicy.ON_DELETE
            else -> throw AssertionError("Unhandled case: $type")
        }
    }
}
