
package com.android.email.service;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.NetworkInfo.State;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Config;
import android.util.Log;

import com.android.email.Account;
import com.android.email.Email;
import com.android.email.MessagingController;
import com.android.email.MessagingListener;
import com.android.email.Preferences;
import com.android.email.R;
import com.android.email.mail.Address;
import com.android.email.mail.Message;
import com.android.email.mail.MessagingException;
import com.android.email.mail.Pusher;
import com.android.email.EmailReceivedIntent;

/**
 */
public class MailService extends Service {
    private static final String ACTION_APP_STARTED = "com.android.email.intent.action.MAIL_SERVICE_APP_STARTED";
    private static final String ACTION_CHECK_MAIL = "com.android.email.intent.action.MAIL_SERVICE_WAKEUP";
    private static final String ACTION_RESCHEDULE = "com.android.email.intent.action.MAIL_SERVICE_RESCHEDULE";
    private static final String ACTION_CANCEL = "com.android.email.intent.action.MAIL_SERVICE_CANCEL";
    private static final String ACTION_REFRESH_PUSHERS = "com.android.email.intent.action.MAIL_SERVICE_REFRESH_PUSHERS";
    private static final String CONNECTIVITY_CHANGE = "com.android.email.intent.action.MAIL_SERVICE_CONNECTIVITY_CHANGE";
    private static final String CANCEL_CONNECTIVITY_NOTICE = "com.android.email.intent.action.MAIL_SERVICE_CANCEL_CONNECTIVITY_NOTICE";

    private static final String HAS_CONNECTIVITY = "com.android.email.intent.action.MAIL_SERVICE_HAS_CONNECTIVITY";
    
    private Listener mListener = new Listener();
    
    private State state = null;

    private int mStartId;
 
    public static void actionReschedule(Context context) {
        Intent i = new Intent();
        i.setClass(context, MailService.class);
        i.setAction(MailService.ACTION_RESCHEDULE);
        context.startService(i);
    }
    
    public static void appStarted(Context context) {
        Intent i = new Intent();
        i.setClass(context, MailService.class);
        i.setAction(MailService.ACTION_APP_STARTED);
        context.startService(i);
    }
    
//    private static void checkMail(Context context) {
//        Intent i = new Intent();
//        i.setClass(context, MailService.class);
//        i.setAction(MailService.ACTION_CHECK_MAIL);
//        context.startService(i);
//    }

    public static void actionCancel(Context context)  {
        Intent i = new Intent();
        i.setClass(context, MailService.class);
        i.setAction(MailService.ACTION_CANCEL);
        context.startService(i);
    }
    
    public static void connectivityChange(Context context, boolean hasConnectivity)  {
        Intent i = new Intent();
        i.setClass(context, MailService.class);
        i.setAction(MailService.CONNECTIVITY_CHANGE);
        i.putExtra(HAS_CONNECTIVITY, hasConnectivity);
        context.startService(i);
    }

    @Override
    public void onCreate() {
    	super.onCreate();
    	Log.v(Email.LOG_TAG, "***** MailService *****: onCreate");
    }

