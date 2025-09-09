package com.fsck.k9.ui.messagelist;


import android.text.TextUtils;
import app.k9mail.legacy.di.DI;
import com.fsck.k9.helper.Utility;
import net.thunderbird.core.common.exception.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.mailstore.LocalStoreProvider;
import net.thunderbird.core.android.account.LegacyAccountDto;


public class MlfUtils {

    static LocalFolder getOpenFolder(long folderId, LegacyAccountDto account) throws MessagingException {
        LocalStore localStore = DI.get(LocalStoreProvider.class).getInstance(account);
        LocalFolder localFolder = localStore.getFolder(folderId);
        localFolder.open();
        return localFolder;
    }

    public static String buildSubject(String subjectFromCursor, String emptySubject, int threadCount) {
        if (TextUtils.isEmpty(subjectFromCursor)) {
            return emptySubject;
        } else if (threadCount > 1) {
            // If this is a thread, strip the RE/FW from the subject.  "Be like Outlook."
            return Utility.stripSubject(subjectFromCursor);
        }
        return subjectFromCursor;
    }
}
