
package com.fsck.k9.controller;

import android.content.Context;
import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.BaseAccount;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;

import java.util.List;

/**
 * Defines the interface that MessagingController will use to callback to requesters. This class
 * is defined as non-abstract so that someone who wants to receive only a few messages can
 * do so without implementing the entire interface. It is highly recommended that users of
 * this interface use the @Override annotation in their implementations to avoid being caught by
 * changes in this class.
 */
public class MessagingListener
{
    public void searchStats(AccountStats stats) {}

    public void accountStatusChanged(BaseAccount account, AccountStats stats)
    {
    }

    public void accountSizeChanged(Account account, long oldSize, long newSize)
    {
    }

    public void listFoldersStarted(Account account)
    {
    }

    public void listFolders(Account account, Folder[] folders)
    {
    }

    public void listFoldersFailed(Account account, String message)
    {
    }

    public void listFoldersFinished(Account account)
    {
    }

    public void listLocalMessagesStarted(Account account, String folder)
    {
    }

    public void listLocalMessages(Account account, String folder, Message[] messages)
    {
    }

    public void listLocalMessagesAddMessages(Account account, String folder, List<Message> messages)
    {
    }

    public void listLocalMessagesUpdateMessage(Account account, String folder, Message message)
    {
    }

    public void listLocalMessagesRemoveMessage(Account account, String folder, Message message)
    {
    }

    public void listLocalMessagesFailed(Account account, String folder, String message)
    {
    }

    public void listLocalMessagesFinished(Account account, String folder)
    {
    }

    public void synchronizeMailboxStarted(Account account, String folder)
    {
    }

    public void synchronizeMailboxProgress(Account account, String folder, int completed, int total)
    {}

    public void synchronizeMailboxNewMessage(Account account, String folder, Message message)
    {
    }

    public void synchronizeMailboxAddOrUpdateMessage(Account account, String folder, Message message)
    {
    }

    public void synchronizeMailboxRemovedMessage(Account account, String folder,Message message)
    {
    }

    public void synchronizeMailboxFinished(Account account, String folder,
                                           int totalMessagesInMailbox, int numNewMessages)
    {
    }

    public void synchronizeMailboxFailed(Account account, String folder,
                                         String message)
    {
    }

    public void loadMessageForViewStarted(Account account, String folder, String uid)
    {
    }

    public void loadMessageForViewHeadersAvailable(Account account, String folder, String uid,
            Message message)
    {
    }

    public void loadMessageForViewBodyAvailable(Account account, String folder, String uid,
            Message message)
    {
    }

    public void loadMessageForViewFinished(Account account, String folder, String uid,
                                           Message message)
    {
    }

    public void loadMessageForViewFailed(Account account, String folder, String uid, Throwable t)
    {
    }

    public void checkMailStarted(Context context, Account account)
    {
    }

    public void checkMailFinished(Context context, Account account)
    {
    }

    public void checkMailFailed(Context context, Account account, String reason)
    {
    }

    public void sendPendingMessagesStarted(Account account)
    {
    }

    public void sendPendingMessagesCompleted(Account account)
    {
    }

    public void sendPendingMessagesFailed(Account account)
    {
    }

    public void messageDeleted(Account account, String folder, Message message)
    {

    }
    public void emptyTrashCompleted(Account account)
    {
    }

    public void folderStatusChanged(Account account, String folderName, int unreadMessageCount)
    {
    }

    public void folderStatusChanged(Account account, String folderName)
    {
    }

    public void messageUidChanged(Account account, String folder, String oldUid, String newUid)
    {

    }

    public void setPushActive(Account account, String folderName, boolean enabled)
    {

    }

    public void loadAttachmentStarted(
        Account account,
        Message message,
        Part part,
        Object tag,
        boolean requiresDownload)
    {
    }

    public void loadAttachmentFinished(
        Account account,
        Message message,
        Part part,
        Object tag)
    {
    }

    public void loadAttachmentFailed(
        Account account,
        Message message,
        Part part,
        Object tag,
        String reason)
    {
    }

    public void pendingCommandsProcessing(Account account) {}
    public void pendingCommandsFinished(Account account) {}
    public void pendingCommandStarted(Account account, String commandTitle)
    {}

    public void pendingCommandCompleted(Account account, String commandTitle)
    {}

    /**
     * General notification messages subclasses can override to be notified that the controller
     * has completed a command. This is useful for turning off progress indicators that may have
     * been left over from previous commands.
     * @param moreCommandsToRun True if the controller will continue on to another command
     * immediately.
     */
    public void controllerCommandCompleted(boolean moreCommandsToRun)
    {

    }
}
