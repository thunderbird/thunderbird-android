
package com.android.email.service;

import java.util.Date;
import java.util.HashMap;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
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
import com.android.email.mail.Folder;
import com.android.email.mail.MessagingException;
import com.android.email.mail.Store;

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
    public void onCreate() {
    	super.onCreate();
    	Log.v(Email.LOG_TAG, "***** MailService *****: onCreate");
    }

    @Override
    public void onStart(Intent intent, int startId) {
    	setForeground(true);  // if it gets killed once, it'll never restart
    		Log.v(Email.LOG_TAG, "***** MailService *****: onStart(" + intent + ", " + startId + ")");
        super.onStart(intent, startId);
        this.mStartId = startId;

       // MessagingController.getInstance(getApplication()).addListener(mListener);
        if (ACTION_CHECK_MAIL.equals(intent.getAction())) {
            //if (Config.LOGV) {
          MessagingController.getInstance(getApplication()).log("***** MailService *****: checking mail");
                Log.v(Email.LOG_TAG, "***** MailService *****: checking mail");
            //}

            MessagingController controller = MessagingController.getInstance(getApplication());
            Listener listener = (Listener)controller.getCheckMailListener();
            if (listener == null)
            {
              MessagingController.getInstance(getApplication()).log("***** MailService *****: starting new check");

              mListener.wakeLockAcquire();
              controller.setCheckMailListener(mListener);
              controller.checkMail(this, null, false, false, mListener);
            }
            else
            {
              MessagingController.getInstance(getApplication()).log("***** MailService *****: renewing WakeLock");

              listener.wakeLockAcquire();
            }

            reschedule();
	    //            stopSelf(startId);
        }
        else if (ACTION_CANCEL.equals(intent.getAction())) {
            if (Config.LOGV) {
                Log.v(Email.LOG_TAG, "***** MailService *****: cancel");
            }
            MessagingController.getInstance(getApplication()).log("***** MailService *****: cancel");

            cancel();
	    //            stopSelf(startId);
        }
        else if (ACTION_RESCHEDULE.equals(intent.getAction())) {
            if (Config.LOGV) {
                Log.v(Email.LOG_TAG, "***** MailService *****: reschedule");
            }
            MessagingController.getInstance(getApplication()).log("***** MailService *****: reschedule");
            reschedule();
	    //            stopSelf(startId);
        }
    }

    @Override
    public void onDestroy() {
    		Log.v(Email.LOG_TAG, "***** MailService *****: onDestroy()");
        super.onDestroy();
   //     MessagingController.getInstance(getApplication()).removeListener(mListener);
    }

    private void cancel() {
        AlarmManager alarmMgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent();
        i.setClassName(getApplication().getPackageName(), "com.android.email.service.MailService");
        i.setAction(ACTION_CHECK_MAIL);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        alarmMgr.cancel(pi);
    }

    private void reschedule() {
        AlarmManager alarmMgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent();
        i.setClassName(getApplication().getPackageName(), "com.android.email.service.MailService");
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
        		Log.v(Email.LOG_TAG, "No next check scheduled for package " + getApplication().getPackageName());
            alarmMgr.cancel(pi);
            stopSelf(mStartId);
        }
        else
        {
	        long delay = (shortestInterval * (60 * 1000));

	        long nextTime = System.currentTimeMillis() + delay;
	        try
	        {
	          String checkString = "Next check for package " + getApplication().getPackageName() + " scheduled for " + new Date(nextTime);
	          Log.v(Email.LOG_TAG, checkString);
	          MessagingController.getInstance(getApplication()).log(checkString);
	        }
	        catch (Exception e)
	        {
	          // I once got a NullPointerException deep in new Date();
	          Log.e(Email.LOG_TAG, "Exception while logging", e);
	        }
	        
	        alarmMgr.set(AlarmManager.RTC_WAKEUP, nextTime, pi);
        }

    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    class Listener extends MessagingListener {
        HashMap<String, Integer> accountsChecked = new HashMap<String, Integer>();
        private WakeLock wakeLock = null;

        // wakelock strategy is to be very conservative.  If there is any reason to release, then release
        // don't want to take the chance of running wild
        public synchronized void wakeLockAcquire()
        {
          WakeLock oldWakeLock = wakeLock;

         	PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        	wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Email");
        	wakeLock.setReferenceCounted(false);
         	wakeLock.acquire(Email.WAKE_LOCK_TIMEOUT);

         	if (oldWakeLock != null)
         	{
         	  oldWakeLock.release();
         	}

        }
        public synchronized void wakeLockRelease()
        {
        	if (wakeLock != null)
        	{
        		wakeLock.release();
        		wakeLock = null;
        	}
        }
        @Override
        public void checkMailStarted(Context context, Account account) {
            accountsChecked.clear();
        }

        @Override
        public void checkMailFailed(Context context, Account account, String reason) {
            release();
        }

        @Override
        public void synchronizeMailboxFinished(
                Account account,
                String folder,
                int totalMessagesInMailbox,
                int numNewMessages) {
            if (account.isNotifyNewMail()) {
              Integer existingNewMessages = accountsChecked.get(account.getUuid());
              if (existingNewMessages == null)
              {
                existingNewMessages = 0;
              }
              accountsChecked.put(account.getUuid(), existingNewMessages + numNewMessages);
            }
        }

        private void checkMailDone(Context context, Account doNotUseaccount)
        {
            if (accountsChecked.isEmpty())
            {
                return;
            }

            NotificationManager notifMgr =
                (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

            int index = 0;
            for (Account thisAccount : Preferences.getPreferences(context).getAccounts()) {
                Integer newMailCount = accountsChecked.get(thisAccount.getUuid());
                int unreadMessageCount = -1;
                if (newMailCount != null)
                {
                    try
                    {
                        unreadMessageCount = thisAccount.getUnreadMessageCount(context, getApplication());
                        if (unreadMessageCount > 0 && newMailCount > 0)
                        {
                            String notice = getString(R.string.notification_new_one_account_fmt, unreadMessageCount,
                            thisAccount.getDescription());
                            Notification notif = new Notification(R.drawable.stat_notify_email_generic,
                                getString(R.string.notification_new_title), System.currentTimeMillis() + (index*1000));
                          
                            notif.number = unreadMessageCount;
                    
                            Intent i = FolderMessageList.actionHandleAccountIntent(context, thisAccount);
        
                            PendingIntent pi = PendingIntent.getActivity(context, 0, i, 0);
        
                            notif.setLatestEventInfo(context, getString(R.string.notification_new_title), notice, pi);
        
                            String ringtone = thisAccount.getRingtone();
                            notif.sound = TextUtils.isEmpty(ringtone) ? null : Uri.parse(ringtone);
        
                            if (thisAccount.isVibrate()) {
                                notif.defaults |= Notification.DEFAULT_VIBRATE;
                            }
        
                            notif.flags |= Notification.FLAG_SHOW_LIGHTS;
                            notif.ledARGB = Email.NOTIFICATION_LED_COLOR;
                            notif.ledOnMS = Email.NOTIFICATION_LED_ON_TIME;
                            notif.ledOffMS = Email.NOTIFICATION_LED_OFF_TIME;
        
                            notifMgr.notify(thisAccount.getAccountNumber(), notif);
                        }
                        else if (unreadMessageCount == 0)
                        {
                          notifMgr.cancel(thisAccount.getAccountNumber());
                        }
                    }
                    catch (MessagingException me)
                    {
                        Log.e(Email.LOG_TAG, "***** MailService *****: couldn't get unread count for account " +
                            thisAccount.getDescription(), me);
                    }
                }
            }//for accounts
        }//checkMailDone

        private void release()
        {
          MessagingController controller = MessagingController.getInstance(getApplication());
          controller.setCheckMailListener(null);
          reschedule();
          wakeLockRelease();
	  //          stopSelf(mStartId);
        }

        @Override
        public void checkMailFinished(Context context, Account account) {

        	Log.v(Email.LOG_TAG, "***** MailService *****: checkMailFinished");
        	try
        	{
        		checkMailDone(context, account);
        	}
        	finally
        	{
        	  release();
        	}
        }
    }
}
