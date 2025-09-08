
package app.k9mail.legacy.message.controller;


import java.util.List;

import android.content.Context;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;
import net.thunderbird.core.android.account.LegacyAccountDto;


public abstract class SimpleMessagingListener implements MessagingListener {
    @Override
    public void synchronizeMailboxStarted(LegacyAccountDto account, long folderId) {
    }

    @Override
    public void synchronizeMailboxHeadersStarted(LegacyAccountDto account, String folderServerId) {
    }

    @Override
    public void synchronizeMailboxHeadersProgress(LegacyAccountDto account, String folderServerId, int completed, int total) {
    }

    @Override
    public void synchronizeMailboxHeadersFinished(LegacyAccountDto account, String folderServerId, int totalMessagesInMailbox,
            int numNewMessages) {
    }

    @Override
    public void synchronizeMailboxProgress(LegacyAccountDto account, long folderId, int completed, int total) {
    }

    @Override
    public void synchronizeMailboxNewMessage(LegacyAccountDto account, String folderServerId, Message message) {
    }

    @Override
    public void synchronizeMailboxRemovedMessage(LegacyAccountDto account, String folderServerId, String messageServerId) {
    }

    @Override
    public void synchronizeMailboxFinished(LegacyAccountDto account, long folderId) {
    }

    @Override
    public void synchronizeMailboxFailed(LegacyAccountDto account, long folderId, String message) {
    }

    @Override
    public void loadMessageRemoteFinished(LegacyAccountDto account, long folderId, String uid) {
    }

    @Override
    public void loadMessageRemoteFailed(LegacyAccountDto account, long folderId, String uid, Throwable t) {
    }

    @Override
    public void checkMailStarted(Context context, LegacyAccountDto account) {
    }

    @Override
    public void checkMailFinished(Context context, LegacyAccountDto account) {
    }

    @Override
    public void folderStatusChanged(LegacyAccountDto account, long folderId) {
    }

    @Override
    public void messageUidChanged(LegacyAccountDto account, long folderId, String oldUid, String newUid) {
    }

    @Override
    public void loadAttachmentFinished(LegacyAccountDto account, Message message, Part part) {
    }

    @Override
    public void loadAttachmentFailed(LegacyAccountDto account, Message message, Part part, String reason) {
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
