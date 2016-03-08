package com.fsck.k9.mail.store.webdav;

import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HttpContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static java.util.Collections.singletonList;

import static com.fsck.k9.mail.Folder.OPEN_MODE_RW;
import static com.fsck.k9.mail.Folder.OPEN_MODE_RO;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 21)
public class WebDavFolderTest {
    @Mock
    private MessageRetrievalListener<WebDavMessage> listener;
    @Mock
    private WebDavStore mockStore;
    @Mock
    private DataSet mockDataSet;
    @Mock
    private WebDavHttpClient mockHttpClient;
    @Mock
    private HttpResponse mockHttpResponse;
    @Mock
    private StatusLine mockStatusLine;

    private WebDavFolder folder;

    @Before
    public void before() throws MessagingException, IOException {
        MockitoAnnotations.initMocks(this);
        when(mockStore.getUrl()).thenReturn("https://localhost/webDavStoreUrl");
        when(mockStore.getHttpClient()).thenReturn(mockHttpClient);
        when(mockHttpClient.executeOverride(any(HttpUriRequest.class), any(HttpContext.class))).thenReturn(mockHttpResponse);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        folder = new WebDavFolder(mockStore, "testFolder");
    }

    private WebDavFolder setupDestinationFolder() {
        WebDavFolder destinationFolder = new WebDavFolder(mockStore, "destFolder");
        when(mockStore.getFolder("destFolder")).thenReturn(destinationFolder);
        return destinationFolder;
    }

    private void verifyUrlMappingRequest(String uid, String url) throws MessagingException {
        String getUrlsXml = "<xml>GetUrls</xml>";
        when(mockStore.getMessageUrlsXml(new String[]{uid})).thenReturn(getUrlsXml);
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Brief", "t");
        when(mockStore.processRequest("https://localhost/webDavStoreUrl/testFolder", "SEARCH", getUrlsXml, headers))
                .thenReturn(mockDataSet);
        Map<String, String> urlUids = new HashMap<String,String>();
        urlUids.put(uid, url);
        when(mockDataSet.getUidToUrl()).thenReturn(urlUids);
    }

