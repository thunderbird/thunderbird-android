package app.k9mail.feature.migration.qrcode.payload

import com.fsck.k9.account.DeletePolicyProvider
import net.thunderbird.core.android.account.DeletePolicy

class FakeDeletePolicyProvider : DeletePolicyProvider {
    override fun getDeletePolicy(accountType: String): DeletePolicy {
        return DELETE_POLICY
    }

    companion object {
        val DELETE_POLICY = DeletePolicy.ON_DELETE
    }
}
