package com.fsck.k9.controller;


import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.cache.EmailProviderCache;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mailstore.LocalMessage;


class SyncUtils {

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
