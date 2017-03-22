
package com.fsck.k9.service;


import java.util.Collection;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;

import com.fsck.k9.Account;
import com.fsck.k9.Account.FolderMode;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Pusher;
import com.fsck.k9.preferences.Storage;
import com.fsck.k9.preferences.StorageEditor;
import timber.log.Timber;


public class MailService extends CoreService {
    private static final String ACTION_CHECK_MAIL = "com.fsck.k9.intent.action.MAIL_SERVICE_WAKEUP";
    private static final String ACTION_RESET = "com.fsck.k9.intent.action.MAIL_SERVICE_RESET";
    private static final String ACTION_RESCHEDULE_POLL = "com.fsck.k9.intent.action.MAIL_SERVICE_RESCHEDULE_POLL";
    private static final String ACTION_CANCEL = "com.fsck.k9.intent.action.MAIL_SERVICE_CANCEL";
    private static final String ACTION_REFRESH_PUSHERS = "com.fsck.k9.intent.action.MAIL_SERVICE_REFRESH_PUSHERS";
    private static final String ACTION_RESTART_PUSHERS = "com.fsck.k9.intent.action.MAIL_SERVICE_RESTART_PUSHERS";
    private static final String CONNECTIVITY_CHANGE = "com.fsck.k9.intent.action.MAIL_SERVICE_CONNECTIVITY_CHANGE";
    private static final String CANCEL_CONNECTIVITY_NOTICE = "com.fsck.k9.intent.action.MAIL_SERVICE_CANCEL_CONNECTIVITY_NOTICE";

    private static long nextCheck = -1;
    private static boolean pushingRequested = false;
    private static boolean pollingRequested = false;
    private static boolean syncNoBackground = false;
    private static boolean syncNoConnectivity = false;
    private static boolean syncBlocked = false;

    public static void actionReset(Context context, Integer wakeLockId) {
        Intent i = new Intent();
        i.setClass(context, MailService.class);
        i.setAction(MailService.ACTION_RESET);
        addWakeLockId(context, i, wakeLockId, true);
        context.startService(i);
    }

    public static void actionRestartPushers(Context context, Integer wakeLockId) {
        Intent i = new Intent();
        i.setClass(context, MailService.class);
        i.setAction(MailService.ACTION_RESTART_PUSHERS);
        addWakeLockId(context, i, wakeLockId, true);
        context.startService(i);
    }

    public static void actionReschedulePoll(Context context, Integer wakeLockId) {
        Intent i = new Intent();
        i.setClass(context, MailService.class);
        i.setAction(MailService.ACTION_RESCHEDULE_POLL);
        addWakeLockId(context, i, wakeLockId, true);
        context.startService(i);
    }

    public static void actionCancel(Context context, Integer wakeLockId) {
        Intent i = new Intent();
        i.setClass(context, MailService.class);
        i.setAction(MailService.ACTION_CANCEL);
        addWakeLockId(context, i, wakeLockId, false); // CK:Q: why should we not create a wake lock if one is not already existing like for example in actionReschedulePoll?
        context.startService(i);
    }

    public static void connectivityChange(Context context, Integer wakeLockId) {
        Intent i = new Intent();
        i.setClass(context, MailService.class);
        i.setAction(MailService.CONNECTIVITY_CHANGE);
        addWakeLockId(context, i, wakeLockId, false); // CK:Q: why should we not create a wake lock if one is not already existing like for example in actionReschedulePoll?
        context.startService(i);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.v("***** MailService *****: onCreate");
    }

