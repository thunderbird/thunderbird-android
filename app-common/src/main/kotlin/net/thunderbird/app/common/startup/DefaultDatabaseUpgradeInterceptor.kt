package net.thunderbird.app.common.startup

import android.content.Context
import android.content.Intent
import com.fsck.k9.K9
import com.fsck.k9.activity.UpgradeDatabases
import net.thunderbird.core.android.common.startup.DatabaseUpgradeInterceptor

/**
 * Intercepts app startup to check if database upgrade is required.
 *
 * If databases need upgrading, this will launch the [UpgradeDatabases] activity
 * and return true, indicating the calling activity should finish.
 */
class DefaultDatabaseUpgradeInterceptor : DatabaseUpgradeInterceptor {
    override fun checkAndHandleUpgrade(context: Context, intent: Intent): Boolean {
        if (K9.areDatabasesUpToDate()) {
            return false
        }

        val upgradeIntent = Intent(context, UpgradeDatabases::class.java).apply {
            action = UpgradeDatabases.ACTION_UPGRADE_DATABASES
            putExtra(UpgradeDatabases.EXTRA_START_INTENT, intent)

            // Make sure this activity is only running once
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        context.startActivity(upgradeIntent)
        return true
    }
}
