package com.fsck.k9.account

import net.thunderbird.core.android.account.DeletePolicy
import net.thunderbird.core.common.mail.Protocols

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
