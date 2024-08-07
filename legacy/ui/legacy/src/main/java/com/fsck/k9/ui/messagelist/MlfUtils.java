package com.fsck.k9.ui.messagelist;


import java.util.List;

import android.text.TextUtils;

import app.k9mail.legacy.account.Account;
import app.k9mail.legacy.di.DI;
import com.fsck.k9.controller.MessageReference;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.mailstore.LocalStoreProvider;
import app.k9mail.legacy.account.AccountManager;


public class MlfUtils {

    static LocalFolder getOpenFolder(long folderId, Account account) throws MessagingException {
        LocalStore localStore = DI.get(LocalStoreProvider.class).getInstance(account);
        LocalFolder localFolder = localStore.getFolder(folderId);
        localFolder.open();
        return localFolder;
    }

    static void setLastSelectedFolder(AccountManager accountManager, List<MessageReference> messages, long folderId) {
        MessageReference firstMsg = messages.get(0);
        Account account = accountManager.getAccount(firstMsg.getAccountUuid());
        account.setLastSelectedFolderId(folderId);
    }

    static String buildSubject(String subjectFromCursor, String emptySubject, int threadCount) {
        if (TextUtils.isEmpty(subjectFromCursor)) {
            return emptySubject;
        } else if (threadCount > 1) {
            // If this is a thread, strip the RE/FW from the subject.  "Be like Outlook."
            return Utility.stripSubject(subjectFromCursor);
        }
        return subjectFromCursor;
    }
}
