package com.fsck.k9.account

import net.thunderbird.core.android.account.DeletePolicy
import net.thunderbird.core.common.mail.Protocols

class DefaultDeletePolicyProvider : DeletePolicyProvider {
    override fun getDeletePolicy(accountType: String): DeletePolicy {
        return when (accountType) {
            Protocols.IMAP -> DeletePolicy.ON_DELETE
            Protocols.GRAPH,
            Protocols.GRAPH_LEGACY,
            Protocols.GMAIL_API,
            Protocols.ZOHO_MAIL_API,
            -> DeletePolicy.ON_DELETE
            Protocols.POP3 -> DeletePolicy.NEVER
            "demo" -> DeletePolicy.ON_DELETE
            else -> throw AssertionError("Unhandled case: $accountType")
        }
    }
}
