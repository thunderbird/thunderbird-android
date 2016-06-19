package com.fsck.k9.mail;

import android.support.annotation.Nullable;

import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;

import java.util.ArrayList;
import java.util.List;

public class MessageUtils {
    public static String[] getUidsFromMessages(List<? extends Message> messages) {
        String[] uids = new String[messages.size()];
        for (int i = 0, end = uids.length; i < end; i++) {
            uids[i] = messages.get(i).getUid();
        }
        return uids;
    }

    public static List<Message> getRemoteMessagesFromUids(String localUidPrefix, Folder folder, String[] uids, int start) throws MessagingException {
        return getRemoteMessagesFromUids(localUidPrefix, folder, uids, start, uids.length);
    }

    public static List<Message> getRemoteMessagesFromUids(String localUidPrefix, Folder folder, String[] uids, int start, int end) throws MessagingException {
        List<Message> messages = new ArrayList<Message>();
        for (int i = start; i < end; i++) {
            String uid = uids[i];
            if (!uid.startsWith(localUidPrefix)) {
                messages.add(folder.getMessage(uid));
            }
        }
        return messages;
    }

    @Nullable
    public static Message getRemoteMessageFromUid(String localUidPrefix, Folder folder, String uid) throws MessagingException {
        if (!uid.startsWith(localUidPrefix)) {
            return folder.getMessage(uid);
        }
        return null;
    }
}
