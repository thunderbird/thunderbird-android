package com.fsck.k9.activity

import android.content.Intent
import android.os.Bundle
import com.fsck.k9.BaseAccount
import com.fsck.k9.search.SearchAccount
import com.fsck.k9.ui.R

class LauncherShortcuts : AccountList() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.shortcuts_title)

        // finish() immediately if we aren't supposed to be here
        if (intent.action != Intent.ACTION_CREATE_SHORTCUT) {
            finish()
        }
    }

    override fun onAccountSelected(account: BaseAccount) {
        val shortcutIntent = if (account is SearchAccount) {
            MessageList.shortcutIntent(this, account.id)
        } else {
            MessageList.shortcutIntentForAccount(this, account.uuid)
        }

        val displayName = account.name ?: account.email
        val iconResId = R.drawable.ic_launcher
        val iconResource = Intent.ShortcutIconResource.fromContext(this, iconResId)

        setResult(
            RESULT_OK,
            createResultIntent(
                shortcutIntent,
                displayName,
                iconResource,
            ),
        )
        finish()
    }

    private fun createResultIntent(
        shortcutIntent: Intent,
        displayName: String,
        iconResource: Intent.ShortcutIconResource,
    ): Intent {
        return Intent().apply {
            putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
            putExtra(Intent.EXTRA_SHORTCUT_NAME, displayName)
            putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource)
        }
    }
}
