package com.fsck.k9.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.core.content.IntentCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.fsck.k9.K9.areDatabasesUpToDate
import com.fsck.k9.Preferences
import com.fsck.k9.Preferences.Companion.getPreferences
import com.fsck.k9.service.DatabaseUpgradeService
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.BaseActivity
import com.google.android.material.textview.MaterialTextView

/**
 * This activity triggers a database upgrade if necessary and displays the current upgrade progress.
 *
 * The current upgrade process works as follows:
 *
 *  1. Activities that can be started via an external intent (entry-point activities) use
 * [net.thunderbird.core.android.common.startup.DatabaseUpgradeInterceptor.checkAndHandleUpgrade]
 * in their `onCreate()` and `onNewIntent()` methods.
 *  2. `checkAndHandleUpgrade()` will call [K9.areDatabasesUpToDate]
 * to check if we already know whether the databases have been upgraded.
 *  3. [K9.areDatabasesUpToDate] will compare the last known database version stored in a
 * [SharedPreferences] file to [LocalStore.getDbVersion]. This
 * is done as an optimization because it's faster than opening all the accounts' databases
 * one by one.
 *  4. If there was an error reading the cached database version or if it shows the databases need
 * upgrading this activity (`UpgradeDatabases`) is started.
 *  5. This activity will display a spinning progress indicator and start
 * [DatabaseUpgradeService].
 *  6. [DatabaseUpgradeService] will acquire a partial wake lock (with a 10-minute timeout),
 * start a background thread to perform the database upgrades, and report the progress using
 * [LocalBroadcastManager] to this activity which will update the UI accordingly.
 *  7. Once the upgrade is complete [DatabaseUpgradeService] will notify this activity,
 * release the wake lock, and stop itself.
 *  8. This activity will start the original activity using the intent supplied when starting
 * this activity.
 *
 * Notes:
 * Currently we make no attempts to stop the background code (e.g. [com.fsck.k9.controller.MessagingController]) from
 * opening the accounts' databases. If this happens the upgrade is performed in one of the
 * background threads and not by [DatabaseUpgradeService]. But this is not a problem. Due to
 * the locking in [com.fsck.k9.mailstore.LocalStoreProvider.getInstance] the upgrade service will block
 * and from the outside (especially for this activity) it will appear as if
 * [DatabaseUpgradeService] is performing the upgrade.
 */
class UpgradeDatabases : BaseActivity() {
    private var mStartIntent: Intent? = null

    private var mUpgradeText: MaterialTextView? = null

    private var mLocalBroadcastManager: LocalBroadcastManager? = null
    private var mBroadcastReceiver: UpgradeDatabaseBroadcastReceiver? = null
    private var mIntentFilter: IntentFilter? = null
    private var mPreferences: Preferences? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        decodeExtras()

        // If the databases have already been upgraded, there's no point in displaying this activity.
        if (areDatabasesUpToDate()) {
            launchOriginalActivity()
            return
        }

        mPreferences = getPreferences()

        initializeLayout()

        setupBroadcastReceiver()
    }

    private fun initializeLayout() {
        setLayout(R.layout.upgrade_databases)
        setTitle(R.string.upgrade_databases_title)

        mUpgradeText = findViewById(R.id.databaseUpgradeText)
    }

    private fun decodeExtras() {
        val intent = getIntent()
        mStartIntent = IntentCompat.getParcelableExtra(intent, EXTRA_START_INTENT, Intent::class.java)
    }

    /**
     * Setup the broadcast receiver used to receive progress updates from [DatabaseUpgradeService].
     */
    private fun setupBroadcastReceiver() {
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this)
        mBroadcastReceiver = UpgradeDatabaseBroadcastReceiver()

        mIntentFilter = IntentFilter(DatabaseUpgradeService.ACTION_UPGRADE_PROGRESS)
        mIntentFilter!!.addAction(DatabaseUpgradeService.ACTION_UPGRADE_COMPLETE)
    }

    public override fun onResume() {
        super.onResume()

        // Check if the upgrade was completed while the activity was paused.
        if (areDatabasesUpToDate()) {
            launchOriginalActivity()
            return
        }

        // Register the broadcast receiver to listen for progress reports from
        // DatabaseUpgradeService.
        mLocalBroadcastManager!!.registerReceiver(mBroadcastReceiver!!, mIntentFilter!!)

        // Now that the broadcast receiver was registered start DatabaseUpgradeService.
        DatabaseUpgradeService.startService(this)
    }

    public override fun onPause() {
        // The activity is being paused, so there's no point in listening to the progress of the
        // database upgrade service.
        mLocalBroadcastManager!!.unregisterReceiver(mBroadcastReceiver!!)

        super.onPause()
    }

    /**
     * Finish this activity and launch the original activity using the supplied intent.
     */
    private fun launchOriginalActivity() {
        finish()
        startActivity(mStartIntent)
    }

    /**
     * Receiver for broadcasts send by [DatabaseUpgradeService].
     */
    internal inner class UpgradeDatabaseBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action

            if (DatabaseUpgradeService.ACTION_UPGRADE_PROGRESS == action) {
                // Information on the current upgrade progress
                val accountUuid = intent.getStringExtra(
                    DatabaseUpgradeService.EXTRA_ACCOUNT_UUID,
                )

                val account = mPreferences!!.getAccount(accountUuid!!)

                if (account != null) {
                    val upgradeStatus = getString(R.string.upgrade_database_format, account.displayName)
                    mUpgradeText!!.text = upgradeStatus
                }
            } else if (DatabaseUpgradeService.ACTION_UPGRADE_COMPLETE == action) {
                // Upgrade complete, launch the original activity.
                launchOriginalActivity()
            }
        }
    }

    companion object {
        const val ACTION_UPGRADE_DATABASES: String = "upgrade_databases"
        const val EXTRA_START_INTENT: String = "start_intent"
    }
}