    @Override
    public int startService(Intent intent, int startId) {
        long startTime = SystemClock.elapsedRealtime();
        boolean oldIsSyncDisabled = isSyncDisabled();
        boolean doBackground = true;

        final boolean hasConnectivity = Utility.hasConnectivity(getApplication());
        boolean autoSync = ContentResolver.getMasterSyncAutomatically();

        K9.BACKGROUND_OPS bOps = K9.getBackgroundOps();

        switch (bOps) {
            case NEVER:
                doBackground = false;
                break;
            case ALWAYS:
                doBackground = true;
                break;
            case WHEN_CHECKED_AUTO_SYNC:
                doBackground = autoSync;
                break;
        }

        syncNoBackground = !doBackground;
        syncNoConnectivity = !hasConnectivity;
        syncBlocked = !(doBackground && hasConnectivity);

        Timber.i("MailService.onStart(%s, %d), hasConnectivity = %s, doBackground = %s",
                intent, startId, hasConnectivity, doBackground);

        // MessagingController.getInstance(getApplication()).addListener(mListener);
        if (ACTION_CHECK_MAIL.equals(intent.getAction())) {
            Timber.i("***** MailService *****: checking mail");
            if (hasConnectivity && doBackground) {
                PollService.startService(this);
            }
            reschedulePollInBackground(hasConnectivity, doBackground, startId, false);
        } else if (ACTION_CANCEL.equals(intent.getAction())) {
            Timber.v("***** MailService *****: cancel");
            cancel();
        } else if (ACTION_RESET.equals(intent.getAction())) {
            Timber.v("***** MailService *****: reschedule");
            rescheduleAllInBackground(hasConnectivity, doBackground, startId);
        } else if (ACTION_RESTART_PUSHERS.equals(intent.getAction())) {
            Timber.v("***** MailService *****: restarting pushers");
            reschedulePushersInBackground(hasConnectivity, doBackground, startId);
        } else if (ACTION_RESCHEDULE_POLL.equals(intent.getAction())) {
            Timber.v("***** MailService *****: rescheduling poll");
            reschedulePollInBackground(hasConnectivity, doBackground, startId, true);
        } else if (ACTION_REFRESH_PUSHERS.equals(intent.getAction())) {
            refreshPushersInBackground(hasConnectivity, doBackground, startId);
        } else if (CONNECTIVITY_CHANGE.equals(intent.getAction())) {
            rescheduleAllInBackground(hasConnectivity, doBackground, startId);
            Timber.i("Got connectivity action with hasConnectivity = %s, doBackground = %s",
                    hasConnectivity, doBackground);
        } else if (CANCEL_CONNECTIVITY_NOTICE.equals(intent.getAction())) {
            /* do nothing */
        }

        if (isSyncDisabled() != oldIsSyncDisabled) {
            MessagingController.getInstance(getApplication()).systemStatusChanged();
        }

        Timber.i("MailService.onStart took %d ms", SystemClock.elapsedRealtime() - startTime);

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Timber.v("***** MailService *****: onDestroy()");
        super.onDestroy();
        //     MessagingController.getInstance(getApplication()).removeListener(mListener);
    }

    private void cancel() {
        Intent i = new Intent(this, MailService.class);
        i.setAction(ACTION_CHECK_MAIL);
        BootReceiver.cancelIntent(this, i);
    }

    private final static String PREVIOUS_INTERVAL = "MailService.previousInterval";
    private final static String LAST_CHECK_END = "MailService.lastCheckEnd";

    public static void saveLastCheckEnd(Context context) {
        long lastCheckEnd = System.currentTimeMillis();
        Timber.i("Saving lastCheckEnd = %tc", lastCheckEnd);

        Preferences prefs = Preferences.getPreferences(context);
        Storage storage = prefs.getStorage();
        StorageEditor editor = storage.edit();
        editor.putLong(LAST_CHECK_END, lastCheckEnd);
        editor.commit();
    }

    private void rescheduleAllInBackground(final boolean hasConnectivity,
            final boolean doBackground, Integer startId) {

        execute(getApplication(), new Runnable() {
            @Override
            public void run() {
                reschedulePoll(hasConnectivity, doBackground, true);
                reschedulePushers(hasConnectivity, doBackground);
            }
        }, K9.MAIL_SERVICE_WAKE_LOCK_TIMEOUT, startId);
    }

