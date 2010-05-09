
package com.fsck.k9.service;

import java.util.Collection;
import java.util.Date;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.IBinder;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.MessagingController;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.Account.FolderMode;
import com.fsck.k9.mail.Pusher;

/**
 */
public class MailService extends CoreService
{
    private static final String ACTION_CHECK_MAIL = "com.fsck.k9.intent.action.MAIL_SERVICE_WAKEUP";
    private static final String ACTION_RESET = "com.fsck.k9.intent.action.MAIL_SERVICE_RESET";
    private static final String ACTION_RESCHEDULE_POLL = "com.fsck.k9.intent.action.MAIL_SERVICE_RESCHEDULE_POLL";
    private static final String ACTION_CANCEL = "com.fsck.k9.intent.action.MAIL_SERVICE_CANCEL";
    private static final String ACTION_REFRESH_PUSHERS = "com.fsck.k9.intent.action.MAIL_SERVICE_REFRESH_PUSHERS";
    private static final String ACTION_RESTART_PUSHERS = "com.fsck.k9.intent.action.MAIL_SERVICE_RESTART_PUSHERS";
    private static final String CONNECTIVITY_CHANGE = "com.fsck.k9.intent.action.MAIL_SERVICE_CONNECTIVITY_CHANGE";
    private static final String CANCEL_CONNECTIVITY_NOTICE = "com.fsck.k9.intent.action.MAIL_SERVICE_CANCEL_CONNECTIVITY_NOTICE";

    private static final String HAS_CONNECTIVITY = "com.fsck.k9.intent.action.MAIL_SERVICE_HAS_CONNECTIVITY";

    private static long nextCheck = -1;

    public static void actionReset(Context context, Integer wakeLockId)
    {
        Intent i = new Intent();
        i.setClass(context, MailService.class);
        i.setAction(MailService.ACTION_RESET);
        addWakeLockId(i, wakeLockId);
        if (wakeLockId == null)
        {
            addWakeLock(context, i);
        }
        context.startService(i);
    }

    public static void actionRestartPushers(Context context, Integer wakeLockId)
    {
        Intent i = new Intent();
        i.setClass(context, MailService.class);
        i.setAction(MailService.ACTION_RESTART_PUSHERS);
        addWakeLockId(i, wakeLockId);
        if (wakeLockId == null)
        {
            addWakeLock(context, i);
        }
        context.startService(i);
    }

    public static void actionReschedulePoll(Context context, Integer wakeLockId)
    {
        Intent i = new Intent();
        i.setClass(context, MailService.class);
        i.setAction(MailService.ACTION_RESCHEDULE_POLL);
        addWakeLockId(i, wakeLockId);
        if (wakeLockId == null)
        {
            addWakeLock(context, i);
        }
        context.startService(i);
    }

    public static void actionCancel(Context context, Integer wakeLockId)
    {
        Intent i = new Intent();
        i.setClass(context, MailService.class);
        i.setAction(MailService.ACTION_CANCEL);
        addWakeLockId(i, wakeLockId);
        context.startService(i);
    }

    public static void connectivityChange(Context context, boolean hasConnectivity, Integer wakeLockId)
    {
        Intent i = new Intent();
        i.setClass(context, MailService.class);
        i.setAction(MailService.CONNECTIVITY_CHANGE);
        i.putExtra(HAS_CONNECTIVITY, hasConnectivity);
        addWakeLockId(i, wakeLockId);
        context.startService(i);
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        if (K9.DEBUG)
            Log.v(K9.LOG_TAG, "***** MailService *****: onCreate");
    }

