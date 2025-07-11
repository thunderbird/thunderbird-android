
package app.k9mail.legacy.message.controller;


import java.util.List;
import android.content.Context;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;
import net.thunderbird.core.android.account.LegacyAccount;


public interface MessagingListener {
    void synchronizeMailboxStarted(LegacyAccount account, long folderId);
    void synchronizeMailboxHeadersStarted(LegacyAccount account, String folderServerId);
    void synchronizeMailboxHeadersProgress(LegacyAccount account, String folderServerId, int completed, int total);
    void synchronizeMailboxHeadersFinished(LegacyAccount account, String folderServerId, int totalMessagesInMailbox,
            int numNewMessages);
    void synchronizeMailboxProgress(LegacyAccount account, long folderId, int completed, int total);
    void synchronizeMailboxNewMessage(LegacyAccount account, String folderServerId, Message message);
    void synchronizeMailboxRemovedMessage(LegacyAccount account, String folderServerId, String messageServerId);
    void synchronizeMailboxFinished(LegacyAccount account, long folderId);
    void synchronizeMailboxFailed(LegacyAccount account, long folderId, String message);

    void loadMessageRemoteFinished(LegacyAccount account, long folderId, String uid);
    void loadMessageRemoteFailed(LegacyAccount account, long folderId, String uid, Throwable t);

    void checkMailStarted(Context context, LegacyAccount account);
    void checkMailFinished(Context context, LegacyAccount account);

    void folderStatusChanged(LegacyAccount account, long folderId);

    void messageUidChanged(LegacyAccount account, long folderId, String oldUid, String newUid);

    void loadAttachmentFinished(LegacyAccount account, Message message, Part part);
    void loadAttachmentFailed(LegacyAccount account, Message message, Part part, String reason);

    void remoteSearchStarted(long folderId);
    void remoteSearchServerQueryComplete(long folderId, int numResults, int maxResults);
    void remoteSearchFinished(long folderId, int numResults, int maxResults, List<String> extraResults);
    void remoteSearchFailed(String folderServerId, String err);

    void enableProgressIndicator(boolean enable);

    void updateProgress(int progress);
}
