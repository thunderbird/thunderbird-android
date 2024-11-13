package com.fsck.k9.account

import app.k9mail.core.common.mail.Protocols
import app.k9mail.legacy.account.Account.DeletePolicy

/**
 * Decides which [DeletePolicy] an account uses by default.
 */
interface DeletePolicyProvider {
    /**
     * Returns the [DeletePolicy] an account of type [accountType] should use by default.
     *
     * @param accountType The protocol identifier of the incoming server of an account. See [Protocols].
     */
    fun getDeletePolicy(accountType: String): DeletePolicy
}
