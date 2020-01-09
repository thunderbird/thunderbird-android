package com.fsck.k9.fragment;


import java.util.List;

import android.database.Cursor;
import android.text.TextUtils;

import com.fsck.k9.Account;
import com.fsck.k9.DI;
import com.fsck.k9.Preferences;
import com.fsck.k9.controller.MessageReference;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.mailstore.LocalStoreProvider;

import static com.fsck.k9.fragment.MLFProjectionInfo.SENDER_LIST_COLUMN;


public class MlfUtils {

    static LocalFolder getOpenFolder(String folderServerId, Account account) throws MessagingException {
        LocalStore localStore = DI.get(LocalStoreProvider.class).getInstance(account);
        LocalFolder localFolder = localStore.getFolder(folderServerId);
        localFolder.open();
        return localFolder;
    }

    static void setLastSelectedFolder(Preferences preferences,
            List<MessageReference> messages, String destFolderName) {
        MessageReference firstMsg = messages.get(0);
        Account account = preferences.getAccount(firstMsg.getAccountUuid());
        account.setLastSelectedFolder(destFolderName);
    }

    static String getSenderAddressFromCursor(Cursor cursor) {
        String fromList = cursor.getString(SENDER_LIST_COLUMN);
        Address[] fromAddrs = Address.unpack(fromList);
        return (fromAddrs.length > 0) ? fromAddrs[0].getAddress() : null;
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
