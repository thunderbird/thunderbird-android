package com.fsck.k9.service;


import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import app.k9mail.legacy.di.DI;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.mail.power.PowerManager;
import com.fsck.k9.mail.power.WakeLock;
import com.fsck.k9.mailstore.LocalStoreProvider;
import net.thunderbird.core.android.account.LegacyAccountDto;
import net.thunderbird.core.logging.legacy.Log;

/**
 * Service used to upgrade the accounts' databases and/or track the progress of the upgrade.
 *
 * <p>
 * See {@code UpgradeDatabases} for a detailed explanation of the database upgrade process.
 * </p>
 */
public class DatabaseUpgradeService extends Service {
    /**
     * Broadcast intent reporting the current progress of the database upgrade.
     *
     * <p>Extras:</p>
     * <ul>
     * <li>{@link #EXTRA_ACCOUNT_UUID}</li>
     * <li>{@link #EXTRA_PROGRESS}</li>
     * <li>{@link #EXTRA_PROGRESS_END}</li>
     * </ul>
     */
    public static final String ACTION_UPGRADE_PROGRESS = "DatabaseUpgradeService.upgradeProgress";

    /**
     * Broadcast intent sent when the upgrade has been completed.
     */
    public static final String ACTION_UPGRADE_COMPLETE = "DatabaseUpgradeService.upgradeComplete";

    /**
     * UUID of the account whose database is currently being upgraded.
     */
    public static final String EXTRA_ACCOUNT_UUID = "account_uuid";

    /**
     * The current progress.
     *
     * <p>Integer from {@code 0} (inclusive) to the value in {@link #EXTRA_PROGRESS_END}
     * (exclusive).</p>
     */
    public static final String EXTRA_PROGRESS = "progress";

    /**
     * Number of items that will be upgraded.
     *
     * <p>Currently this is the number of accounts.</p>
     */
    public static final String EXTRA_PROGRESS_END = "progress_end";


    /**
     * Action used to start this service.
     */
    private static final String ACTION_START_SERVICE =
            "com.fsck.k9.service.DatabaseUpgradeService.startService";

    private static final String WAKELOCK_TAG = "DatabaseUpgradeService";
    private static final long WAKELOCK_TIMEOUT = 10 * 60 * 1000;    // 10 minutes


    /**
     * Start {@link DatabaseUpgradeService}.
     *
     * @param context
     *         The {@link Context} used to start this service.
     */
    public static void startService(Context context) {
        Intent i = new Intent();
        i.setClass(context, DatabaseUpgradeService.class);
        i.setAction(DatabaseUpgradeService.ACTION_START_SERVICE);
        context.startService(i);
    }


    /**
     * Stores whether or not this service was already running when
     * {@link #onStartCommand(Intent, int, int)} is executed.
     */
    private AtomicBoolean mRunning = new AtomicBoolean(false);

    private LocalBroadcastManager mLocalBroadcastManager;

    private String mAccountUuid;
    private int mProgress;
    private int mProgressEnd;

    private WakeLock mWakeLock;


    @Override
    public IBinder onBind(Intent intent) {
        // unused
        return null;
    }

    @Override
    public void onCreate() {
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
    }

    @Override
    public final int onStartCommand(Intent intent, int flags, int startId) {
        boolean success = mRunning.compareAndSet(false, true);
        if (success) {
            // The service wasn't running yet.
            Log.i("DatabaseUpgradeService started");

            acquireWakelock();

            startUpgradeInBackground();
        } else {
            // We're already running, so don't start the upgrade process again. But send the current
            // progress via broadcast.
            sendProgressBroadcast(mAccountUuid, mProgress, mProgressEnd);
        }

        return START_STICKY;
    }

    /**
     * Acquire a partial wake lock so the CPU won't go to sleep when the screen is turned off.
     */
    private void acquireWakelock() {
        PowerManager pm = DI.get(PowerManager.class);
        mWakeLock = pm.newWakeLock(WAKELOCK_TAG);
        mWakeLock.setReferenceCounted(false);
        mWakeLock.acquire(WAKELOCK_TIMEOUT);
    }

    /**
     * Release the wake lock.
     */
    private void releaseWakelock() {
        mWakeLock.release();
    }

    /**
     * Stop this service.
     */
    private void stopService() {
        stopSelf();
        Log.i("DatabaseUpgradeService stopped");

        releaseWakelock();
        mRunning.set(false);
    }

    /**
     * Start a background thread for upgrading the databases.
     */
    private void startUpgradeInBackground() {
        new Thread("DatabaseUpgradeService") {
            @Override
            public void run() {
                upgradeDatabases();
                stopService();
            }
        }.start();
    }

    /**
     * Upgrade the accounts' databases.
     */
    private void upgradeDatabases() {
        Preferences preferences = Preferences.getPreferences();

        List<LegacyAccountDto> accounts = preferences.getAccounts();
        mProgressEnd = accounts.size();
        mProgress = 0;

        for (LegacyAccountDto account : accounts) {
            mAccountUuid = account.getUuid();

            sendProgressBroadcast(mAccountUuid, mProgress, mProgressEnd);

            try {
                // Account.getLocalStore() is blocking and will upgrade the database if necessary
                DI.get(LocalStoreProvider.class).getInstance(account);
            } catch (Exception e) {
                Log.e(e, "Error while upgrading database");
            }

            mProgress++;
        }

        K9.setDatabasesUpToDate(true);
        sendUpgradeCompleteBroadcast();
    }

    private void sendProgressBroadcast(String accountUuid, int progress, int progressEnd) {
        Intent intent = new Intent();
        intent.setAction(ACTION_UPGRADE_PROGRESS);
        intent.putExtra(EXTRA_ACCOUNT_UUID, accountUuid);
        intent.putExtra(EXTRA_PROGRESS, progress);
        intent.putExtra(EXTRA_PROGRESS_END, progressEnd);

        mLocalBroadcastManager.sendBroadcast(intent);
    }

    private void sendUpgradeCompleteBroadcast() {
        Intent intent = new Intent();
        intent.setAction(ACTION_UPGRADE_COMPLETE);

        mLocalBroadcastManager.sendBroadcast(intent);
    }
}
