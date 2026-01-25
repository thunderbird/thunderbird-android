package com.fsck.k9.ui.settings.notificationactions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.BaseActivity
import com.fsck.k9.ui.base.extensions.fragmentTransaction

/**
 * Hosts the notification action configuration screen.
 */
class NotificationActionsSettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLayout(R.layout.general_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.notification_actions_settings_title)

        if (savedInstanceState == null) {
            fragmentTransaction {
                replace(
                    R.id.generalSettingsContainer,
                    NotificationActionsSettingsFragment(),
                )
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, NotificationActionsSettingsActivity::class.java))
        }
    }
}
