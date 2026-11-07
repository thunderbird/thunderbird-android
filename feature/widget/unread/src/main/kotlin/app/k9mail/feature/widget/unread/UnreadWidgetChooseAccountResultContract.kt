package app.k9mail.feature.widget.unread

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory

internal class UnreadWidgetChooseAccountResultContract : ActivityResultContract<Unit, AccountId?>() {
    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(context, UnreadWidgetChooseAccountActivity::class.java)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): AccountId? {
        return intent?.takeIf { resultCode == Activity.RESULT_OK }
            ?.getStringExtra(UnreadWidgetChooseAccountActivity.EXTRA_ACCOUNT_UUID)
            ?.let { accountIdRaw -> runCatching { AccountIdFactory.of(accountIdRaw) }.getOrNull() }
    }
}
