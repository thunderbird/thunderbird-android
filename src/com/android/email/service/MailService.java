
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

        MessagingController.getInstance(getApplication()).addListener(mListener);
        if (ACTION_CHECK_MAIL.equals(intent.getAction())) {
            //if (Config.LOGV) {
                Log.v(Email.LOG_TAG, "***** MailService *****: checking mail");
            //}
                reschedule();
            mListener.wakeLockAcquire();
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
    		Log.v(Email.LOG_TAG, "***** MailService *****: onDestroy()");
        super.onDestroy();
        MessagingController.getInstance(getApplication()).removeListener(mListener);
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
        }
        else
        {
	        long delay = (shortestInterval * (60 * 1000));
	        
	        long nextTime = System.currentTimeMillis() + delay;
	        Log.v(Email.LOG_TAG, "Next check for package " + getApplication().getPackageName() + " scheduled for " + new Date(nextTime));
	        alarmMgr.set(AlarmManager.RTC_WAKEUP, nextTime, pi);
        }
        
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    class Listener extends MessagingListener {
        HashMap<Account, Integer> accountsWithNewMail = new HashMap<Account, Integer>();
        private WakeLock wakeLock = null;
        
        // wakelock strategy is to be very conservative.  If there is any reason to release, then release
        // don't want to take the chance of running wild
        public synchronized void wakeLockAcquire()
        {
        	if (wakeLock == null)
        	{
           	PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
          	wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Email");
          	wakeLock.setReferenceCounted(false);
           	wakeLock.acquire(Email.WAKE_LOCK_TIMEOUT);
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
            accountsWithNewMail.clear();
        }

        @Override
        public void checkMailFailed(Context context, Account account, String reason) {
            wakeLockRelease();
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
        
        private void checkMailDone(Context context, Account doNotUseaccount)
        {
        	if (accountsWithNewMail.isEmpty())
        	{
        		return;
        	}
          StringBuffer notice = new StringBuffer();
          int accountNumber = Email.FETCHING_EMAIL_NOTIFICATION_NO_ACCOUNT;
          
          boolean vibrate = false;
		      String ringtone = null;
          for (Account thisAccount : Preferences.getPreferences(context).getAccounts()) {
          	if (thisAccount.isNotifyNewMail())
          	{
	          	int unreadMessageCount = 0;
		          try
		          {
	            	unreadMessageCount = thisAccount.getUnreadMessageCount(context, getApplication());
	            	if (unreadMessageCount > 0)
	            	{
	            		notice.append(getString(R.string.notification_new_one_account_fmt, unreadMessageCount,
	            				thisAccount.getDescription()) + "\n");
	            		if (accountNumber != Email.FETCHING_EMAIL_NOTIFICATION_MULTI_ACCOUNT_ID)   // if already set to Multi, nothing to do
	            		{
		            		if (accountNumber == Email.FETCHING_EMAIL_NOTIFICATION_NO_ACCOUNT)   // Haven't set to anything, yet, set to this account number
		            		{
		            			accountNumber = thisAccount.getAccountNumber();
		            		}
		            		else   // Another account was already set, so there is more than one with new mail
		            		{
		            			accountNumber = Email.FETCHING_EMAIL_NOTIFICATION_MULTI_ACCOUNT_ID;
		            		}
	            		}
	            	}
		          }
		          catch (MessagingException me)
		          {
		          	Log.e(Email.LOG_TAG, "***** MailService *****: couldn't get unread count for account " +
		          			thisAccount.getDescription(), me);
		          }
		          if (ringtone == null)
		          {
		          	ringtone = thisAccount.getRingtone();
		          }
		          vibrate |= thisAccount.isVibrate();
          	}
          }
          if (notice.length() > 0)
          {
	          NotificationManager notifMgr = 
	          	(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
	          
	          Notification notif = new Notification(R.drawable.stat_notify_email_generic,
	              getString(R.string.notification_new_title), System.currentTimeMillis());
	          
	          // If only one account has mail, maybe go back to the old way of targetting the account.
	          Intent i = new Intent(context, Accounts.class);
	          PendingIntent pi = PendingIntent.getActivity(context, 0, i, 0);
          
				    notif.setLatestEventInfo(context, getString(R.string.notification_new_title),
				                  notice, pi);
		
				   // notif.defaults = Notification.DEFAULT_LIGHTS;
				    notif.sound = TextUtils.isEmpty(ringtone) ? null : Uri.parse(ringtone);
				    if (vibrate) {
				       notif.defaults |= Notification.DEFAULT_VIBRATE;
				    }
				    
				    notif.flags |= Notification.FLAG_SHOW_LIGHTS;
            notif.ledARGB = Email.NOTIFICATION_LED_COLOR;
            notif.ledOnMS = Email.NOTIFICATION_LED_ON_TIME;
            notif.ledOffMS = Email.NOTIFICATION_LED_OFF_TIME;

            notifMgr.cancelAll();
				    notifMgr.notify(accountNumber, notif);
				  }

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
            wakeLockRelease();
            stopSelf(mStartId);
        	}
        }
    }
}
