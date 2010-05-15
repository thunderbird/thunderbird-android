package com.fsck.k9;

import android.app.Application;
import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Folder.OpenMode;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.PushReceiver;
import com.fsck.k9.mail.store.LocalStore;
import com.fsck.k9.mail.store.LocalStore.LocalFolder;
import com.fsck.k9.service.SleepService;

import java.util.List;
import java.util.concurrent.CountDownLatch;

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
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "K9");
            wakeLock.setReferenceCounted(false);
            threadWakeLock.set(wakeLock);
        }
        wakeLock.acquire(K9.PUSH_WAKE_LOCK_TIMEOUT);

        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "Acquired WakeLock for Pushing for thread " + Thread.currentThread().getName());
    }

    public void releaseWakeLock()
    {
        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "Considering releasing WakeLock for Pushing");

        WakeLock wakeLock = threadWakeLock.get();
        if (wakeLock != null)
        {

            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "Releasing WakeLock for Pushing for thread " + Thread.currentThread().getName());

            wakeLock.release();
        }
        else
        {
            Log.e(K9.LOG_TAG, "No WakeLock waiting to be released for thread " + Thread.currentThread().getName());
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
    public void messagesRemoved(Folder folder, List<Message> messages)
    {
        controller.messagesArrived(account, folder, messages, true);
    }

    public void syncFolder(Folder folder)
    {
        if (K9.DEBUG)
            Log.v(K9.LOG_TAG, "syncFolder(" + folder.getName() + ")");
        final CountDownLatch latch = new CountDownLatch(1);
        controller.synchronizeMailbox(account, folder.getName(), new MessagingListener()
        {
            @Override
            public void synchronizeMailboxFinished(Account account, String folder,
                                                   int totalMessagesInMailbox, int numNewMessages)
            {
                latch.countDown();
            }

            @Override
            public void synchronizeMailboxFailed(Account account, String folder,
                                                 String message)
            {
                latch.countDown();
            }
        }, folder);

        if (K9.DEBUG)
            Log.v(K9.LOG_TAG, "syncFolder(" + folder.getName() + ") about to await latch release");
        try
        {
            latch.await();
            if (K9.DEBUG)
                Log.v(K9.LOG_TAG, "syncFolder(" + folder.getName() + ") got latch release");
        }
        catch (Exception e)
        {
            Log.e(K9.LOG_TAG, "Interrupted while awaiting latch release", e);
        }
    }

    public void sleep(long millis)
    {
        SleepService.sleep(mApplication, millis, threadWakeLock.get(), K9.PUSH_WAKE_LOCK_TIMEOUT);
    }

    public void pushError(String errorMessage, Exception e)
    {
        String errMess = errorMessage;

        if (errMess == null && e != null)
        {
            errMess = e.getMessage();
        }
        controller.addErrorMessage(account, errMess, e);
    }

    public String getPushState(String folderName)
    {
        LocalFolder localFolder = null;
        try
        {
            LocalStore localStore = account.getLocalStore();
            localFolder = localStore.getFolder(folderName);
            localFolder.open(OpenMode.READ_WRITE);
            return localFolder.getPushState();
        }
        catch (Exception e)
        {
            Log.e(K9.LOG_TAG, "Unable to get push state from account " + account.getDescription()
                  + ", folder " + folderName, e);
            return null;
        }
        finally
        {
            if (localFolder != null)
            {
                localFolder.close();
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