    private void reschedulePollInBackground(final boolean hasConnectivity,
            final boolean doBackground, Integer startId, final boolean considerLastCheckEnd) {

        execute(getApplication(), new Runnable() {
            public void run() {
                reschedulePoll(hasConnectivity, doBackground, considerLastCheckEnd);
            }
        }, K9.MAIL_SERVICE_WAKE_LOCK_TIMEOUT, startId);
    }

    private void reschedulePushersInBackground(final boolean hasConnectivity,
            final boolean doBackground, Integer startId) {

        execute(getApplication(), new Runnable() {
            public void run() {
                reschedulePushers(hasConnectivity, doBackground);
            }
        }, K9.MAIL_SERVICE_WAKE_LOCK_TIMEOUT, startId);
    }

    private void refreshPushersInBackground(boolean hasConnectivity, boolean doBackground,
            Integer startId) {

        if (hasConnectivity && doBackground) {
            execute(getApplication(), new Runnable() {
                public void run() {
                    refreshPushers();
                    schedulePushers();
                }
            }, K9.MAIL_SERVICE_WAKE_LOCK_TIMEOUT, startId);
        }
    }

    private void reschedulePoll(final boolean hasConnectivity, final boolean doBackground,
            boolean considerLastCheckEnd) {

        if (!(hasConnectivity && doBackground)) {
            Timber.i("No connectivity, canceling check for %s", getApplication().getPackageName());

            nextCheck = -1;
            cancel();

            return;
        }

        Preferences prefs = Preferences.getPreferences(MailService.this);
        Storage storage = prefs.getStorage();
        int previousInterval = storage.getInt(PREVIOUS_INTERVAL, -1);
        long lastCheckEnd = storage.getLong(LAST_CHECK_END, -1);

        long now = System.currentTimeMillis();
        if (lastCheckEnd > now) {
            Timber.i("The database claims that the last time mail was checked was in the future (%tc). To try to get " +
                    "things back to normal, the last check time has been reset to: %tc", lastCheckEnd, now);

            lastCheckEnd = now;
        }

        int shortestInterval = -1;
        for (Account account : prefs.getAvailableAccounts()) {
            if (account.getAutomaticCheckIntervalMinutes() != -1 &&
                    account.getFolderSyncMode() != FolderMode.NONE &&
                    (account.getAutomaticCheckIntervalMinutes() < shortestInterval ||
                            shortestInterval == -1)) {
                shortestInterval = account.getAutomaticCheckIntervalMinutes();
            }
        }
        StorageEditor editor = storage.edit();
        editor.putInt(PREVIOUS_INTERVAL, shortestInterval);
        editor.commit();

        if (shortestInterval == -1) {
            Timber.i("No next check scheduled for package %s", getApplication().getPackageName());

            nextCheck = -1;
            pollingRequested = false;
            cancel();
        } else {
            long delay = (shortestInterval * (60 * 1000));
            long base = (previousInterval == -1 || lastCheckEnd == -1 ||
                    !considerLastCheckEnd ? System.currentTimeMillis() : lastCheckEnd);
            long nextTime = base + delay;

            Timber.i("previousInterval = %d, shortestInterval = %d, lastCheckEnd = %tc, considerLastCheckEnd = %b",
                    previousInterval,
                    shortestInterval,
                    lastCheckEnd,
                    considerLastCheckEnd);

            nextCheck = nextTime;
            pollingRequested = true;

            try {
                Timber.i("Next check for package %s scheduled for %tc", getApplication().getPackageName(), nextTime);
            } catch (Exception e) {
                // I once got a NullPointerException deep in new Date();
                Timber.e(e, "Exception while logging");
            }

            Intent i = new Intent(this, MailService.class);
            i.setAction(ACTION_CHECK_MAIL);
            BootReceiver.scheduleIntent(MailService.this, nextTime, i);
        }
    }

