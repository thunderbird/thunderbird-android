package app.k9mail.feature.settings.push.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import app.k9mail.core.ui.compose.theme.K9Theme
import com.fsck.k9.Account.FolderMode
import com.fsck.k9.ui.base.K9Activity

/**
 * Screen to change the "Push folders" setting of an account.
 */
class PushFoldersActivity : K9Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val accountUuid = intent.getStringExtra(EXTRA_ACCOUNT_UUID) ?: error("Missing extra '$EXTRA_ACCOUNT_UUID'")

        setContent {
            K9Theme {
                PushFoldersScreen(
                    accountUuid = accountUuid,
                    onOptionSelected = ::onPushFoldersChanged,
                    onBack = { finish() },
                )
            }
        }
    }

    private fun onPushFoldersChanged(result: FolderMode) {
        val resultIntent = Intent().apply {
            putExtra(EXTRA_RESULT, result.name)
        }

        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    class ResultContract : ActivityResultContract<String, FolderMode?>() {
        override fun createIntent(context: Context, input: String): Intent {
            return Intent(context, PushFoldersActivity::class.java).apply {
                putExtra(EXTRA_ACCOUNT_UUID, input)
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): FolderMode? {
            return if (resultCode == Activity.RESULT_OK && intent != null) {
                intent.getStringExtra(EXTRA_RESULT)?.let { FolderMode.valueOf(it) }
            } else {
                null
            }
        }
    }

    companion object {
        private const val EXTRA_ACCOUNT_UUID = "accountUuid"
        private const val EXTRA_RESULT = "result"
    }
}
