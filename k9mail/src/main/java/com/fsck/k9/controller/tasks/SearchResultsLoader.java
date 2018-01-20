package com.fsck.k9.controller.tasks;


import java.util.Collections;
import java.util.List;

import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;


public class SearchResultsLoader {
    public void load(
            List<Message> messages, LocalFolder localFolder, Folder remoteFolder,
            MessagingListener listener) throws MessagingException {
        final FetchProfile header = new FetchProfile();
        header.add(FetchProfile.Item.FLAGS);
        header.add(FetchProfile.Item.ENVELOPE);
        final FetchProfile structure = new FetchProfile();
        structure.add(FetchProfile.Item.STRUCTURE);

        int i = 0;
        for (Message message : messages) {
            i++;
            LocalMessage localMsg = localFolder.getMessage(message.getUid());

            if (localMsg == null) {
                remoteFolder.fetch(Collections.singletonList(message), header, null);
                //fun fact: ImapFolder.fetch can't handle getting STRUCTURE at same time as headers
                remoteFolder.fetch(Collections.singletonList(message), structure, null);
                localFolder.appendMessages(Collections.singletonList(message));
                localMsg = localFolder.getMessage(message.getUid());
            }
        }
    }
}
