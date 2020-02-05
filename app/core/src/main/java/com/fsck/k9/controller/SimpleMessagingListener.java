
package com.fsck.k9.controller;


import java.util.List;

import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mailstore.LocalMessage;


public abstract class SimpleMessagingListener implements MessagingListener {
    @Override
    public void accountSizeChanged(Account account, long oldSize, long newSize) {
    }

    @Override
    public void listLocalMessagesAddMessages(Account account, String folderServerId, List<LocalMessage> messages) {
    }

    @Override
    public void listLocalMessagesFinished() {
    }

    @Override
    public void synchronizeMailboxStarted(Account account, String folderServerId) {
    }

    @Override
    public void synchronizeMailboxHeadersStarted(Account account, String folderServerId) {
    }

    @Override
    public void synchronizeMailboxHeadersProgress(Account account, String folderServerId, int completed, int total) {
    }

    @Override
    public void synchronizeMailboxHeadersFinished(Account account, String folderServerId, int totalMessagesInMailbox,
            int numNewMessages) {
    }

    @Override
    public void synchronizeMailboxProgress(Account account, String folderServerId, int completed, int total) {
    }

    @Override
    public void synchronizeMailboxNewMessage(Account account, String folderServerId, Message message) {
    }

    @Override
    public void synchronizeMailboxRemovedMessage(Account account, String folderServerId, String messageServerId) {
    }

    @Override
    public void synchronizeMailboxFinished(Account account, String folderServerId) {
    }

    @Override
    public void synchronizeMailboxFailed(Account account, String folderServerId, String message) {
    }

    @Override
    public void loadMessageRemoteFinished(Account account, String folderServerId, String uid) {
    }

    @Override
    public void loadMessageRemoteFailed(Account account, String folderServerId, String uid, Throwable t) {
    }

    @Override
    public void checkMailStarted(Context context, Account account) {
    }

    @Override
    public void checkMailFinished(Context context, Account account) {
    }

    @Override
    public void folderStatusChanged(Account account, String folderServerId) {
    }

    @Override
    public void messageUidChanged(Account account, String folderServerId, String oldUid, String newUid) {
    }

    @Override
    public void loadAttachmentFinished(Account account, Message message, Part part) {
    }

    @Override
    public void loadAttachmentFailed(Account account, Message message, Part part, String reason) {
    }

    @Override
    public void remoteSearchStarted(String folder) {
    }

    @Override
    public void remoteSearchServerQueryComplete(String folderServerId, int numResults, int maxResults) {
    }

    @Override
    public void remoteSearchFinished(String folderServerId, int numResults, int maxResults, List<String> extraResults) {
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
