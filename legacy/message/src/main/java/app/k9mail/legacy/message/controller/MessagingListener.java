
package app.k9mail.legacy.message.controller;


import java.util.List;
import android.content.Context;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;
import net.thunderbird.feature.account.AccountId;


public interface MessagingListener {
    void synchronizeMailboxStarted(AccountId accountId, long folderId);
    void synchronizeMailboxHeadersStarted(AccountId accountId, String folderServerId);
    void synchronizeMailboxHeadersProgress(AccountId accountId, String folderServerId, int completed, int total);
    void synchronizeMailboxHeadersFinished(AccountId accountId, String folderServerId, int totalMessagesInMailbox,
            int numNewMessages);
    void synchronizeMailboxProgress(AccountId accountId, long folderId, int completed, int total);
    void synchronizeMailboxNewMessage(AccountId accountId, String folderServerId, Message message);
    void synchronizeMailboxRemovedMessage(AccountId accountId, String folderServerId, String messageServerId);
    void synchronizeMailboxFinished(AccountId accountId, long folderId);
    void synchronizeMailboxFailed(AccountId accountId, long folderId, String message);

    void loadMessageRemoteFinished(AccountId accountId, long folderId, String uid);
    void loadMessageRemoteFailed(AccountId accountId, long folderId, String uid, Throwable t);

    void checkMailStarted(Context context, AccountId accountId);
    void checkMailFinished(Context context, AccountId accountId);

    void folderStatusChanged(AccountId accountId, long folderId);

    void messageUidChanged(AccountId accountId, long folderId, String oldUid, String newUid);

    void loadAttachmentFinished(AccountId accountId, Message message, Part part);
    void loadAttachmentFailed(AccountId accountId, Message message, Part part, String reason);

    void remoteSearchStarted(long folderId);
    void remoteSearchServerQueryComplete(long folderId, int numResults, int maxResults);
    void remoteSearchFinished(long folderId, int numResults, int maxResults, List<String> extraResults);
    void remoteSearchFailed(String folderServerId, String err);

    void enableProgressIndicator(boolean enable);

    void updateProgress(int progress);
}
