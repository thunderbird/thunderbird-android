package com.fsck.k9.controller.tasks;


import java.util.Set;

import android.content.Context;
import android.support.annotation.VisibleForTesting;

import com.fsck.k9.Account;
import com.fsck.k9.controller.ControllerUtils;
import com.fsck.k9.controller.IMessageController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.controller.UnavailableAccountException;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.UnavailableStorageException;
import timber.log.Timber;


public class ClearFolderTask implements Runnable {

    private final IMessageController controller;
    private final Context context;
    private final Account account;
    private final String folderName;
    private final MessagingListener taskListener;
    private final Set<MessagingListener> listeners;

    public ClearFolderTask(IMessageController controller, Context context, Account account,
            String folderName, MessagingListener taskListener,
            Set<MessagingListener> listeners) {
        this.controller = controller;
        this.context = context;
        this.account = account;
        this.folderName = folderName;
        this.taskListener = taskListener;
        this.listeners = listeners;
    }

    @Override
    public void run() {

    }

    @VisibleForTesting
    protected void clearFolderSynchronous() {
        LocalFolder localFolder = null;
        try {
            localFolder = account.getLocalStore().getFolder(folderName);
            localFolder.open(Folder.OPEN_MODE_RW);
            localFolder.clearAllMessages();
        } catch (UnavailableStorageException e) {
            Timber.i("Failed to clear folder because storage is not available - trying again later.");
            throw new UnavailableAccountException(e);
        } catch (Exception e) {
            Timber.e(e, "clearFolder failed");
        } finally {
            ControllerUtils.closeFolder(localFolder);
        }

        new ListFoldersTask(controller, context, account, false, taskListener, listeners).run();
    }
}
