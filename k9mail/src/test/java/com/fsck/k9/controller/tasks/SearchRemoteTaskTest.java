package com.fsck.k9.controller.tasks;


import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.K9RobolectricTestRunner;
import com.fsck.k9.Preferences;
import com.fsck.k9.controller.SimpleMessagingListener;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalStore;
import edu.emory.mathcs.backport.java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLog;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@SuppressWarnings("unchecked")
@RunWith(K9RobolectricTestRunner.class)
public class SearchRemoteTaskTest {
    private static final String FOLDER_NAME = "Folder";
    private static final int MAXIMUM_SMALL_MESSAGE_SIZE = 1000;

    @Mock
    private Account account;
    @Mock
    private AccountStats accountStats;
    @Mock
    private SimpleMessagingListener listener;
    @Mock
    private LocalFolder localFolder;
    @Mock
    private Folder remoteFolder;
    @Mock
    private LocalStore localStore;
    @Mock
    private Store remoteStore;
    @Captor
    private ArgumentCaptor<FetchProfile> fetchProfileCaptor;

    private Context appContext;
    private Set<Flag> reqFlags;
    private Set<Flag> forbiddenFlags;

    private List<Message> remoteMessages;
    @Mock
    private Message remoteOldMessage;
    @Mock
    private Message remoteNewMessage1;
    @Mock
    private Message remoteNewMessage2;
    @Mock
    private LocalMessage localNewMessage1;
    @Mock
    private LocalMessage localNewMessage2;
    @Mock
    private SearchResultsLoader searchResultsLoader;
    private volatile boolean hasFetchedMessage = false;


    @Before
    public void setUp() throws MessagingException {
        ShadowLog.stream = System.out;
        MockitoAnnotations.initMocks(this);
        appContext = ShadowApplication.getInstance().getApplicationContext();

        configureAccount();
        configureLocalStore();
    }

    private void configureAccount() throws MessagingException {
        when(account.isAvailable(appContext)).thenReturn(true);
        when(account.getLocalStore()).thenReturn(localStore);
        when(account.getStats(any(Context.class))).thenReturn(accountStats);
        when(account.getMaximumAutoDownloadMessageSize()).thenReturn(MAXIMUM_SMALL_MESSAGE_SIZE);
        when(account.getEmail()).thenReturn("user@host.com");
    }

    private void configureLocalStore() throws MessagingException {
        when(localStore.getFolder(FOLDER_NAME)).thenReturn(localFolder);
        when(localFolder.getName()).thenReturn(FOLDER_NAME);
        when(localStore.getPersonalNamespaces(false)).thenReturn(Collections.singletonList(localFolder));
    }

    private void configureRemoteStoreWithFolder() throws MessagingException {
        when(account.getRemoteStore()).thenReturn(remoteStore);
        when(remoteStore.getFolder(FOLDER_NAME)).thenReturn(remoteFolder);
        when(remoteFolder.getName()).thenReturn(FOLDER_NAME);
    }

    private void setAccountsInPreferences(Map<String, Account> newAccounts)
            throws Exception {
        Field accounts = Preferences.class.getDeclaredField("accounts");
        accounts.setAccessible(true);
        accounts.set(Preferences.getPreferences(appContext), newAccounts);

        Field accountsInOrder = Preferences.class.getDeclaredField("accountsInOrder");
        accountsInOrder.setAccessible(true);
        ArrayList<Account> newAccountsInOrder = new ArrayList<>();
        newAccountsInOrder.addAll(newAccounts.values());
        accountsInOrder.set(Preferences.getPreferences(appContext), newAccountsInOrder);
    }


