package app.k9mail.feature.widget.unread

import android.content.Intent
import android.os.Bundle
import com.fsck.k9.activity.AccountList
import net.thunderbird.feature.mail.account.api.BaseAccount
import net.thunderbird.feature.search.legacy.SearchAccount

class UnreadWidgetChooseAccountActivity : AccountList() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.unread_widget_choose_account_title)
    }

    override fun onAccountSelected(account: Any) {
        val accountUuid = when (account) {
            is SearchAccount -> account.id
            is BaseAccount -> account.id.toString()
            else -> error("Unknown account type: $account")
        }

        val intent = Intent().apply {
            putExtra(EXTRA_ACCOUNT_UUID, accountUuid)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    companion object {
        const val EXTRA_ACCOUNT_UUID: String = "com.fsck.k9.ChooseAccount_account_uuid"
    }
}
