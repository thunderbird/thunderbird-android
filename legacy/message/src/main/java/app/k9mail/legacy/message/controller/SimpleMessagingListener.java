
package app.k9mail.legacy.message.controller;


import java.util.List;

import android.content.Context;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;
import net.thunderbird.core.android.account.LegacyAccount;


public abstract class SimpleMessagingListener implements MessagingListener {
    @Override
    public void synchronizeMailboxStarted(LegacyAccount account, long folderId) {
    }

    @Override
    public void synchronizeMailboxHeadersStarted(LegacyAccount account, String folderServerId) {
    }

    @Override
    public void synchronizeMailboxHeadersProgress(LegacyAccount account, String folderServerId, int completed, int total) {
    }

    @Override
    public void synchronizeMailboxHeadersFinished(LegacyAccount account, String folderServerId, int totalMessagesInMailbox,
            int numNewMessages) {
    }

    @Override
    public void synchronizeMailboxProgress(LegacyAccount account, long folderId, int completed, int total) {
    }

    @Override
    public void synchronizeMailboxNewMessage(LegacyAccount account, String folderServerId, Message message) {
    }

    @Override
    public void synchronizeMailboxRemovedMessage(LegacyAccount account, String folderServerId, String messageServerId) {
    }

    @Override
    public void synchronizeMailboxFinished(LegacyAccount account, long folderId) {
    }

    @Override
    public void synchronizeMailboxFailed(LegacyAccount account, long folderId, String message) {
    }

    @Override
    public void loadMessageRemoteFinished(LegacyAccount account, long folderId, String uid) {
    }

    @Override
    public void loadMessageRemoteFailed(LegacyAccount account, long folderId, String uid, Throwable t) {
    }

    @Override
    public void checkMailStarted(Context context, LegacyAccount account) {
    }

    @Override
    public void checkMailFinished(Context context, LegacyAccount account) {
    }

    @Override
    public void folderStatusChanged(LegacyAccount account, long folderId) {
    }

    @Override
    public void messageUidChanged(LegacyAccount account, long folderId, String oldUid, String newUid) {
    }

    @Override
    public void loadAttachmentFinished(LegacyAccount account, Message message, Part part) {
    }

    @Override
    public void loadAttachmentFailed(LegacyAccount account, Message message, Part part, String reason) {
    }

    @Override
    public void remoteSearchStarted(long folderId) {
    }

    @Override
    public void remoteSearchServerQueryComplete(long folderId, int numResults, int maxResults) {
    }

    @Override
    public void remoteSearchFinished(long folderId, int numResults, int maxResults, List<String> extraResults) {
    }

    @Override
    public void remoteSearchFailed(String folderServerId, String err) {
    }

    @Override
    public void enableProgressIndicator(boolean enable) {
    }

    @Override
    public void updateProgress(int progress) {

    }
}