    @Test
    public void folder_can_fetch_less_than_10_envelopes() throws MessagingException {
        when(mockStore.processRequest(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(mockDataSet);

        List<WebDavMessage> messages = new ArrayList<>();
        for(int i = 0; i < 5; i++) {
            WebDavMessage mockMessage = mock(WebDavMessage.class);
            messages.add(mockMessage);
        }

        FetchProfile profile = new FetchProfile();
        profile.add(FetchProfile.Item.ENVELOPE);
        folder.fetch(messages, profile, listener);
    }

    @Test
    public void folder_can_fetch_more_than_10_envelopes() throws MessagingException {
        when(mockStore.processRequest(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(mockDataSet);

        List<WebDavMessage> messages = new ArrayList<>();
        for(int i = 0; i < 15; i++) {
            WebDavMessage mockMessage = mock(WebDavMessage.class);
            messages.add(mockMessage);
        }
        FetchProfile profile = new FetchProfile();
        profile.add(FetchProfile.Item.ENVELOPE);
        folder.fetch(messages, profile, listener);
    }

    @Test
    public void folder_can_fetch_less_than_20_flags() throws MessagingException {
        when(mockStore.processRequest(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(mockDataSet);

        List<WebDavMessage> messages = new ArrayList<>();
        for(int i = 0; i < 5; i++) {
            WebDavMessage mockMessage = mock(WebDavMessage.class);
            messages.add(mockMessage);
        }
        FetchProfile profile = new FetchProfile();
        profile.add(FetchProfile.Item.FLAGS);
        folder.fetch(messages, profile, listener);
    }

    @Test
    public void folder_can_fetch_more_than_20_flags() throws MessagingException {
        when(mockStore.processRequest(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(mockDataSet);

        List<WebDavMessage> messages = new ArrayList<>();
        for(int i = 0; i < 25; i++) {
            WebDavMessage mockMessage = mock(WebDavMessage.class);
            messages.add(mockMessage);
        }

        FetchProfile profile = new FetchProfile();
        profile.add(FetchProfile.Item.FLAGS);
        folder.fetch(messages, profile, listener);
    }

    @Test
    public void folder_does_not_start_open() throws MessagingException {
        assertFalse(folder.isOpen());
    }

    @Test
    public void open_should_open_folder() throws MessagingException {
        folder.open(OPEN_MODE_RW);
        assertTrue(folder.isOpen());
    }

    @Test
    public void close_should_close_folder() throws MessagingException {
        folder.close();
        assertFalse(folder.isOpen());
    }

    @Test
    public void mode_is_always_readwrite() throws Exception {
        assertEquals(OPEN_MODE_RW, folder.getMode());
        folder.open(OPEN_MODE_RO);
        assertEquals(OPEN_MODE_RW, folder.getMode());
    }

    @Test
    public void exists_is_always_true() throws Exception {
        assertTrue(folder.exists());
    }

    @Test
    public void can_fetch_message_count() throws Exception {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Brief", "t");
        String messageCountXml = "<xml>MessageCountXml</xml>";
        when(mockStore.getMessageCountXml("True")).thenReturn(messageCountXml);
        when(mockStore.processRequest("https://localhost/webDavStoreUrl/testFolder",
                "SEARCH", messageCountXml, headers)).thenReturn(mockDataSet);
        when(mockDataSet.getMessageCount()).thenReturn(23);
        assertEquals(23, folder.getMessageCount());
    }

    @Test
    public void can_fetch_unread_message_count() throws Exception {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Brief", "t");
        String messageCountXml = "<xml>MessageCountXml</xml>";
        when(mockStore.getMessageCountXml("False")).thenReturn(messageCountXml);
        when(mockStore.processRequest("https://localhost/webDavStoreUrl/testFolder",
                "SEARCH", messageCountXml, headers)).thenReturn(mockDataSet);
        when(mockDataSet.getMessageCount()).thenReturn(13);
        assertEquals(13, folder.getUnreadMessageCount());
    }

    @Test
    public void moveMessages_should_send_move_command() throws Exception {
        WebDavFolder destinationFolder = setupDestinationFolder();
        String uid = "uid1";
        String url = "url1";
        List<WebDavMessage> messages = singletonList(createWebDavMessage(uid));
        verifyUrlMappingRequest(uid, url);

        String moveOrCopyXml = "<xml>MoveOrCopyXml</xml>";
        when(mockStore.getMoveOrCopyMessagesReadXml(new String[]{url}, true)).thenReturn(moveOrCopyXml);
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Destination", "https://localhost/webDavStoreUrl/destFolder");
        headers.put("Brief", "t");
        headers.put("If-Match", "*");
        when(mockStore.processRequest("https://localhost/webDavStoreUrl/testFolder", "BMOVE", moveOrCopyXml, headers, false))
                .thenReturn(mockDataSet);

        Map<String, String> result = folder.moveMessages(messages, destinationFolder);
        assertNull(result);
    }

    @Test
    public void copyMessages_should_send_copy_command() throws Exception {
        WebDavFolder destinationFolder = setupDestinationFolder();
        String uid = "uid1";
        String url = "url1";
        List<WebDavMessage> messages = singletonList(createWebDavMessage(uid));
        verifyUrlMappingRequest(uid, url);


        String moveOrCopyXml = "<xml>MoveOrCopyXml</xml>";
        when(mockStore.getMoveOrCopyMessagesReadXml(new String[]{url},false)).thenReturn(moveOrCopyXml);
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Destination", "https://localhost/webDavStoreUrl/destFolder");
        headers.put("Brief", "t");
        headers.put("If-Match", "*");
        when(mockStore.processRequest("https://localhost/webDavStoreUrl/testFolder", "COPY", moveOrCopyXml, headers, false))
                .thenReturn(mockDataSet);
        Map<String, String> result = folder.copyMessages(messages, destinationFolder);
        assertNull(result);
    }

    private WebDavMessage createWebDavMessage(String uid) {
        WebDavMessage webDavMessage = mock(WebDavMessage.class);
        when(webDavMessage.getUid()).thenReturn(uid);
        return webDavMessage;
    }

    @Test
    public void appendWebDavMessages_replaces_messages_with_WebDAV_versions() throws MessagingException {
        when(mockStatusLine.getStatusCode()).thenReturn(200);

        List<Message> existingMessages = new ArrayList<>();
        Message existingMessage = mock(Message.class);
        existingMessages.add(existingMessage);
        String messageUid = "testMessageUid";
        when(existingMessage.getUid()).thenReturn(messageUid);

        List<? extends Message> response = folder.appendWebDavMessages(existingMessages);

        assertEquals(1, response.size(), 1);
        assertEquals(WebDavMessage.class, response.get(0).getClass());
        assertEquals(messageUid, response.get(0).getUid());
    }
}
