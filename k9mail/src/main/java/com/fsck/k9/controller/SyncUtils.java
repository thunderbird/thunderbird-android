package com.fsck.k9.controller;


import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.cache.EmailProviderCache;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Folder.FolderType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalStore;
import timber.log.Timber;


class SyncUtils {

    static LocalFolder getOpenedLocalFolder(Account account, String folderName) throws MessagingException{
        final LocalStore localStore = account.getLocalStore();
        LocalFolder localFolder = localStore.getFolder(folderName);
        localFolder.open(Folder.OPEN_MODE_RW);
        return localFolder;
    }

    /*
     * If the folder is a "special" folder we need to see if it exists
     * on the remote server. It if does not exist we'll try to create it. If we
     * can't create we'll abort. This will happen on every single Pop3 folder as
     * designed and on Imap folders during error conditions. This allows us
     * to treat Pop3 and Imap the same in this code.
     */
    static boolean verifyOrCreateRemoteSpecialFolder(Account account, String folder, Folder remoteFolder,
            MessagingListener listener, MessagingController controller) throws MessagingException {
        if (folder.equals(account.getTrashFolderName()) ||
                folder.equals(account.getSentFolderName()) ||
                folder.equals(account.getDraftsFolderName())) {
            if (!remoteFolder.exists()) {
                if (!remoteFolder.create(FolderType.HOLDS_MESSAGES)) {
                    for (MessagingListener l : controller.getListeners(listener)) {
                        l.synchronizeMailboxFinished(account, folder, 0, 0);
                    }

                    Timber.i("Done synchronizing folder %s", folder);
                    return false;
                }
            }
        }
        return true;
    }

    static boolean isMessageSuppressed(LocalMessage message, Context context) {
        long messageId = message.getId();
        long folderId = message.getFolder().getId();

        EmailProviderCache cache = EmailProviderCache.getCache(message.getFolder().getAccountUuid(), context);
        return cache.isMessageHidden(messageId, folderId);
    }

    static boolean modeMismatch(Account.FolderMode aMode, Folder.FolderClass fMode) {
        if (aMode == Account.FolderMode.NONE
                || (aMode == Account.FolderMode.FIRST_CLASS &&
                fMode != Folder.FolderClass.FIRST_CLASS)
                || (aMode == Account.FolderMode.FIRST_AND_SECOND_CLASS &&
                fMode != Folder.FolderClass.FIRST_CLASS &&
                fMode != Folder.FolderClass.SECOND_CLASS)
                || (aMode == Account.FolderMode.NOT_SECOND_CLASS &&
                fMode == Folder.FolderClass.SECOND_CLASS)) {
            return true;
        } else {
            return false;
        }
    }
}
