
package com.android.email.service;

import java.util.HashMap;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Config;
import android.util.Log;
import android.text.TextUtils;
import android.net.Uri;

import com.android.email.Account;
import com.android.email.Email;
import com.android.email.MessagingController;
import com.android.email.MessagingListener;
import com.android.email.Preferences;
import com.android.email.R;
import com.android.email.activity.Accounts;
import com.android.email.activity.FolderMessageList;

/**
 */
public class MailService extends Service {
    private static final String ACTION_CHECK_MAIL = "com.android.email.intent.action.MAIL_SERVICE_WAKEUP";
    private static final String ACTION_RESCHEDULE = "com.android.email.intent.action.MAIL_SERVICE_RESCHEDULE";
    private static final String ACTION_CANCEL = "com.android.email.intent.action.MAIL_SERVICE_CANCEL";

    private Listener mListener = new Listener();

    private int mStartId;

    public static void actionReschedule(Context context) {
        Intent i = new Intent();
        i.setClass(context, MailService.class);
        i.setAction(MailService.ACTION_RESCHEDULE);
        context.startService(i);
    }

    public static void actionCancel(Context context)  {
        Intent i = new Intent();
        i.setClass(context, MailService.class);
        i.setAction(MailService.ACTION_CANCEL);
        context.startService(i);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        this.mStartId = startId;

        MessagingController.getInstance(getApplication()).addListener(mListener);
        if (ACTION_CHECK_MAIL.equals(intent.getAction())) {
            if (Config.LOGV) {
                Log.v(Email.LOG_TAG, "***** MailService *****: checking mail");
            }
            MessagingController.getInstance(getApplication()).checkMail(this, null, mListener);
        }
        else if (ACTION_CANCEL.equals(intent.getAction())) {
            if (Config.LOGV) {
                Log.v(Email.LOG_TAG, "***** MailService *****: cancel");
            }
            cancel();
            stopSelf(startId);
        }
        else if (ACTION_RESCHEDULE.equals(intent.getAction())) {
            if (Config.LOGV) {
                Log.v(Email.LOG_TAG, "***** MailService *****: reschedule");
            }
            reschedule();
            stopSelf(startId);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MessagingController.getInstance(getApplication()).removeListener(mListener);
    }

    private void cancel() {
        AlarmManager alarmMgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent();
        i.setClassName("com.android.email", "com.android.email.service.MailService");
        i.setAction(ACTION_CHECK_MAIL);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        alarmMgr.cancel(pi);
    }

    private void reschedule() {
        AlarmManager alarmMgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent();
        i.setClassName("com.android.email", "com.android.email.service.MailService");
        i.setAction(ACTION_CHECK_MAIL);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);

        int shortestInterval = -1;
        for (Account account : Preferences.getPreferences(this).getAccounts()) {
            if (account.getAutomaticCheckIntervalMinutes() != -1
                    && (account.getAutomaticCheckIntervalMinutes() < shortestInterval || shortestInterval == -1)) {
                shortestInterval = account.getAutomaticCheckIntervalMinutes();
            }
        }

        if (shortestInterval == -1) {
            alarmMgr.cancel(pi);
        }
        else {
            alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()
                    + (shortestInterval * (60 * 1000)), pi);
        }
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    class Listener extends MessagingListener {
        HashMap<Account, Integer> accountsWithNewMail = new HashMap<Account, Integer>();

        @Override
        public void checkMailStarted(Context context, Account account) {
            accountsWithNewMail.clear();
        }

        @Override
        public void checkMailFailed(Context context, Account account, String reason) {
            reschedule();
            stopSelf(mStartId);
        }

        @Override
        public void synchronizeMailboxFinished(
                Account account,
                String folder,
                int totalMessagesInMailbox,
                int numNewMessages) {
            if (account.isNotifyNewMail() && numNewMessages > 0) {
                accountsWithNewMail.put(account, numNewMessages);
            }
        }

        @Override
        public void checkMailFinished(Context context, Account account) {
            NotificationManager notifMgr = (NotificationManager)context
                    .getSystemService(Context.NOTIFICATION_SERVICE);

            if (accountsWithNewMail.size() > 0) {
                Notification notif = new Notification(R.drawable.stat_notify_email_generic,
                        getString(R.string.notification_new_title), System.currentTimeMillis());
                boolean vibrate = false;
                String ringtone = null;
                if (accountsWithNewMail.size() > 1) {
                    for (Account account1 : accountsWithNewMail.keySet()) {
                        if (account1.isVibrate()) vibrate = true;
                        ringtone = account1.getRingtone();
                    }
                    Intent i = new Intent(context, Accounts.class);
                    PendingIntent pi = PendingIntent.getActivity(context, 0, i, 0);
                    notif.setLatestEventInfo(context, getString(R.string.notification_new_title),
                            getString(R.string.notification_new_multi_account_fmt,
                                    accountsWithNewMail.size()), pi);
                } else {
                    Account account1 = accountsWithNewMail.keySet().iterator().next();
                    int totalNewMails = accountsWithNewMail.get(account1);
                    Intent i = FolderMessageList.actionHandleAccountIntent(context, account1, Email.INBOX);
                    PendingIntent pi = PendingIntent.getActivity(context, 0, i, 0);
                    notif.setLatestEventInfo(context, getString(R.string.notification_new_title),
                            getString(R.string.notification_new_one_account_fmt, totalNewMails,
                                    account1.getDescription()), pi);
                    vibrate = account1.isVibrate();
                    ringtone = account1.getRingtone();
                }
                notif.defaults = Notification.DEFAULT_LIGHTS;
                notif.sound = TextUtils.isEmpty(ringtone) ? null : Uri.parse(ringtone);
                if (vibrate) {
                    notif.defaults |= Notification.DEFAULT_VIBRATE;
                }
                notifMgr.notify(1, notif);
            }

            reschedule();
            stopSelf(mStartId);
        }
    }
}