    public static boolean isSyncDisabled() {
        return  syncBlocked || (!pollingRequested && !pushingRequested);
    }

    public static boolean hasNoConnectivity() {
        return syncNoConnectivity;
    }

    public static boolean isSyncNoBackground() {
        return syncNoBackground;
    }

    public static boolean isSyncBlocked() {
        return syncBlocked;
    }

    public static boolean isPollAndPushDisabled() {
        return (!pollingRequested && !pushingRequested);
    }

    private void stopPushers() {
        MessagingController.getInstance(getApplication()).stopAllPushing();
        PushService.stopService(MailService.this);
    }

    private void reschedulePushers(boolean hasConnectivity, boolean doBackground) {
        Timber.i("Rescheduling pushers");

        stopPushers();

        if (!(hasConnectivity && doBackground)) {
            Timber.i("Not scheduling pushers:  connectivity? %s -- doBackground? %s", hasConnectivity, doBackground);
            return;
        }

        setupPushers();
        schedulePushers();
    }


    private void setupPushers() {
        boolean pushing = false;
        for (Account account : Preferences.getPreferences(MailService.this).getAccounts()) {
            Timber.i("Setting up pushers for account %s", account.getDescription());

            if (account.isEnabled() && account.isAvailable(getApplicationContext())) {
                pushing |= MessagingController.getInstance(getApplication()).setupPushing(account);
            } else {
                //TODO: setupPushing of unavailable accounts when they become available (sd-card inserted)
            }
        }
        if (pushing) {
            PushService.startService(MailService.this);
        }
        pushingRequested = pushing;
    }

    private void refreshPushers() {
        try {
            long nowTime = System.currentTimeMillis();
            Timber.i("Refreshing pushers");

            Collection<Pusher> pushers = MessagingController.getInstance(getApplication()).getPushers();
            for (Pusher pusher : pushers) {
                long lastRefresh = pusher.getLastRefresh();
                int refreshInterval = pusher.getRefreshInterval();
                long sinceLast = nowTime - lastRefresh;
                if (sinceLast + 10000 > refreshInterval) { // Add 10 seconds to keep pushers in sync, avoid drift
                    Timber.d("PUSHREFRESH: refreshing lastRefresh = %d, interval = %d, nowTime = %d, " +
                            "sinceLast = %d",
                            lastRefresh,
                            refreshInterval,
                            nowTime,
                            sinceLast);

                    pusher.refresh();
                    pusher.setLastRefresh(nowTime);
                } else {
                    Timber.d("PUSHREFRESH: NOT refreshing lastRefresh = %d, interval = %d, nowTime = %d, " +
                            "sinceLast = %d",
                            lastRefresh,
                            refreshInterval,
                            nowTime,
                            sinceLast);
                }
            }
            // Whenever we refresh our pushers, send any unsent messages
            Timber.d("PUSHREFRESH:  trying to send mail in all folders!");

            MessagingController.getInstance(getApplication()).sendPendingMessages(null);

        } catch (Exception e) {
            Timber.e(e, "Exception while refreshing pushers");
        }
    }

    private void schedulePushers() {
        int minInterval = -1;

        Collection<Pusher> pushers = MessagingController.getInstance(getApplication()).getPushers();
        for (Pusher pusher : pushers) {
            int interval = pusher.getRefreshInterval();
            if (interval > 0 && (interval < minInterval || minInterval == -1)) {
                minInterval = interval;
            }
        }

        Timber.v("Pusher refresh interval = %d", minInterval);

        if (minInterval > 0) {
            long nextTime = System.currentTimeMillis() + minInterval;
            Timber.d("Next pusher refresh scheduled for %tc", nextTime);

            Intent i = new Intent(this, MailService.class);
            i.setAction(ACTION_REFRESH_PUSHERS);
            BootReceiver.scheduleIntent(MailService.this, nextTime, i);
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        // Unused
        return null;
    }

    public static long getNextPollTime() {
        return nextCheck;
    }
}
