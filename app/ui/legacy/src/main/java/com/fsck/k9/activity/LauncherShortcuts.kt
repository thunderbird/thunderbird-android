package com.fsck.k9.activity

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import com.fsck.k9.BaseAccount
import com.fsck.k9.activity.MessageList.Companion.shortcutIntent
import com.fsck.k9.activity.MessageList.Companion.shortcutIntentForAccount
import com.fsck.k9.search.SearchAccount
import com.fsck.k9.ui.R

class LauncherShortcuts : AccountList() {
    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setTitle(R.string.shortcuts_title)

        // finish() immediately if we aren't supposed to be here
        if (Intent.ACTION_CREATE_SHORTCUT != intent.action) {
            finish()
        }
    }

    override fun onAccountSelected(account: BaseAccount) {
        val shortcutIntent: Intent
        if (account is SearchAccount) {
            shortcutIntent = shortcutIntent(this, account.id)
        } else {
            shortcutIntent = shortcutIntentForAccount(this, account.uuid)
        }

        val intent = Intent()
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
        val accountName = account.name
        val displayName = accountName ?: account.email
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, displayName)
        val iconResource: Parcelable = Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_launcher)
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource)

        setResult(RESULT_OK, intent)
        finish()
    }
}
