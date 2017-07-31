package com.fsck.k9.controller;


import java.util.Date;
import java.util.List;

import com.fsck.k9.Account;
import com.fsck.k9.Account.Expunge;
import com.fsck.k9.K9RobolectricTestRunner;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mail.store.RemoteStore;
import com.fsck.k9.mail.store.imap.ImapFolder;
import com.fsck.k9.mail.store.imap.QresyncParamResponse;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalFolder.MoreMessages;
import com.fsck.k9.mailstore.LocalMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static com.fsck.k9.mail.Folder.OPEN_MODE_RW;
import static edu.emory.mathcs.backport.java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
@RunWith(K9RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ImapSyncInteractorTest {

    private static final String FOLDER_NAME = "Folder";
    private static final long CACHED_UID_VALIDITY = 1L;
    private static final long CURRENT_UID_VALIDITY = 2L;
    private static final long CACHED_HIGHEST_MOD_SEQ = 3L;
    private static final long CURRENT_HIGHEST_MOD_SEQ = 4L;
    private static final List<String> LOCAL_MESSAGE_UIDS = singletonList("1");

    private ImapSyncInteractor syncInteractor;
    private Account account;
    private LocalFolder localFolder;
    private ImapFolder imapFolder;
    private MessagingListener listener;
    private MessagingController controller;
    private FlagSyncHelper flagSyncHelper;
    private MessageDownloader messageDownloader;
    private SyncHelper syncHelper;

    @Before
    public void setUp() throws Exception {
        account = mock(Account.class);
        localFolder = mock(LocalFolder.class);
        imapFolder = mock(ImapFolder.class);
        listener = mock(MessagingListener.class);
        controller = mock(MessagingController.class);
        flagSyncHelper = mock(FlagSyncHelper.class);
        messageDownloader = mock(MessageDownloader.class);
        syncHelper = mock(SyncHelper.class);

        configureLocalFolder();
        configureRemoteFolder();

        when(syncHelper.verifyOrCreateRemoteSpecialFolder(account, FOLDER_NAME, imapFolder, listener, controller))
                .thenReturn(true);
        when(syncHelper.getOpenedLocalFolder(account, FOLDER_NAME)).thenReturn(localFolder);
        when(controller.getListeners(listener)).thenReturn(singleton(listener));
        Store remoteStore = mock(RemoteStore.class);
        when(account.getRemoteStore()).thenReturn(remoteStore);
        when(remoteStore.getFolder(FOLDER_NAME)).thenReturn((Folder) imapFolder);

        syncInteractor = new ImapSyncInteractor(account, FOLDER_NAME, listener, controller);
    }

    @Test
    public void performSync_withProcessPendingCommandsSynchronousThrowingException_shouldFinishWithError()
            throws Exception {
        doThrow(MessagingException.class).when(controller).processPendingCommandsSynchronous(account);

        syncInteractor.performSync(flagSyncHelper, messageDownloader, syncHelper);

        verify(listener).synchronizeMailboxFailed(eq(account), eq(FOLDER_NAME), anyString());
    }
    @Test
    public void performSync_withImapFolder_shouldFinishWithoutError() throws Exception {
        syncInteractor.performSync(flagSyncHelper, messageDownloader, syncHelper);

        verify(listener).synchronizeMailboxFinished(eq(account), eq(FOLDER_NAME), anyInt(), anyInt());
    }

    @Test
    public void performSync_withNonImapFolder_shouldFinishWithError() throws Exception {
        Store remoteStore = mock(RemoteStore.class);
        when(account.getRemoteStore()).thenReturn(remoteStore);
        when(remoteStore.getFolder(FOLDER_NAME)).thenReturn(mock(Folder.class));

        syncInteractor.performSync(flagSyncHelper, messageDownloader, syncHelper);

        verify(listener).synchronizeMailboxFailed(eq(account), eq(FOLDER_NAME), anyString());
    }

    @Test
    public void performSync_withRemoteFolderCreationFailed_shouldExitWithoutOpeningRemoteFolder() throws Exception {
        when(syncHelper.verifyOrCreateRemoteSpecialFolder(account, FOLDER_NAME, imapFolder, listener, controller))
                .thenReturn(false);

        syncInteractor.performSync(flagSyncHelper, messageDownloader, syncHelper);

        verify(imapFolder, never()).openUsingQresyncParam(anyInt(), anyLong(), anyLong());
    }

    @Test
    public void performSync_withValidCachedUidValidityAndHighestModSeq_shouldOpenFolderUsingQresyncParams()
            throws Exception {
        syncInteractor.performSync(flagSyncHelper, messageDownloader, syncHelper);

        verify(imapFolder).openUsingQresyncParam(OPEN_MODE_RW, CACHED_UID_VALIDITY, CACHED_HIGHEST_MOD_SEQ);
    }
    @Test
    public void performSync_withoutCachedUidValidity_shouldOpenImapFolderNormally() throws Exception {
        when(localFolder.hasCachedUidValidity()).thenReturn(false);

        syncInteractor.performSync(flagSyncHelper, messageDownloader, syncHelper);

        verify(imapFolder).open(OPEN_MODE_RW);
    }

    @Test
    public void performSync_withInvalidCachedHighestModSeq_shouldOpenImapFolderNormally() throws Exception {
        when(localFolder.isCachedHighestModSeqValid()).thenReturn(false);

        syncInteractor.performSync(flagSyncHelper, messageDownloader, syncHelper);

        verify(imapFolder).open(OPEN_MODE_RW);
    }

    @Test
    public void performSync_withQresyncEnabledAndAccountSetToExpungeOnPoll_shouldExpungeRemoteFolder()
            throws Exception {
        when(imapFolder.openUsingQresyncParam(OPEN_MODE_RW, CACHED_UID_VALIDITY, CACHED_HIGHEST_MOD_SEQ))
                .thenReturn(mock(QresyncParamResponse.class));
        when(account.getExpungePolicy()).thenReturn(Expunge.EXPUNGE_ON_POLL);

        syncInteractor.performSync(flagSyncHelper, messageDownloader, syncHelper);

        verify(imapFolder).expungeUsingQresync();
    }

    @Test
    public void performSync_withoutQresyncEnabledAndAccountSetToExpungeOnPoll_shouldExpungeRemoteFolder()
            throws Exception {
        when(imapFolder.openUsingQresyncParam(OPEN_MODE_RW, CACHED_UID_VALIDITY, CACHED_HIGHEST_MOD_SEQ))
                .thenReturn(null);
        when(account.getExpungePolicy()).thenReturn(Expunge.EXPUNGE_ON_POLL);

        syncInteractor.performSync(flagSyncHelper, messageDownloader, syncHelper);

        verify(imapFolder).expunge();
    }

    @Test
    public void performSync_withAccountNotSetToExpungeOnPoll_shouldNotExpungeRemoteFolder() throws Exception {
        when(account.getExpungePolicy()).thenReturn(Expunge.EXPUNGE_MANUALLY);

        syncInteractor.performSync(flagSyncHelper, messageDownloader, syncHelper);

        verify(imapFolder, never()).expunge();
        verify(imapFolder, never()).expungeUsingQresync();
    }

    @Test
    public void performSync_withNegativeMessageCount_shouldFinishWithError() throws Exception {
        when(imapFolder.getMessageCount()).thenReturn(-1);

        syncInteractor.performSync(flagSyncHelper, messageDownloader, syncHelper);

        verify(listener).synchronizeMailboxFailed(eq(account), eq(FOLDER_NAME), anyString());
    }

    @Test
    public void performSync_withDifferentUidValidity_shouldDeleteAllLocallyStoredMessages() throws Exception {
        when(imapFolder.getUidValidity()).thenReturn(CURRENT_UID_VALIDITY);
        List<LocalMessage> localMessages = configureLocalFolderWithSingleMessage();

        syncInteractor.performSync(flagSyncHelper, messageDownloader, syncHelper);

        verify(localFolder).destroyMessages(localMessages);
    }

    @Test
    public void performSync_withDifferentUidValidity_shouldNotifyListenersOfDeletion() throws Exception {
        when(imapFolder.getUidValidity()).thenReturn(CURRENT_UID_VALIDITY);
        List<LocalMessage> localMessages = configureLocalFolderWithSingleMessage();

        syncInteractor.performSync(flagSyncHelper, messageDownloader, syncHelper);

        verify(listener).synchronizeMailboxRemovedMessage(account, FOLDER_NAME, localMessages.get(0));
    }

    @Test
    public void performSync_withDifferentUidValidity_shouldClearHighestModSeq() throws Exception {
        when(imapFolder.getUidValidity()).thenReturn(CURRENT_UID_VALIDITY);
        configureLocalFolderWithSingleMessage();

        syncInteractor.performSync(flagSyncHelper, messageDownloader, syncHelper);

        verify(localFolder, atLeastOnce()).invalidateHighestModSeq();
    }

    @Test
    public void performSync_withEqualUidValidity_shouldNotDeleteAllLocallyStoredMessages() throws Exception {
        when(imapFolder.getUidValidity()).thenReturn(CACHED_UID_VALIDITY);
        List<LocalMessage> localMessages = configureLocalFolderWithSingleMessage();

        syncInteractor.performSync(flagSyncHelper, messageDownloader, syncHelper);

        verify(localFolder, never()).destroyMessages(localMessages);
    }

    @Test
    public void performSync_withoutCachedUidValidity_shouldNotDeleteAllLocallyStoredMessages() throws Exception {
        when(localFolder.hasCachedUidValidity()).thenReturn(false);
        when(imapFolder.getUidValidity()).thenReturn(CURRENT_UID_VALIDITY);
        List<LocalMessage> localMessages = configureLocalFolderWithSingleMessage();

        syncInteractor.performSync(flagSyncHelper, messageDownloader, syncHelper);

        verify(localFolder, never()).destroyMessages(localMessages);
    }

    @Test
    public void performSync_shouldUpdateCachedUidValidity() throws Exception {
        when(imapFolder.getUidValidity()).thenReturn(CURRENT_UID_VALIDITY);

        syncInteractor.performSync(flagSyncHelper, messageDownloader, syncHelper);

        verify(localFolder).setUidValidity(CURRENT_UID_VALIDITY);
    }

    @Test
    public void performSync_withFolderSupportingModSeqs_shouldUpdateCachedHighestModSeq() throws Exception {
        when(imapFolder.supportsModSeq()).thenReturn(true);

        syncInteractor.performSync(flagSyncHelper, messageDownloader, syncHelper);

        verify(localFolder).setHighestModSeq(CURRENT_HIGHEST_MOD_SEQ);
    }

    @Test
    public void performSync_withFolderNotSupportingModSeqs_shouldInvalidateCachedHighestModSeq() throws Exception {
        when(imapFolder.supportsModSeq()).thenReturn(false);

        syncInteractor.performSync(flagSyncHelper, messageDownloader, syncHelper);

        verify(localFolder).invalidateHighestModSeq();
    }

    @Test
    public void performSync_afterCompletion_shouldCloseFolders() throws Exception {
        syncInteractor.performSync(flagSyncHelper, messageDownloader, syncHelper);

        verify(localFolder).close();
        verify(imapFolder).close();
    }

    @Test
    public void syncRemoteDeletions_withNonEmptyMessageUidList_shouldDeleteLocalCopiesOfMessages() throws Exception {
        List<LocalMessage> localMessages = configureLocalFolderWithSingleMessage();

        syncInteractor.syncRemoteDeletions(LOCAL_MESSAGE_UIDS, syncHelper);

        verify(localFolder).destroyMessages(localMessages);
    }

    @Test
    public void syncRemoteDeletions_withNonEmptyMessageUidList_shouldNotifyListeners() throws Exception {
        List<LocalMessage> localMessages = configureLocalFolderWithSingleMessage();

        syncInteractor.syncRemoteDeletions(LOCAL_MESSAGE_UIDS, syncHelper);

        verify(listener).synchronizeMailboxRemovedMessage(account, FOLDER_NAME, localMessages.get(0));
    }

    @Test
    public void syncRemoteDeletions_withNonEmptyMessageUidListAndRemoteStartAsOne_shouldNotCheckForMoreMessages()
            throws Exception {
        configureLocalFolderWithSingleMessage();
        when(syncHelper.getRemoteStart(localFolder, imapFolder)).thenReturn(1);

        syncInteractor.syncRemoteDeletions(LOCAL_MESSAGE_UIDS, syncHelper);

        verify(imapFolder, never()).areMoreMessagesAvailable(anyInt(), any(Date.class));
        verify(localFolder).setMoreMessages(MoreMessages.FALSE);
    }

    @Test
    public void syncRemoteDeletions_withNonEmptyMessageUidListAndMoreMessagesAvailable_shouldSetMoreMessagesAsTrue()
            throws Exception {
        configureLocalFolderWithSingleMessage();
        when(imapFolder.areMoreMessagesAvailable(anyInt(), any(Date.class))).thenReturn(true);

        syncInteractor.syncRemoteDeletions(LOCAL_MESSAGE_UIDS, syncHelper);

        verify(localFolder).setMoreMessages(MoreMessages.TRUE);
    }

    @Test
    public void syncRemoteDeletions_withNonEmptyMessageUidListAndNoMoreMessagesAvailable_shouldSetMoreMessagesAsFalse()
            throws Exception {
        configureLocalFolderWithSingleMessage();
        when(imapFolder.areMoreMessagesAvailable(anyInt(), any(Date.class))).thenReturn(false);

        syncInteractor.syncRemoteDeletions(LOCAL_MESSAGE_UIDS, syncHelper);

        verify(localFolder).setMoreMessages(MoreMessages.FALSE);
    }

    @Test
    public void syncRemoteDeletions_withEmptyMessageUidList_shouldNotCheckForMoreMessages() throws Exception {
        syncInteractor.syncRemoteDeletions(emptyList(), syncHelper);

        verify(imapFolder, never()).areMoreMessagesAvailable(anyInt(), any(Date.class));
    }

    private void configureLocalFolder() throws Exception {
        when(localFolder.getName()).thenReturn(FOLDER_NAME);
        when(localFolder.hasCachedUidValidity()).thenReturn(true);
        when(localFolder.getUidValidity()).thenReturn(CACHED_UID_VALIDITY);
        when(localFolder.isCachedHighestModSeqValid()).thenReturn(true);
        when(localFolder.getHighestModSeq()).thenReturn(CACHED_HIGHEST_MOD_SEQ);
    }

    private void configureRemoteFolder() throws Exception {
        when(imapFolder.getName()).thenReturn(FOLDER_NAME);
        when(imapFolder.getUidValidity()).thenReturn(CACHED_UID_VALIDITY);
        when(imapFolder.getHighestModSeq()).thenReturn(CURRENT_HIGHEST_MOD_SEQ);
    }

    private List<LocalMessage> configureLocalFolderWithSingleMessage() throws Exception {
        when(localFolder.getAllMessagesAndEffectiveDates()).thenReturn(singletonMap(LOCAL_MESSAGE_UIDS.get(0), 1L));
        List<LocalMessage> localMessages = singletonList(mock(LocalMessage.class));
        when(localFolder.getMessagesByUids(anyCollection())).thenReturn(localMessages);
        return localMessages;
    }
}
