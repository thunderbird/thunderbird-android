package com.fsck.k9.activity;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.core.content.IntentCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import net.thunderbird.core.android.account.LegacyAccount;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.service.DatabaseUpgradeService;
import com.fsck.k9.ui.R;
import com.fsck.k9.ui.base.K9Activity;
import com.google.android.material.textview.MaterialTextView;


/**
 * This activity triggers a database upgrade if necessary and displays the current upgrade progress.
 *
 * <p>
 * The current upgrade process works as follows:
 * <ol>
 * <li>Activities that access an account's database call
 *     {@link #actionUpgradeDatabases(Context, Intent)} in their {@link Activity#onCreate(Bundle)}
 *     method.</li>
 * <li>{@link #actionUpgradeDatabases(Context, Intent)} will call {@link K9#areDatabasesUpToDate()}
 *     to check if we already know whether the databases have been upgraded.</li>
 * <li>{@link K9#areDatabasesUpToDate()} will compare the last known database version stored in a
 *     {@link SharedPreferences} file to {@link LocalStore#getDbVersion()}. This
 *     is done as an optimization because it's faster than opening all of the accounts' databases
 *     one by one.</li>
 * <li>If there was an error reading the cached database version or if it shows the databases need
 *     upgrading this activity ({@code UpgradeDatabases}) is started.</li>
 * <li>This activity will display a spinning progress indicator and start
 *     {@link DatabaseUpgradeService}.</li>
 * <li>{@link DatabaseUpgradeService} will acquire a partial wake lock (with a 10 minute timeout),
 *     start a background thread to perform the database upgrades, and report the progress using
 *     {@link LocalBroadcastManager} to this activity which will update the UI accordingly.</li>
 * <li>Once the upgrade is complete {@link DatabaseUpgradeService} will notify this activity,
 *     release the wake lock, and stop itself.</li>
 * <li>This activity will start the original activity using the intent supplied when calling
 *     {@link #actionUpgradeDatabases(Context, Intent)}.</li>
 * </ol>
 * </p><p>
 * Currently we make no attempts to stop the background code (e.g. {@link MessagingController}) from
 * opening the accounts' databases. If this happens the upgrade is performed in one of the
 * background threads and not by {@link DatabaseUpgradeService}. But this is not a problem. Due to
 * the locking in {@link com.fsck.k9.mailstore.LocalStoreProvider#getInstance(LegacyAccount)} the upgrade service will block
 * and from the outside (especially for this activity) it will appear as if
 * {@link DatabaseUpgradeService} is performing the upgrade.
 * </p>
 */
public class UpgradeDatabases extends K9Activity {
    private static final String ACTION_UPGRADE_DATABASES = "upgrade_databases";
    private static final String EXTRA_START_INTENT = "start_intent";


    /**
     * Start the {@link UpgradeDatabases} activity if necessary.
     *
     * @param context
     *         The {@link Context} used to start the activity.
     * @param startIntent
     *         After the database upgrade is complete an activity is started using this intent.
     *         Usually this is the intent that was used to start the calling activity.
     *         Never {@code null}.
     *
     * @return {@code true}, if the {@code UpgradeDatabases} activity was started. In this case the
     *         calling activity is expected to finish itself.<br>
     *         {@code false}, if the account databases don't need upgrading.
     */
    public static boolean actionUpgradeDatabases(Context context, Intent startIntent) {
        if (K9.areDatabasesUpToDate()) {
            return false;
        }

        Intent intent = new Intent(context, UpgradeDatabases.class);
        intent.setAction(ACTION_UPGRADE_DATABASES);
        intent.putExtra(EXTRA_START_INTENT, startIntent);

        // Make sure this activity is only running once
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        context.startActivity(intent);
        return true;
    }


    private Intent mStartIntent;

    private MaterialTextView mUpgradeText;

    private LocalBroadcastManager mLocalBroadcastManager;
    private UpgradeDatabaseBroadcastReceiver mBroadcastReceiver;
    private IntentFilter mIntentFilter;
    private Preferences mPreferences;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        decodeExtras();

        // If the databases have already been upgraded there's no point in displaying this activity.
        if (K9.areDatabasesUpToDate()) {
            launchOriginalActivity();
            return;
        }

        mPreferences = Preferences.getPreferences();

        initializeLayout();

        setupBroadcastReceiver();
    }

    /**
     * Initialize the activity's layout
     */
    private void initializeLayout() {
        setLayout(R.layout.upgrade_databases);
        setTitle(R.string.upgrade_databases_title);

        mUpgradeText = findViewById(R.id.databaseUpgradeText);
    }

    /**
     * Decode extras in the intent used to start this activity.
     */
    private void decodeExtras() {
        Intent intent = getIntent();
        mStartIntent = IntentCompat.getParcelableExtra(intent, EXTRA_START_INTENT, Intent.class);
    }

    /**
     * Setup the broadcast receiver used to receive progress updates from
     * {@link DatabaseUpgradeService}.
     */
    private void setupBroadcastReceiver() {
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mBroadcastReceiver = new UpgradeDatabaseBroadcastReceiver();

        mIntentFilter = new IntentFilter(DatabaseUpgradeService.ACTION_UPGRADE_PROGRESS);
        mIntentFilter.addAction(DatabaseUpgradeService.ACTION_UPGRADE_COMPLETE);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Check if the upgrade was completed while the activity was paused.
        if (K9.areDatabasesUpToDate()) {
            launchOriginalActivity();
            return;
        }

        // Register the broadcast receiver to listen for progress reports from
        // DatabaseUpgradeService.
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, mIntentFilter);

        // Now that the broadcast receiver was registered start DatabaseUpgradeService.
        DatabaseUpgradeService.startService(this);
    }

    @Override
    public void onPause() {
        // The activity is being paused, so there's no point in listening to the progress of the
        // database upgrade service.
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);

        super.onPause();
    }

    /**
     * Finish this activity and launch the original activity using the supplied intent.
     */
    private void launchOriginalActivity() {
        finish();
        startActivity(mStartIntent);
    }

    /**
     * Receiver for broadcasts send by {@link DatabaseUpgradeService}.
     */
    class UpgradeDatabaseBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, final Intent intent) {
            String action = intent.getAction();

            if (DatabaseUpgradeService.ACTION_UPGRADE_PROGRESS.equals(action)) {
                /*
                 * Information on the current upgrade progress
                 */

                String accountUuid = intent.getStringExtra(
                        DatabaseUpgradeService.EXTRA_ACCOUNT_UUID);

                LegacyAccount account = mPreferences.getAccount(accountUuid);

                if (account != null) {
                    String upgradeStatus = getString(R.string.upgrade_database_format, account.getDisplayName());
                    mUpgradeText.setText(upgradeStatus);
                }

            } else if (DatabaseUpgradeService.ACTION_UPGRADE_COMPLETE.equals(action)) {
                /*
                 * Upgrade complete
                 */

                launchOriginalActivity();
            }
        }
    }
}
