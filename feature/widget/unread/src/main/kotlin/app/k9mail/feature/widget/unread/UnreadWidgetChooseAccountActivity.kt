package app.k9mail.feature.widget.unread

import android.content.Intent
import android.os.Bundle
import com.fsck.k9.activity.AccountList
import net.thunderbird.core.account.BaseAccount

class UnreadWidgetChooseAccountActivity : AccountList() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.unread_widget_choose_account_title)
    }

    override fun onAccountSelected(account: BaseAccount) {
        val intent = Intent().apply {
            putExtra(EXTRA_ACCOUNT_UUID, account.uuid)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    companion object {
        const val EXTRA_ACCOUNT_UUID: String = "com.fsck.k9.ChooseAccount_account_uuid"
    }
}
