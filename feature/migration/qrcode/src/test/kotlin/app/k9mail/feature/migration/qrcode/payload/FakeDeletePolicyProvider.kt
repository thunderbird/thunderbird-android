package app.k9mail.feature.migration.qrcode.payload

import app.k9mail.legacy.account.Account.DeletePolicy
import com.fsck.k9.account.DeletePolicyProvider

class FakeDeletePolicyProvider : DeletePolicyProvider {
    override fun getDeletePolicy(accountType: String): DeletePolicy {
        return DELETE_POLICY
    }

    companion object {
        val DELETE_POLICY = DeletePolicy.ON_DELETE
    }
}
