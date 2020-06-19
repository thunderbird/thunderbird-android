package com.fsck.k9.backend.imap;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fsck.k9.backend.api.Backend;
import com.fsck.k9.backend.api.BackendStorage;
import com.fsck.k9.backend.api.SyncConfig;
import com.fsck.k9.backend.api.SyncListener;
import com.fsck.k9.mail.BodyFactory;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.power.PowerManager;
import com.fsck.k9.mail.store.imap.ImapStore;
import com.fsck.k9.mail.transport.smtp.SmtpTransport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class ImapBackend implements Backend {
    private final ImapStore imapStore;
    private final PowerManager powerManager;
    private final SmtpTransport smtpTransport;
    private final ImapSync imapSync;
    private final CommandRefreshFolderList commandRefreshFolderList;
    private final CommandSetFlag commandSetFlag;
    private final CommandMarkAllAsRead commandMarkAllAsRead;
    private final CommandExpunge commandExpunge;
    private final CommandMoveOrCopyMessages commandMoveOrCopyMessages;
    private final CommandDeleteAll commandDeleteAll;
    private final CommandSearch commandSearch;
    private final CommandFetchMessage commandFetchMessage;
    private final CommandFindByMessageId commandFindByMessageId;
    private final CommandUploadMessage commandUploadMessage;


    public ImapBackend(String accountName, BackendStorage backendStorage, ImapStore imapStore,
            PowerManager powerManager, SmtpTransport smtpTransport) {
        this.imapStore = imapStore;
        this.powerManager = powerManager;
        this.smtpTransport = smtpTransport;

        imapSync = new ImapSync(accountName, backendStorage, imapStore);
        commandSetFlag = new CommandSetFlag(imapStore);
        commandMarkAllAsRead = new CommandMarkAllAsRead(imapStore);
        commandExpunge = new CommandExpunge(imapStore);
        commandMoveOrCopyMessages = new CommandMoveOrCopyMessages(imapStore);
        commandRefreshFolderList = new CommandRefreshFolderList(backendStorage, imapStore);
        commandDeleteAll = new CommandDeleteAll(imapStore);
        commandSearch = new CommandSearch(imapStore);
        commandFetchMessage = new CommandFetchMessage(imapStore);
        commandFindByMessageId = new CommandFindByMessageId(imapStore);
        commandUploadMessage = new CommandUploadMessage(imapStore);
    }

    @Override
    public boolean getSupportsFlags() {
        return true;
    }

    @Override
    public boolean getSupportsExpunge() {
        return true;
    }

    @Override
    public boolean getSupportsMove() {
        return true;
    }

    @Override
    public boolean getSupportsCopy() {
        return true;
    }

    @Override
    public boolean getSupportsUpload() {
        return true;
    }

    @Override
    public boolean getSupportsTrashFolder() {
        return true;
    }

    @Override
    public boolean getSupportsSearchByDate() {
        return true;
    }

    @Override
    public boolean isPushCapable() {
        return true;
    }

    @Override
    public boolean isDeleteMoveToTrash() {
        return true;
    }

    @Override
    public void refreshFolderList() {
        commandRefreshFolderList.refreshFolderList();
    }

    @Override
    public void sync(@NotNull String folder, @NotNull SyncConfig syncConfig, @NotNull SyncListener listener) {
        imapSync.sync(folder, syncConfig, listener);
    }

    @Override
    public void downloadMessage(@NotNull SyncConfig syncConfig, @NotNull String folderServerId,
            @NotNull String messageServerId) throws MessagingException {
        imapSync.downloadMessage(syncConfig, folderServerId, messageServerId);
    }

    @Override
    public void setFlag(@NotNull String folderServerId, @NotNull List<String> messageServerIds, @NotNull Flag flag,
            boolean newState) throws MessagingException {
        commandSetFlag.setFlag(folderServerId, messageServerIds, flag, newState);
    }

    @Override
    public void markAllAsRead(@NotNull String folderServerId) throws MessagingException {
        commandMarkAllAsRead.markAllAsRead(folderServerId);
    }

    @Override
    public void expunge(@NotNull String folderServerId) throws MessagingException {
        commandExpunge.expunge(folderServerId);
    }

    @Override
    public void expungeMessages(@NotNull String folderServerId, @NotNull List<String> messageServerIds)
            throws MessagingException {
        commandExpunge.expungeMessages(folderServerId, messageServerIds);
    }

    @Override
    public void deleteMessages(@NotNull String folderServerId, @NotNull List<String> messageServerIds)
            throws MessagingException {
        commandSetFlag.setFlag(folderServerId, messageServerIds, Flag.DELETED, true);
    }

    @Override
    public void deleteAllMessages(@NotNull String folderServerId) throws MessagingException {
        commandDeleteAll.deleteAll(folderServerId);
    }

    @Nullable
    @Override
    public Map<String, String> moveMessages(@NotNull String sourceFolderServerId, @NotNull String targetFolderServerId,
            @NotNull List<String> messageServerIds) throws MessagingException {
        return commandMoveOrCopyMessages.moveMessages(sourceFolderServerId, targetFolderServerId, messageServerIds);
    }

    @Nullable
    @Override
    public Map<String, String> moveMessagesAndMarkAsRead(@NotNull String sourceFolderServerId,
             @NotNull String targetFolderServerId, @NotNull List<String> messageServerIds) throws MessagingException {
        Map<String, String> uidMapping = commandMoveOrCopyMessages
                .moveMessages(sourceFolderServerId, targetFolderServerId, messageServerIds);
        if (uidMapping != null) {
            setFlag(targetFolderServerId, new ArrayList<>(uidMapping.values()), Flag.SEEN, true);
        }
        return uidMapping;
    }

    @Nullable
    @Override
    public Map<String, String> copyMessages(@NotNull String sourceFolderServerId, @NotNull String targetFolderServerId,
            @NotNull List<String> messageServerIds) throws MessagingException {
        return commandMoveOrCopyMessages.copyMessages(sourceFolderServerId, targetFolderServerId, messageServerIds);
    }

    @NotNull
    @Override
    public List<String> search(@NotNull String folderServerId, @Nullable String query,
            @Nullable Set<? extends Flag> requiredFlags, @Nullable Set<? extends Flag> forbiddenFlags,
            boolean performFullTextSearch) {
        return commandSearch.search(folderServerId, query, requiredFlags, forbiddenFlags, performFullTextSearch);
    }

    @NotNull
    @Override
    public Message fetchMessage(@NotNull String folderServerId, @NotNull String messageServerId,
            @NotNull FetchProfile fetchProfile, int maxDownloadSize) {
        return commandFetchMessage.fetchMessage(folderServerId, messageServerId, fetchProfile, maxDownloadSize);
    }

    @Override
    public void fetchPart(@NotNull String folderServerId, @NotNull String messageServerId, @NotNull Part part,
            @NotNull BodyFactory bodyFactory) throws MessagingException {
        commandFetchMessage.fetchPart(folderServerId, messageServerId, part, bodyFactory);
    }

    @Nullable
    @Override
    public String findByMessageId(@NotNull String folderServerId, @NotNull String messageId) throws MessagingException {
        return commandFindByMessageId.findByMessageId(folderServerId, messageId);
    }

    @Nullable
    @Override
    public String uploadMessage(@NotNull String folderServerId, @NotNull Message message) throws MessagingException {
        return commandUploadMessage.uploadMessage(folderServerId, message);
    }

    @Override
    public void checkIncomingServerSettings() throws MessagingException {
        imapStore.checkSettings();
    }

    @Override
    public void sendMessage(@NotNull Message message) throws MessagingException {
        smtpTransport.sendMessage(message);
    }

    @Override
    public void checkOutgoingServerSettings() throws MessagingException {
        smtpTransport.checkSettings();
    }
}