    @Override
    public void onStart(Intent intent, int startId) {
        
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Email");
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire(Email.MAIL_SERVICE_WAKE_LOCK_TIMEOUT);
        try
        {
           
            ConnectivityManager connectivityManager = (ConnectivityManager)getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);
            state = State.DISCONNECTED;
            if (connectivityManager != null)
            {
                NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
                if (netInfo != null)
                {
                    state = netInfo.getState();
                
                    if (state == State.CONNECTED)
                    {   
                        Log.i(Email.LOG_TAG, "Currently connected to a network");
                    }
                    else
                    {
                        Log.i(Email.LOG_TAG, "Current network state = " + state);
                    }
                }
            }
            
            
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
                if (state == State.CONNECTED)
                {
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
                boolean polling = reschedule();
                boolean pushing = reschedulePushers();
                if (polling == false && pushing == false)
                {
                    Log.i(Email.LOG_TAG, "Neither pushing nor polling, so stopping");
                    stopSelf(startId);
                }
    	    //            stopSelf(startId);
            }
            else if (ACTION_REFRESH_PUSHERS.equals(intent.getAction()))
            {
                schedulePushers();
                try
                {
                    if (state == State.CONNECTED)
                    {
                        Log.i(Email.LOG_TAG, "Refreshing pushers");
                        Collection<Pusher> pushers = MessagingController.getInstance(getApplication()).getPushers();
                        for (Pusher pusher : pushers)
                        {
                            pusher.refresh();
                        }
                    }
                }
                catch (Exception e)
                {
                    Log.e(Email.LOG_TAG, "Exception while refreshing pushers", e);
                }
            }
            else if (CONNECTIVITY_CHANGE.equals(intent.getAction()))
            {
                boolean hasConnectivity = intent.getBooleanExtra(HAS_CONNECTIVITY, true);
                Log.i(Email.LOG_TAG, "Got connectivity action with hasConnectivity = " + hasConnectivity);
                notifyConnectionStatus(hasConnectivity);
                if (hasConnectivity)
                {
                    reschedulePushers();
    		// TODO: Make it send pending outgoing messages here
                    //checkMail(getApplication());
                }
                else
                {
                    stopPushers();
                }
            }
            else if (CANCEL_CONNECTIVITY_NOTICE.equals(intent.getAction()))
            {
                notifyConnectionStatus(true);
            }
            else if (ACTION_APP_STARTED.equals(intent.getAction()))
            {
                setupListener(this);
            }
        }
        finally
        {
            if (wakeLock != null)
            {
                wakeLock.release();
            }
        }
    }
    
    private void notifyConnectionStatus(boolean hasConnectivity)
    {
        NotificationManager notifMgr =
            (NotificationManager)getApplication().getSystemService(Context.NOTIFICATION_SERVICE);
        if (hasConnectivity == false)
        {
            String notice = getApplication().getString(R.string.no_connection_alert);
            String header = getApplication().getString(R.string.alert_header);
            
            
            Notification notif = new Notification(R.drawable.stat_notify_email_generic,
                    header, System.currentTimeMillis());
            
            Intent i = new Intent();
            i.setClassName(getApplication().getPackageName(), "com.android.email.service.MailService");
            i.setAction(MailService.CANCEL_CONNECTIVITY_NOTICE);
    
            PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
    
            notif.setLatestEventInfo(getApplication(), header, notice, pi);
            notif.flags = Notification.FLAG_ONGOING_EVENT;
    
            notifMgr.notify(Email.CONNECTIVITY_ID, notif); 
        }
        else
        {
            notifMgr.cancel(Email.CONNECTIVITY_ID);
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

    private boolean reschedule() {
        boolean polling = true;
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
            polling = false;
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
        return polling;
    }
    
    private void stopPushers()
    {
        MessagingController.getInstance(getApplication()).stopAllPushing();
    }
    
    private boolean reschedulePushers()
    {
        boolean pushing = false;
        Log.i(Email.LOG_TAG, "Rescheduling pushers");
        stopPushers();
        if (state == State.CONNECTED)   
        {
            for (Account account : Preferences.getPreferences(this).getAccounts()) {
                Log.i(Email.LOG_TAG, "Setting up pushers for account " + account.getDescription());
                Pusher pusher = MessagingController.getInstance(getApplication()).setupPushing(account);
                if (pusher != null)
                {
                    pushing = true;
                    Log.i(Email.LOG_TAG, "Starting configured pusher for account " + account.getDescription());
                    pusher.start();
                }
            }
            schedulePushers();
        }
        return pushing;
        
    }
    
    private void schedulePushers()
    {
        int minInterval = -1;
        
        Collection<Pusher> pushers = MessagingController.getInstance(getApplication()).getPushers();
        for (Pusher pusher : pushers)
        {
            int interval = pusher.getRefreshInterval();
            if (interval != -1 && (interval < minInterval || minInterval == -1))
            {
                minInterval = interval;
            }
        }
        if (Email.DEBUG)
        {
            Log.v(Email.LOG_TAG, "Pusher refresh interval = " + minInterval);
        }
        if (minInterval != -1)
        {

            AlarmManager alarmMgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
            long nextTime = System.currentTimeMillis() + minInterval;
            String checkString = "Next pusher refresh scheduled for " + new Date(nextTime);
            if (Email.DEBUG)
            {
                Log.d(Email.LOG_TAG, checkString);
            }
            Intent i = new Intent();
            i.setClassName(getApplication().getPackageName(), "com.android.email.service.MailService");
            i.setAction(ACTION_REFRESH_PUSHERS);
            PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
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

            for (Account thisAccount : Preferences.getPreferences(context).getAccounts()) {
                Integer newMailCount = accountsChecked.get(thisAccount.getUuid());
                if (newMailCount != null)
                {
                    try
                    {
                        int  unreadMessageCount = thisAccount.getUnreadMessageCount(context, getApplication());
                        if (unreadMessageCount > 0 && newMailCount > 0)
                        {
                            MessagingController.getInstance(getApplication()).notifyAccount(context, thisAccount, unreadMessageCount);
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
    public void setupListener(final Context context)
    {
        Log.i(Email.LOG_TAG, "Setting up listener for new mail Intents");
        MessagingController.getInstance(getApplication()).addListener(new MessagingListener()
        {
            public final void synchronizeMailboxNewMessage(Account account, String folder, Message message) {
                try {
                  Uri uri = Uri.parse("email://messages/" + account.getAccountNumber() + "/" + Uri.encode(folder) + "/" + Uri.encode(message.getUid()));
                  android.content.Intent intent = new android.content.Intent(EmailReceivedIntent.ACTION_EMAIL_RECEIVED, uri);
                  intent.putExtra(EmailReceivedIntent.EXTRA_ACCOUNT, account.getDescription());
                  intent.putExtra(EmailReceivedIntent.EXTRA_FOLDER, folder);
                  intent.putExtra(EmailReceivedIntent.EXTRA_SENT_DATE, message.getSentDate());
                  intent.putExtra(EmailReceivedIntent.EXTRA_FROM, Address.toString(message.getFrom()));
                  intent.putExtra(EmailReceivedIntent.EXTRA_TO, Address.toString(message.getRecipients(Message.RecipientType.TO)));
                  intent.putExtra(EmailReceivedIntent.EXTRA_CC, Address.toString(message.getRecipients(Message.RecipientType.CC)));
                  intent.putExtra(EmailReceivedIntent.EXTRA_BCC, Address.toString(message.getRecipients(Message.RecipientType.BCC)));
                  intent.putExtra(EmailReceivedIntent.EXTRA_SUBJECT, message.getSubject());
                  context.sendBroadcast(intent);
                  Log.i(Email.LOG_TAG, "Broadcasted intent: " + message.getSubject());
          }
              catch (MessagingException e) {
                  Log.w(Email.LOG_TAG, "Account=" + account.getName() + " folder=" + folder + "message uid=" + message.getUid(), e);
              }
            }
        }
        );
        
    }
    
}
