package com.fsck.k9.controller;


import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import android.annotation.SuppressLint;
import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.activity.ActivityListener;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.activity.setup.AccountSetupCheckSettings.CheckDirection;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Pusher;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.search.LocalSearch;
import com.fsck.k9.search.SearchAccount;


public interface IMessageController {
    void addListener(MessagingListener listener);

    void refreshListener(MessagingListener listener);

    void removeListener(MessagingListener listener);

    Set<MessagingListener> getListeners();

    Set<MessagingListener> getListeners(MessagingListener listener);

    boolean isMessageSuppressed(LocalMessage message);

    /**
     * Lists folders that are available locally and remotely. This method calls
     * listFoldersCallback for local folders before it returns, and then for
     * remote folders at some later point. If there are no local folders
     * includeRemote is forced by this method. This method should be called from
     * a Thread as it may take several seconds to list the local folders.
     *
     */
    void listFolders(Account account, boolean refreshRemote, MessagingListener listener);

    void listFoldersSynchronous(Account account, boolean refreshRemote,
            MessagingListener listener);

    void searchLocalMessages(LocalSearch search, MessagingListener listener);

    Future<?> searchRemoteMessages(String acctUuid, String folderName, String query,
            Set<Flag> requiredFlags, Set<Flag> forbiddenFlags, MessagingListener listener);

    void loadSearchResults(Account account, String folderName, List<Message> messages,
            MessagingListener listener);

    void loadMoreMessages(Account account, String folder, MessagingListener listener);

    void synchronizeMailbox(Account account, String folder, MessagingListener listener,
            Folder providedRemoteFolder);

    void handleAuthenticationFailure(Account account, boolean incoming);

    void updateMoreMessages(Folder remoteFolder, LocalFolder localFolder, Date earliestDate, int remoteStart)
            throws MessagingException, IOException;

    void markAllMessagesRead(Account account, String folder);

    void setFlag(Account account, List<Long> messageIds, Flag flag,
            boolean newState);

    void setFlagForThreads(Account account, List<Long> threadRootIds,
            Flag flag, boolean newState);

    void setFlag(Account account, String folderName, List<? extends Message> messages, Flag flag,
            boolean newState);

    void setFlag(Account account, String folderName, String uid, Flag flag,
            boolean newState);

    void clearAllPending(Account account);

    void loadMessageRemotePartial(Account account, String folder,
            String uid, MessagingListener listener);

    void loadMessageRemote(Account account, String folder,
            String uid, MessagingListener listener);

    LocalMessage loadMessage(Account account, String folderName, String uid) throws MessagingException;

    LocalMessage loadMessageMetadata(Account account, String folderName, String uid) throws MessagingException;

    /**
     * Stores the given message in the Outbox and starts a sendPendingMessages command to
     * attempt to send the message.
     */
    void sendMessage(Account account,
            Message message,
            MessagingListener listener);

    void sendPendingMessages(MessagingListener listener);

    /**
     * Attempt to send any messages that are sitting in the Outbox.
     */
    void sendPendingMessages(Account account,
            MessagingListener listener);

    void getAccountStats(Context context, Account account,
            MessagingListener listener);

    void getSearchAccountStats(SearchAccount searchAccount,
            MessagingListener listener);

    AccountStats getSearchAccountStatsSynchronous(SearchAccount searchAccount,
            MessagingListener listener);

    void getFolderUnreadMessageCount(Account account, String folderName,
            MessagingListener l);

    boolean isMoveCapable(MessageReference messageReference);

    boolean isCopyCapable(MessageReference message);

    boolean isMoveCapable(Account account);

    boolean isCopyCapable(Account account);

    void moveMessages(Account srcAccount, String srcFolder,
            List<MessageReference> messageReferences, String destFolder);

    void moveMessagesInThread(Account srcAccount, String srcFolder,
            List<MessageReference> messageReferences, String destFolder);

    void moveMessage(Account account, String srcFolder, MessageReference message,
            String destFolder);

    void copyMessages(Account srcAccount, String srcFolder,
            List<MessageReference> messageReferences, String destFolder);

    void copyMessagesInThread(Account srcAccount, String srcFolder,
            List<MessageReference> messageReferences, String destFolder);

    void copyMessage(Account account, String srcFolder, MessageReference message,
            String destFolder);

    void expunge(Account account, String folder);

    void deleteDraft(Account account, long id);

    void deleteThreads(List<MessageReference> messages);

    void deleteMessage(MessageReference message, MessagingListener listener);

    void deleteMessages(List<MessageReference> messages, MessagingListener listener);

    @SuppressLint("NewApi") // used for debugging only
    void debugClearMessagesLocally(List<MessageReference> messages);

    void emptyTrash(Account account, MessagingListener listener);

    void clearFolder(Account account, String folderName, ActivityListener listener);

    void sendAlternate(Context context, Account account, LocalMessage message);

    /**
     * Checks mail for one or multiple accounts. If account is null all accounts
     * are checked.
     */
    void checkMail(Context context, Account account,
            boolean ignoreLastCheckedTime,
            boolean useManualWakeLock,
            MessagingListener listener);

    void compact(Account account, MessagingListener ml);

    void clear(Account account, MessagingListener ml);

    void recreate(Account account, MessagingListener ml);

    boolean shouldNotifyForMessage(Account account, LocalFolder localFolder, Message message);

    void deleteAccount(Account account);

    /**
     * Save a draft message.
     *
     * @param account
     *         Account we are saving for.
     * @param message
     *         Message to save.
     *
     * @return Message representing the entry in the local store.
     */
    Message saveDraft(Account account, Message message, long existingDraftId, boolean saveRemotely);

    long getId(Message message);

    MessagingListener getCheckMailListener();

    void setCheckMailListener(MessagingListener checkMailListener);

    Collection<Pusher> getPushers();

    boolean setupPushing(Account account);

    void stopAllPushing();

    void messagesArrived(Account account, Folder remoteFolder, List<Message> messages,
            boolean flagSyncOnly);

    void systemStatusChanged();

    void cancelNotificationsForAccount(Account account);

    void cancelNotificationForMessage(Account account, MessageReference messageReference);

    void clearCertificateErrorNotifications(Account account, CheckDirection direction);

    void notifyUserIfCertificateProblem(Account account, Exception exception, boolean incoming);
}
