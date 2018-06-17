package com.fsck.k9.backend.imap;


import java.util.List;
import java.util.Map;

import com.fsck.k9.backend.api.Backend;
import com.fsck.k9.backend.api.BackendStorage;
import com.fsck.k9.backend.api.FolderInfo;
import com.fsck.k9.backend.api.SyncConfig;
import com.fsck.k9.backend.api.SyncListener;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.imap.ImapStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class ImapBackend implements Backend {
    private final ImapSync imapSync;
    private final CommandGetFolders commandGetFolders;
    private final CommandSetFlag commandSetFlag;
    private final CommandMarkAllAsRead commandMarkAllAsRead;
    private final CommandExpunge commandExpunge;
    private final CommandMoveOrCopyMessages commandMoveOrCopyMessages;


    public ImapBackend(String accountName, BackendStorage backendStorage, ImapStore imapStore) {
        imapSync = new ImapSync(accountName, backendStorage, imapStore);
        commandSetFlag = new CommandSetFlag(imapStore);
        commandMarkAllAsRead = new CommandMarkAllAsRead(imapStore);
        commandExpunge = new CommandExpunge(imapStore);
        commandMoveOrCopyMessages = new CommandMoveOrCopyMessages(imapStore);
        commandGetFolders = new CommandGetFolders(imapStore);
    }

    @Override
    public boolean getSupportsSeenFlag() {
        return true;
    }

    @Override
    public boolean getSupportsExpunge() {
        return true;
    }

    @NotNull
    @Override
    public List<FolderInfo> getFolders(boolean forceListAll) {
        return commandGetFolders.getFolders(forceListAll);
    }

    @Override
    public void sync(@NotNull String folder, @NotNull SyncConfig syncConfig, @NotNull SyncListener listener,
            Folder providedRemoteFolder) {
        imapSync.sync(folder, syncConfig, listener, providedRemoteFolder);
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

    @Nullable
    @Override
    public Map<String, String> moveMessages(@NotNull String sourceFolderServerId, @NotNull String targetFolderServerId,
            @NotNull List<String> messageServerIds) throws MessagingException {
        return commandMoveOrCopyMessages.moveMessages(sourceFolderServerId, targetFolderServerId, messageServerIds);
    }

    @Nullable
    @Override
    public Map<String, String> copyMessages(@NotNull String sourceFolderServerId, @NotNull String targetFolderServerId,
            @NotNull List<String> messageServerIds) throws MessagingException {
        return commandMoveOrCopyMessages.copyMessages(sourceFolderServerId, targetFolderServerId, messageServerIds);
    }
}
