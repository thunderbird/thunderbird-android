package com.fsck.k9.controller;

import android.app.Application;
import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.helper.power.TracingPowerManager;
import com.fsck.k9.helper.power.TracingPowerManager.TracingWakeLock;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Folder.OpenMode;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.PushReceiver;
import com.fsck.k9.mail.store.LocalStore;
import com.fsck.k9.mail.store.LocalStore.LocalFolder;
import com.fsck.k9.service.SleepService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    @Override
    public void sleep(TracingWakeLock wakeLock, long millis)
    {
        SleepService.sleep(mApplication, millis, wakeLock, K9.PUSH_WAKE_LOCK_TIMEOUT);
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

    @Override
    public Context getContext()
    {
        return mApplication;
    }

}
