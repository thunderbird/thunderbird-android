
package app.k9mail.legacy.message.controller;


import java.util.List;
import android.content.Context;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;
import net.thunderbird.core.android.account.LegacyAccountDto;


public interface MessagingListener {
    void synchronizeMailboxStarted(LegacyAccountDto account, long folderId);
    void synchronizeMailboxHeadersStarted(LegacyAccountDto account, String folderServerId);
    void synchronizeMailboxHeadersProgress(LegacyAccountDto account, String folderServerId, int completed, int total);
    void synchronizeMailboxHeadersFinished(LegacyAccountDto account, String folderServerId, int totalMessagesInMailbox,
            int numNewMessages);
    void synchronizeMailboxProgress(LegacyAccountDto account, long folderId, int completed, int total);
    void synchronizeMailboxNewMessage(LegacyAccountDto account, String folderServerId, Message message);
    void synchronizeMailboxRemovedMessage(LegacyAccountDto account, String folderServerId, String messageServerId);
    void synchronizeMailboxFinished(LegacyAccountDto account, long folderId);
    void synchronizeMailboxFailed(LegacyAccountDto account, long folderId, String message);

    void loadMessageRemoteFinished(LegacyAccountDto account, long folderId, String uid);
    void loadMessageRemoteFailed(LegacyAccountDto account, long folderId, String uid, Throwable t);

    void checkMailStarted(Context context, LegacyAccountDto account);
    void checkMailFinished(Context context, LegacyAccountDto account);

    void folderStatusChanged(LegacyAccountDto account, long folderId);

    void messageUidChanged(LegacyAccountDto account, long folderId, String oldUid, String newUid);

    void loadAttachmentFinished(LegacyAccountDto account, Message message, Part part);
    void loadAttachmentFailed(LegacyAccountDto account, Message message, Part part, String reason);

    void remoteSearchStarted(long folderId);
    void remoteSearchServerQueryComplete(long folderId, int numResults, int maxResults);
    void remoteSearchFinished(long folderId, int numResults, int maxResults, List<String> extraResults);
    void remoteSearchFailed(String folderServerId, String err);

    void enableProgressIndicator(boolean enable);

    void updateProgress(int progress);
}
