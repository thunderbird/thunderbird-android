package com.android.email;

import java.util.List;

import android.app.Application;
import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import com.android.email.mail.Folder;
import com.android.email.mail.Message;
import com.android.email.mail.PushReceiver;
import com.android.email.mail.Store;
import com.android.email.mail.Folder.OpenMode;
import com.android.email.mail.store.LocalStore;
import com.android.email.mail.store.LocalStore.LocalFolder;
import com.android.email.service.SleepService;

public class MessagingControllerPushReceiver implements PushReceiver
{
    final Account account;
    final MessagingController controller;
    final Application mApplication;
    
    public MessagingControllerPushReceiver(Application nApplication, Account nAccount, MessagingController nController)
    {
        account = nAccount;
        controller = nController;
        mApplication = nApplication;
    }
        ThreadLocal<WakeLock> threadWakeLock = new ThreadLocal<WakeLock>();
        public void acquireWakeLock()
        {
            WakeLock wakeLock = threadWakeLock.get();
            if (wakeLock == null)
            {
                PowerManager pm = (PowerManager) mApplication.getSystemService(Context.POWER_SERVICE);
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Email");
                wakeLock.setReferenceCounted(false);
                threadWakeLock.set(wakeLock);
            }
            wakeLock.acquire(Email.PUSH_WAKE_LOCK_TIMEOUT);
            if (Email.DEBUG)
            {
                Log.d(Email.LOG_TAG, "Acquired WakeLock for Pushing for thread " + Thread.currentThread().getName());
            }
        }

        public void releaseWakeLock()
        {
            if (Email.DEBUG)
            {
                Log.d(Email.LOG_TAG, "Considering releasing WakeLock for Pushing");
            }
            WakeLock wakeLock = threadWakeLock.get();
            if (wakeLock != null)
            {

                if (Email.DEBUG)
                {
                    Log.d(Email.LOG_TAG, "Releasing WakeLock for Pushing for thread " + Thread.currentThread().getName());
                }
                wakeLock.release();
            }
            else
            {
                Log.e(Email.LOG_TAG, "No WakeLock waiting to be released for thread " + Thread.currentThread().getName());
            }
        }

        public void messagesFlagsChanged(Folder folder,
                                         List<Message> messages)
        {
            controller.messagesArrived(account, folder, messages, true);

        }
        public void messagesArrived(Folder folder, List<Message> messages)
        {
            controller.messagesArrived(account, folder, messages, false);
        }

        public void sleep(long millis)
        {
            SleepService.sleep(mApplication, millis, threadWakeLock.get(), Email.PUSH_WAKE_LOCK_TIMEOUT);
        }

        public void pushError(String errorMessage, Exception e)
        {
            String errMess = errorMessage;
            String body = null;

            if (errMess == null && e != null)
            {
                errMess = e.getMessage();
            }
            body = errMess;
            if (e != null)
            {
                body = e.toString();
            }
            controller.addErrorMessage(account, errMess, body);
        }

        public String getPushState(String folderName)
        {
            LocalFolder localFolder = null;
            try
            {
                LocalStore localStore = (LocalStore) Store.getInstance(account.getLocalStoreUri(), mApplication);
                localFolder= (LocalFolder) localStore.getFolder(folderName);
                localFolder.open(OpenMode.READ_WRITE);
                return localFolder.getPushState();
            }
            catch (Exception e)
            {
                Log.e(Email.LOG_TAG, "Unable to get push state from account " + account.getDescription()
                      + ", folder " + folderName, e);
                return null;
            }
            finally
            {
                if (localFolder != null)
                {
                    try
                    {
                        localFolder.close(false);
                    }
                    catch (Exception e)
                    {
                        Log.e(Email.LOG_TAG, "Unable to close folder '" + folderName + "' in account " + account.getDescription(), e);
                    }
                }
            }
        }

        public void setPushActive(String folderName, boolean enabled)
        {
            for (MessagingListener l : controller.getListeners())
            {
                l.setPushActive(account, folderName, enabled);
            }
        }

}
