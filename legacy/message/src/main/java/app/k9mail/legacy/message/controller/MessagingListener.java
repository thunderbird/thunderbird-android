
package app.k9mail.legacy.message.controller;


import java.util.List;

import android.content.Context;

import app.k9mail.legacy.account.Account;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;


public interface MessagingListener {
    void synchronizeMailboxStarted(Account account, long folderId);
    void synchronizeMailboxHeadersStarted(Account account, String folderServerId);
    void synchronizeMailboxHeadersProgress(Account account, String folderServerId, int completed, int total);
    void synchronizeMailboxHeadersFinished(Account account, String folderServerId, int totalMessagesInMailbox,
            int numNewMessages);
    void synchronizeMailboxProgress(Account account, long folderId, int completed, int total);
    void synchronizeMailboxNewMessage(Account account, String folderServerId, Message message);
    void synchronizeMailboxRemovedMessage(Account account, String folderServerId, String messageServerId);
    void synchronizeMailboxFinished(Account account, long folderId);
    void synchronizeMailboxFailed(Account account, long folderId, String message);

    void loadMessageRemoteFinished(Account account, long folderId, String uid);
    void loadMessageRemoteFailed(Account account, long folderId, String uid, Throwable t);

    void checkMailStarted(Context context, Account account);
    void checkMailFinished(Context context, Account account);

    void folderStatusChanged(Account account, long folderId);

    void messageUidChanged(Account account, long folderId, String oldUid, String newUid);

    void loadAttachmentFinished(Account account, Message message, Part part);
    void loadAttachmentFailed(Account account, Message message, Part part, String reason);

    void remoteSearchStarted(long folderId);
    void remoteSearchServerQueryComplete(long folderId, int numResults, int maxResults);
    void remoteSearchFinished(long folderId, int numResults, int maxResults, List<String> extraResults);
    void remoteSearchFailed(String folderServerId, String err);

    void enableProgressIndicator(boolean enable);

    void updateProgress(int progress);
}
