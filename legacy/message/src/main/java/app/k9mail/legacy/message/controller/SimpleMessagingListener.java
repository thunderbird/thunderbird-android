
package app.k9mail.legacy.message.controller;


import java.util.List;

import android.content.Context;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;
import net.thunderbird.feature.account.AccountId;


public abstract class SimpleMessagingListener implements MessagingListener {
    @Override
    public void synchronizeMailboxStarted(AccountId accountId, long folderId) {
    }

    @Override
    public void synchronizeMailboxHeadersStarted(AccountId accountId, String folderServerId) {
    }

    @Override
    public void synchronizeMailboxHeadersProgress(AccountId accountId, String folderServerId, int completed, int total) {
    }

    @Override
    public void synchronizeMailboxHeadersFinished(AccountId accountId, String folderServerId, int totalMessagesInMailbox,
            int numNewMessages) {
    }

    @Override
    public void synchronizeMailboxProgress(AccountId accountId, long folderId, int completed, int total) {
    }

    @Override
    public void synchronizeMailboxNewMessage(AccountId accountId, String folderServerId, Message message) {
    }

    @Override
    public void synchronizeMailboxRemovedMessage(AccountId accountId, String folderServerId, String messageServerId) {
    }

    @Override
    public void synchronizeMailboxFinished(AccountId accountId, long folderId) {
    }

    @Override
    public void synchronizeMailboxFailed(AccountId accountId, long folderId, String message) {
    }

    @Override
    public void loadMessageRemoteFinished(AccountId accountId, long folderId, String uid) {
    }

    @Override
    public void loadMessageRemoteFailed(AccountId accountId, long folderId, String uid, Throwable t) {
    }

    @Override
    public void checkMailStarted(Context context, AccountId accountId) {
    }

    @Override
    public void checkMailFinished(Context context, AccountId accountId) {
    }

    @Override
    public void folderStatusChanged(AccountId accountId, long folderId) {
    }

    @Override
    public void messageUidChanged(AccountId accountId, long folderId, String oldUid, String newUid) {
    }

    @Override
    public void loadAttachmentFinished(AccountId accountId, Message message, Part part) {
    }

    @Override
    public void loadAttachmentFailed(AccountId accountId, Message message, Part part, String reason) {
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
