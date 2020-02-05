
package com.fsck.k9.controller;


import java.util.List;

import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mailstore.LocalMessage;


public interface MessagingListener {
    void accountSizeChanged(Account account, long oldSize, long newSize);

    void listLocalMessagesAddMessages(Account account, String folderServerId, List<LocalMessage> messages);
    void listLocalMessagesFinished();

    void synchronizeMailboxStarted(Account account, String folderServerId);
    void synchronizeMailboxHeadersStarted(Account account, String folderServerId);
    void synchronizeMailboxHeadersProgress(Account account, String folderServerId, int completed, int total);
    void synchronizeMailboxHeadersFinished(Account account, String folderServerId, int totalMessagesInMailbox,
            int numNewMessages);
    void synchronizeMailboxProgress(Account account, String folderServerId, int completed, int total);
    void synchronizeMailboxNewMessage(Account account, String folderServerId, Message message);
    void synchronizeMailboxRemovedMessage(Account account, String folderServerId, String messageServerId);
    void synchronizeMailboxFinished(Account account, String folderServerId);
    void synchronizeMailboxFailed(Account account, String folderServerId, String message);

    void loadMessageRemoteFinished(Account account, String folderServerId, String uid);
    void loadMessageRemoteFailed(Account account, String folderServerId, String uid, Throwable t);

    void checkMailStarted(Context context, Account account);
    void checkMailFinished(Context context, Account account);

    void folderStatusChanged(Account account, String folderServerId);

    void messageUidChanged(Account account, String folderServerId, String oldUid, String newUid);

    void loadAttachmentFinished(Account account, Message message, Part part);
    void loadAttachmentFailed(Account account, Message message, Part part, String reason);

    void remoteSearchStarted(String folder);
    void remoteSearchServerQueryComplete(String folderServerId, int numResults, int maxResults);
    void remoteSearchFinished(String folderServerId, int numResults, int maxResults, List<String> extraResults);
    void remoteSearchFailed(String folderServerId, String err);

    void enableProgressIndicator(boolean enable);

    void updateProgress(int progress);
}
