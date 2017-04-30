package com.fsck.k9.mail.store.imap;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fsck.k9.mail.K9LibRobolectricTestRunner;
import com.fsck.k9.mail.PushReceiver;
import com.fsck.k9.mail.store.StoreConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(K9LibRobolectricTestRunner.class)
public class ImapPusherTest {
    private ImapStore imapStore;
    private TestImapPusher imapPusher;


    @Before
    public void setUp() throws Exception {
        imapStore = mock(ImapStore.class);

        PushReceiver pushReceiver = mock(PushReceiver.class);
        imapPusher = new TestImapPusher(imapStore, pushReceiver);
    }

    @Test
    public void start_shouldSetLastRefreshToCurrentTime() throws Exception {
        List<String> folderNames = Collections.singletonList("INBOX");

        imapPusher.start(folderNames);

        assertEquals(TestImapPusher.CURRENT_TIME_MILLIS, imapPusher.getLastRefresh());
    }

    @Test
    public void start_withSingleFolderName_shouldCreateImapFolderPusherAndCallStartOnIt() throws Exception {
        List<String> folderNames = Collections.singletonList("INBOX");

        imapPusher.start(folderNames);

        List<ImapFolderPusher> imapFolderPushers = imapPusher.getImapFolderPushers();
        assertEquals(1, imapFolderPushers.size());
        ImapFolderPusher imapFolderPusher = imapFolderPushers.get(0);
        verify(imapFolderPusher).start();
    }

    @Test
    public void start_calledAfterStart_shouldStopFirstImapFolderPusher() throws Exception {
        imapPusher.start(Collections.singletonList("Drafts"));

        imapPusher.start(Collections.singletonList("INBOX"));

        ImapFolderPusher draftsPusher = imapPusher.getImapFolderPushers().get(0);
        verify(draftsPusher).stop();
    }

    @Test
    public void start_withTwoFolderNames_shouldCreateTwoImapFolderPushersAndCallStart() throws Exception {
        List<String> folderNames = Arrays.asList("Important", "Drafts");

        imapPusher.start(folderNames);

        List<ImapFolderPusher> imapFolderPushers = imapPusher.getImapFolderPushers();
        assertEquals(2, imapFolderPushers.size());
        ImapFolderPusher imapFolderPusherOne = imapFolderPushers.get(0);
        ImapFolderPusher imapFolderPusherTwo = imapFolderPushers.get(1);
        verify(imapFolderPusherOne).start();
        verify(imapFolderPusherTwo).start();
    }

    @Test
    public void stop_withoutStartBeingCalled_shouldNotCreateAnyImapFolderPushers() throws Exception {
        imapPusher.stop();

        List<ImapFolderPusher> imapFolderPushers = imapPusher.getImapFolderPushers();
        assertEquals(0, imapFolderPushers.size());
    }

    @Test
    public void stop_afterStartWithSingleFolderName_shouldStopImapFolderPusher() throws Exception {
        List<String> folderNames = Collections.singletonList("Archive");
        imapPusher.start(folderNames);

        imapPusher.stop();

        List<ImapFolderPusher> imapFolderPushers = imapPusher.getImapFolderPushers();
        assertEquals(1, imapFolderPushers.size());
        ImapFolderPusher imapFolderPusher = imapFolderPushers.get(0);
        verify(imapFolderPusher).stop();
    }

    @Test
    public void stop_withImapFolderPusherThrowing_shouldNotThrow() throws Exception {
        List<String> folderNames = Collections.singletonList("Archive");
        imapPusher.start(folderNames);
        ImapFolderPusher imapFolderPusher = imapPusher.getImapFolderPushers().get(0);
        doThrow(RuntimeException.class).when(imapFolderPusher).stop();

        imapPusher.stop();
    }

    @Test
    public void refresh_shouldCallRefreshOnStartedImapFolderPusher() throws Exception {
        List<String> folderNames = Collections.singletonList("Trash");
        imapPusher.start(folderNames);

        imapPusher.refresh();

        List<ImapFolderPusher> imapFolderPushers = imapPusher.getImapFolderPushers();
        assertEquals(1, imapFolderPushers.size());
        ImapFolderPusher imapFolderPusher = imapFolderPushers.get(0);
        verify(imapFolderPusher).refresh();
    }

    @Test
    public void refresh_withImapFolderPusherThrowing_shouldNotThrow() throws Exception {
        List<String> folderNames = Collections.singletonList("Folder");
        imapPusher.start(folderNames);
        ImapFolderPusher imapFolderPusher = imapPusher.getImapFolderPushers().get(0);
        doThrow(RuntimeException.class).when(imapFolderPusher).refresh();

        imapPusher.refresh();
    }

    @Test
    public void getRefreshInterval() throws Exception {
        StoreConfig storeConfig = mock(StoreConfig.class);
        when(storeConfig.getIdleRefreshMinutes()).thenReturn(23);
        when(imapStore.getStoreConfig()).thenReturn(storeConfig);

        int result = imapPusher.getRefreshInterval();

        assertEquals(23 * 60 * 1000, result);
    }

    @Test
    public void getLastRefresh_shouldBeMinusOneInitially() throws Exception {
        long result = imapPusher.getLastRefresh();

        assertEquals(-1L, result);
    }


    static class TestImapPusher extends ImapPusher {
        public static final long CURRENT_TIME_MILLIS = 1454375675162L;


        private final List<ImapFolderPusher> imapFolderPushers = new ArrayList<>();


        public TestImapPusher(ImapStore store, PushReceiver receiver) {
            super(store, receiver);
        }

        @Override
        ImapFolderPusher createImapFolderPusher(String folderName) {
            ImapFolderPusher imapFolderPusher = mock(ImapFolderPusher.class);
            imapFolderPushers.add(imapFolderPusher);
            return imapFolderPusher;
        }

        public List<ImapFolderPusher> getImapFolderPushers() {
            return imapFolderPushers;
        }

        @Override
        long currentTimeMillis() {
            return CURRENT_TIME_MILLIS;
        }
    }
}
