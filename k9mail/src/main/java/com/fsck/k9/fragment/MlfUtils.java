package com.fsck.k9.fragment;


import java.util.List;

import android.database.Cursor;
import android.text.TextUtils;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalStore;
import timber.log.Timber;

import static com.fsck.k9.fragment.MLFProjectionInfo.SENDER_LIST_COLUMN;


public class MlfUtils {

    static LocalFolder getOpenFolder(String folderId, Account account) throws MessagingException {
        LocalStore localStore = account.getLocalStore();
        LocalFolder localFolder = localStore.getFolder(folderId);
        localFolder.open(Folder.OPEN_MODE_RO);
        return localFolder;
    }

    static void setLastSelectedFolderName(Preferences preferences,
            List<MessageReference> messages, String destFolderName) {
        try {
            MessageReference firstMsg = messages.get(0);
            Account account = preferences.getAccount(firstMsg.getAccountUuid());
            LocalFolder firstMsgFolder = MlfUtils.getOpenFolder(firstMsg.getFolderId(), account);
            firstMsgFolder.setLastSelectedFolderName(destFolderName);
        } catch (MessagingException e) {
            Timber.e(e, "Error getting folder for setLastSelectedFolderName()");
        }
    }

    static String getSenderAddressFromCursor(Cursor cursor) {
        String fromList = cursor.getString(SENDER_LIST_COLUMN);
        Address[] fromAddrs = Address.unpack(fromList);
        return (fromAddrs.length > 0) ? fromAddrs[0].getAddress() : null;
    }

    static String buildSubject(String subjectFromCursor, String emptySubject, int threadCount) {
        String subject = subjectFromCursor;
        if (TextUtils.isEmpty(subject)) {
            return emptySubject;
        } else if (threadCount > 1) {
            // If this is a thread, strip the RE/FW from the subject.  "Be like Outlook."
            return Utility.stripSubject(subject);
        }
        return subject;
    }
}