    private void setupRemoteSearch() throws Exception {
        setAccountsInPreferences(Collections.singletonMap("1", account));
        configureRemoteStoreWithFolder();

        remoteMessages = new ArrayList<>();
        Collections.addAll(remoteMessages, remoteOldMessage, remoteNewMessage1, remoteNewMessage2);
        List<Message> newRemoteMessages = new ArrayList<>();
        Collections.addAll(newRemoteMessages, remoteNewMessage1, remoteNewMessage2);

        when(remoteOldMessage.getUid()).thenReturn("oldMessageUid");
        when(remoteNewMessage1.getUid()).thenReturn("newMessageUid1");
        when(localNewMessage1.getUid()).thenReturn("newMessageUid1");
        when(remoteNewMessage2.getUid()).thenReturn("newMessageUid2");
        when(localNewMessage2.getUid()).thenReturn("newMessageUid2");
        when(remoteFolder.search(anyString(), anySet(), anySet())).thenReturn(remoteMessages);
        when(localFolder.extractNewMessages(Matchers.<List<Message>>any())).thenReturn(newRemoteMessages);
        when(localFolder.getMessage("newMessageUid1")).thenReturn(localNewMessage1);
        when(localFolder.getMessage("newMessageUid2")).thenAnswer(
                new Answer<LocalMessage>() {
                    @Override
                    public LocalMessage answer(InvocationOnMock invocation) throws Throwable {
                        if(hasFetchedMessage) {
                            return localNewMessage2;
                        }
                        else
                            return null;
                    }
                }
        );
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                hasFetchedMessage = true;
                return null;
            }
        }).when(remoteFolder).fetch(
                Matchers.eq(Collections.singletonList(remoteNewMessage2)),
                any(FetchProfile.class),
                Matchers.<MessageRetrievalListener>eq(null));
        reqFlags = Collections.singleton(Flag.ANSWERED);
        forbiddenFlags = Collections.singleton(Flag.DELETED);

        when(account.getRemoteSearchNumResults()).thenReturn(50);
    }

    @Test
    public void run_shouldNotifyStartedListingRemoteMessages() throws Exception {
        setupRemoteSearch();

        new SearchRemoteTask(appContext, searchResultsLoader, "1", FOLDER_NAME, "query", reqFlags, forbiddenFlags, listener).run();

        verify(listener).remoteSearchStarted(FOLDER_NAME);
    }

    @Test
    public void run_shouldQueryRemoteFolder() throws Exception {
        setupRemoteSearch();

        new SearchRemoteTask(appContext, searchResultsLoader, "1", FOLDER_NAME, "query", reqFlags, forbiddenFlags, listener).run();

        verify(remoteFolder).search("query", reqFlags, forbiddenFlags);
    }

    @Test
    public void run_shouldAskLocalFolderToDetermineNewMessages() throws Exception {
        setupRemoteSearch();

        new SearchRemoteTask(appContext, searchResultsLoader, "1", FOLDER_NAME, "query", reqFlags, forbiddenFlags, listener).run();

        verify(localFolder).extractNewMessages(remoteMessages);
    }

    @Test
    public void run_shouldLoadOnlyNewMessages() throws Exception {
        setupRemoteSearch();

        new SearchRemoteTask(appContext, searchResultsLoader, "1", FOLDER_NAME, "query", reqFlags, forbiddenFlags, listener).run();

        verify(searchResultsLoader).load(
                eq(Arrays.asList(new Message[]{remoteNewMessage1, remoteNewMessage2})),
                eq(localFolder), eq(remoteFolder), eq(listener));
    }

    @Test
    public void run_shouldNotifyOnFailure() throws Exception {
        setupRemoteSearch();
        when(account.getRemoteStore()).thenThrow(new MessagingException("Test"));

        new SearchRemoteTask(appContext, searchResultsLoader, "1", FOLDER_NAME, "query", reqFlags, forbiddenFlags, listener).run();

        verify(listener).remoteSearchFailed(null, "Test");
    }

    @Test
    public void run_shouldNotifyOnFinishFollowingFailure() throws Exception {
        setupRemoteSearch();
        when(account.getRemoteStore()).thenThrow(new MessagingException("Test"));

        new SearchRemoteTask(appContext, searchResultsLoader, "1", FOLDER_NAME, "query", reqFlags, forbiddenFlags, listener).run();

        verify(listener).remoteSearchFinished(FOLDER_NAME, 0, 50, Collections.<Message>emptyList());
    }

    @Test
    public void run_shouldNotifyOnFinishFollowingSuccess() throws Exception {
        setupRemoteSearch();

        new SearchRemoteTask(appContext, searchResultsLoader, "1", FOLDER_NAME, "query", reqFlags, forbiddenFlags, listener).run();

        verify(listener).remoteSearchFinished(FOLDER_NAME, 0, 50, Collections.<Message>emptyList());
    }

}