    @Override
    public void startService(Intent intent, int startId)
    {
        Integer startIdObj = startId;
        long startTime = System.currentTimeMillis();
        try
        {
            ConnectivityManager connectivityManager = (ConnectivityManager)getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);
            boolean doBackground = true;
            boolean hasConnectivity = false;

            if (connectivityManager != null)
            {
                NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
                if (netInfo != null)
                {
                    State state = netInfo.getState();
                    hasConnectivity = state == State.CONNECTED;
                }
                boolean backgroundData = connectivityManager.getBackgroundDataSetting();

                K9.BACKGROUND_OPS bOps = K9.getBackgroundOps();
                doBackground = (backgroundData == true && bOps != K9.BACKGROUND_OPS.NEVER)
                               | (backgroundData == false && bOps == K9.BACKGROUND_OPS.ALWAYS);

            }

            if (K9.DEBUG)
                Log.i(K9.LOG_TAG, "MailService.onStart(" + intent + ", " + startId
                      + "), hasConnectivity = " + hasConnectivity + ", doBackground = " + doBackground);

            // MessagingController.getInstance(getApplication()).addListener(mListener);
            if (ACTION_CHECK_MAIL.equals(intent.getAction()))
            {
                if (K9.DEBUG)
                    Log.i(K9.LOG_TAG, "***** MailService *****: checking mail");

                if (hasConnectivity && doBackground)
                {
                    PollService.startService(this);
                }
                reschedulePoll(hasConnectivity, doBackground, startIdObj, false);
                startIdObj = null;
            }
            else if (ACTION_CANCEL.equals(intent.getAction()))
            {
                if (K9.DEBUG)
                    Log.v(K9.LOG_TAG, "***** MailService *****: cancel");

                cancel();
            }
            else if (ACTION_RESET.equals(intent.getAction()))
            {
                if (K9.DEBUG)
                    Log.v(K9.LOG_TAG, "***** MailService *****: reschedule");

                rescheduleAll(hasConnectivity, doBackground, startIdObj);
                startIdObj = null;

            }
            else if (ACTION_RESTART_PUSHERS.equals(intent.getAction()))
            {
                if (K9.DEBUG)
                    Log.v(K9.LOG_TAG, "***** MailService *****: restarting pushers");
                reschedulePushers(hasConnectivity, doBackground, startIdObj);
                startIdObj = null;

            }
            else if (ACTION_RESCHEDULE_POLL.equals(intent.getAction()))
            {
                if (K9.DEBUG)
                    Log.v(K9.LOG_TAG, "***** MailService *****: rescheduling poll");
                reschedulePoll(hasConnectivity, doBackground, startIdObj, true);
                startIdObj = null;

            }
            else if (ACTION_REFRESH_PUSHERS.equals(intent.getAction()))
            {
                if (hasConnectivity && doBackground)
                {
                    refreshPushers(null);
                    schedulePushers(startIdObj);
                    startIdObj = null;
                }
            }
            else if (CONNECTIVITY_CHANGE.equals(intent.getAction()))
            {
                notifyConnectionStatus(hasConnectivity);
                rescheduleAll(hasConnectivity, doBackground, startIdObj);
                startIdObj = null;
                if (K9.DEBUG)
                    Log.i(K9.LOG_TAG, "Got connectivity action with hasConnectivity = " + hasConnectivity + ", doBackground = " + doBackground);
            }
            else if (CANCEL_CONNECTIVITY_NOTICE.equals(intent.getAction()))
            {
                notifyConnectionStatus(true);
            }
        }
        finally
        {
            if (startIdObj != null)
            {
                stopSelf(startId);
            }
        }
        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "MailService.onStart took " + (System.currentTimeMillis() - startTime) + "ms");
    }

    private void rescheduleAll(final boolean hasConnectivity, final boolean doBackground, final Integer startId)
    {
        reschedulePoll(hasConnectivity, doBackground, null, true);
        reschedulePushers(hasConnectivity, doBackground, startId);

    }

    private void notifyConnectionStatus(boolean hasConnectivity)
    {
        if (true) return;
        NotificationManager notifMgr =
            (NotificationManager)getApplication().getSystemService(Context.NOTIFICATION_SERVICE);
        if (hasConnectivity == false)
        {
            String notice = getApplication().getString(R.string.no_connection_alert);
            String header = getApplication().getString(R.string.alert_header);


            Notification notif = new Notification(R.drawable.stat_notify_email_generic,
                                                  header, System.currentTimeMillis());

            Intent i = new Intent();
            i.setClassName(getApplication().getPackageName(), "com.fsck.k9.service.MailService");
            i.setAction(MailService.CANCEL_CONNECTIVITY_NOTICE);

            PendingIntent pi = PendingIntent.getService(this, 0, i, 0);

            notif.setLatestEventInfo(getApplication(), header, notice, pi);
            notif.flags = Notification.FLAG_ONGOING_EVENT;

            notifMgr.notify(K9.CONNECTIVITY_ID, notif);
        }
        else
        {
            notifMgr.cancel(K9.CONNECTIVITY_ID);
        }
    }

    @Override
    public void onDestroy()
    {
        if (K9.DEBUG)
            Log.v(K9.LOG_TAG, "***** MailService *****: onDestroy()");
        super.onDestroy();
        //     MessagingController.getInstance(getApplication()).removeListener(mListener);
    }

    private void cancel()
    {
        Intent i = new Intent();
        i.setClassName(getApplication().getPackageName(), "com.fsck.k9.service.MailService");
        i.setAction(ACTION_CHECK_MAIL);
        BootReceiver.cancelIntent(this, i);
    }

    private final static String PREVIOUS_INTERVAL = "MailService.previousInterval";
    private final static String LAST_CHECK_END = "MailService.lastCheckEnd";

    public static void saveLastCheckEnd(Context context)
    {

        long lastCheckEnd = System.currentTimeMillis();
        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "Saving lastCheckEnd = " + new Date(lastCheckEnd));
        Preferences prefs = Preferences.getPreferences(context);
        SharedPreferences sPrefs = prefs.getPreferences();
        SharedPreferences.Editor editor = sPrefs.edit();
        editor.putLong(LAST_CHECK_END, lastCheckEnd);
        editor.commit();
    }

    private void reschedulePoll(final boolean hasConnectivity, final boolean doBackground, Integer startId, final boolean considerLastCheckEnd)
    {
        if (hasConnectivity && doBackground)
        {
            execute(getApplication(), new Runnable()
            {
                public void run()
                {
                    int shortestInterval = -1;

                    Preferences prefs = Preferences.getPreferences(MailService.this);
                    SharedPreferences sPrefs = prefs.getPreferences();
                    int previousInterval = sPrefs.getInt(PREVIOUS_INTERVAL, -1);
                    long lastCheckEnd = sPrefs.getLong(LAST_CHECK_END, -1);
                    for (Account account : prefs.getAccounts())
                    {
                        if (account.getAutomaticCheckIntervalMinutes() != -1
                                && account.getFolderSyncMode() != FolderMode.NONE
                                && (account.getAutomaticCheckIntervalMinutes() < shortestInterval || shortestInterval == -1))
                        {
                            shortestInterval = account.getAutomaticCheckIntervalMinutes();
                        }
                    }
                    SharedPreferences.Editor editor = sPrefs.edit();
                    editor.putInt(PREVIOUS_INTERVAL, shortestInterval);
                    editor.commit();

                    if (shortestInterval == -1)
                    {
                        nextCheck = -1;
                        if (K9.DEBUG)
                            Log.i(K9.LOG_TAG, "No next check scheduled for package " + getApplication().getPackageName());
                        cancel();
                    }
                    else
                    {
                        long delay = (shortestInterval * (60 * 1000));
                        long base = (previousInterval == -1 || lastCheckEnd == -1 || considerLastCheckEnd == false ? System.currentTimeMillis() : lastCheckEnd);
                        long nextTime = base + delay;
                        if (K9.DEBUG)
                            Log.i(K9.LOG_TAG,
                                  "previousInterval = " + previousInterval
                                  + ", shortestInterval = " + shortestInterval
                                  + ", lastCheckEnd = " + new Date(lastCheckEnd)
                                  + ", considerLastCheckEnd = " + considerLastCheckEnd);
                        nextCheck = nextTime;
                        try
                        {
                            if (K9.DEBUG)
                                Log.i(K9.LOG_TAG, "Next check for package " + getApplication().getPackageName() + " scheduled for " + new Date(nextTime));
                        }
                        catch (Exception e)
                        {
                            // I once got a NullPointerException deep in new Date();
                            Log.e(K9.LOG_TAG, "Exception while logging", e);
                        }

                        Intent i = new Intent();
                        i.setClassName(getApplication().getPackageName(), "com.fsck.k9.service.MailService");
                        i.setAction(ACTION_CHECK_MAIL);
                        BootReceiver.scheduleIntent(MailService.this, nextTime, i);

                    }
                }
            }
            , K9.MAIL_SERVICE_WAKE_LOCK_TIMEOUT, startId);
        }
        else
        {
            nextCheck = -1;
            if (K9.DEBUG)
                Log.i(K9.LOG_TAG, "No connectivity, canceling check for " + getApplication().getPackageName());
            cancel();
        }
    }

    private void stopPushers(final Integer startId)
    {
        execute(getApplication(), new Runnable()
        {
            public void run()
            {
                MessagingController.getInstance(getApplication()).stopAllPushing();
                PushService.stopService(MailService.this);
            }
        }
        , K9.MAIL_SERVICE_WAKE_LOCK_TIMEOUT, startId);
    }

    private void reschedulePushers(final boolean hasConnectivity, final boolean doBackground, final Integer startId)
    {
        execute(getApplication(), new Runnable()
        {
            public void run()
            {

                if (K9.DEBUG)
                    Log.i(K9.LOG_TAG, "Rescheduling pushers");
                stopPushers(null);
                if (hasConnectivity && doBackground)
                {
                    setupPushers(null);
                    schedulePushers(startId);
                }

            }
        }
        , K9.MAIL_SERVICE_WAKE_LOCK_TIMEOUT, null);
    }

    private void setupPushers(final Integer startId)
    {
        execute(getApplication(), new Runnable()
        {
            public void run()
            {
                boolean pushing = false;
                for (Account account : Preferences.getPreferences(MailService.this).getAccounts())
                {
                    if (K9.DEBUG)
                        Log.i(K9.LOG_TAG, "Setting up pushers for account " + account.getDescription());
                    pushing |= MessagingController.getInstance(getApplication()).setupPushing(account);
                }
                if (pushing)
                {
                    PushService.startService(MailService.this);
                }
            }
        }
        , K9.MAIL_SERVICE_WAKE_LOCK_TIMEOUT, startId);
    }

    private void refreshPushers(final Integer startId)
    {
        execute(getApplication(), new Runnable()
        {
            public void run()
            {
                try
                {
                    long nowTime = System.currentTimeMillis();
                    if (K9.DEBUG)
                        Log.i(K9.LOG_TAG, "Refreshing pushers");
                    Collection<Pusher> pushers = MessagingController.getInstance(getApplication()).getPushers();
                    for (Pusher pusher : pushers)
                    {
                        long lastRefresh = pusher.getLastRefresh();
                        int refreshInterval = pusher.getRefreshInterval();
                        long sinceLast = nowTime - lastRefresh;
                        if (sinceLast + 10000 > refreshInterval)  // Add 10 seconds to keep pushers in sync, avoid drift
                        {
                            if (K9.DEBUG)
                            {
                                Log.d(K9.LOG_TAG, "PUSHREFRESH: refreshing lastRefresh = " + lastRefresh + ", interval = " + refreshInterval
                                        + ", nowTime = " + nowTime + ", sinceLast = " + sinceLast);
                            }
                            pusher.refresh();
                            pusher.setLastRefresh(nowTime);
                        }
                        else
                        {
                            if (K9.DEBUG)
                            {
                                Log.d(K9.LOG_TAG, "PUSHREFRESH: NOT refreshing lastRefresh = " + lastRefresh + ", interval = " + refreshInterval
                                        + ", nowTime = " + nowTime + ", sinceLast = " + sinceLast);
                            }
                        }
                    }
                }
                catch (Exception e)
                {
                    Log.e(K9.LOG_TAG, "Exception while refreshing pushers", e);
                }
            }
        }
        , K9.MAIL_SERVICE_WAKE_LOCK_TIMEOUT, startId);
    }

    private void schedulePushers(final Integer startId)
    {
        execute(getApplication(), new Runnable()
        {
            public void run()
            {
                int minInterval = -1;

                Collection<Pusher> pushers = MessagingController.getInstance(getApplication()).getPushers();
                for (Pusher pusher : pushers)
                {
                    int interval = pusher.getRefreshInterval();
                    if (interval > 0 && (interval < minInterval || minInterval == -1))
                    {
                        minInterval = interval;
                    }
                }
                if (K9.DEBUG)
                {
                    Log.v(K9.LOG_TAG, "Pusher refresh interval = " + minInterval);
                }
                if (minInterval > 0)
                {
                    long nextTime = System.currentTimeMillis() + minInterval;
                    if (K9.DEBUG)
                        Log.d(K9.LOG_TAG, "Next pusher refresh scheduled for " + new Date(nextTime));
                    Intent i = new Intent();
                    i.setClassName(getApplication().getPackageName(), "com.fsck.k9.service.MailService");
                    i.setAction(ACTION_REFRESH_PUSHERS);
                    BootReceiver.scheduleIntent(MailService.this, nextTime, i);
                }
            }
        }
        , K9.MAIL_SERVICE_WAKE_LOCK_TIMEOUT, startId);
    }


    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    public static long getNextPollTime()
    {
        return nextCheck;
    }


}
