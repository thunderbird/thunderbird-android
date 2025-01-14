package app.k9mail.feature.widget.unread

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

internal class UnreadWidgetChooseAccountResultContract : ActivityResultContract<Unit, String?>() {
    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(context, UnreadWidgetChooseAccountActivity::class.java)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): String? {
        return intent?.getStringExtra(UnreadWidgetChooseAccountActivity.EXTRA_ACCOUNT_UUID)
            .takeIf { resultCode == Activity.RESULT_OK }
    }
}
