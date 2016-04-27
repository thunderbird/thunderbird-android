package com.fsck.k9.mail.store.webdav;

import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.BinaryTempFileBody;
import com.fsck.k9.mail.store.StoreConfig;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.protocol.HttpContext;
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
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static java.util.Collections.singletonList;

import static com.fsck.k9.mail.Folder.OPEN_MODE_RW;
import static com.fsck.k9.mail.Folder.OPEN_MODE_RO;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("deprecation")
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
    private StoreConfig mockStoreConfig;
    @Mock
    private HttpResponse mockHttpResponse;
    @Mock
    private StatusLine mockStatusLine;
    @Captor
    private ArgumentCaptor<Map<String, String>> headerCaptor;

    private WebDavFolder folder;

    private WebDavFolder destinationFolder;
    private String moveOrCopyXml = "<xml>MoveOrCopyXml</xml>";
    private HashMap<String, String> moveOrCopyHeaders;
    private List<WebDavMessage> messages;

    @Before
    public void before() throws MessagingException, IOException {
        MockitoAnnotations.initMocks(this);
        when(mockStore.getUrl()).thenReturn("https://localhost/webDavStoreUrl");
        when(mockStore.getHttpClient()).thenReturn(mockHttpClient);
        when(mockStore.getStoreConfig()).thenReturn(mockStoreConfig);
        folder = new WebDavFolder(mockStore, "testFolder");

        setupTempDirectory();
    }

    private void setupTempDirectory() {
        File tempDirectory = new File("temp");
        if (!tempDirectory.exists()) {
            assertTrue(tempDirectory.mkdir());
            tempDirectory.deleteOnExit();
        }
        BinaryTempFileBody.setTempDirectory(tempDirectory);
    }

    private WebDavFolder setupDestinationFolder() {
        WebDavFolder destinationFolder = new WebDavFolder(mockStore, "destFolder");
        when(mockStore.getFolder("destFolder")).thenReturn(destinationFolder);
        return destinationFolder;
    }

    private void setupFolderWithMessages(int count) throws MessagingException {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Brief", "t");
        String messageCountXml = "<xml>MessageCountXml</xml>";
        when(mockStore.getMessageCountXml("True")).thenReturn(messageCountXml);
        when(mockStore.processRequest("https://localhost/webDavStoreUrl/testFolder",
                "SEARCH", messageCountXml, headers)).thenReturn(mockDataSet);
        when(mockDataSet.getMessageCount()).thenReturn(count);
        folder.getMessageCount();
    }

    private WebDavMessage createWebDavMessage(String uid) {
        WebDavMessage webDavMessage = mock(WebDavMessage.class);
        when(webDavMessage.getUid()).thenReturn(uid);
        return webDavMessage;
    }

    private void setupGetUrlsRequestResponse(String uid, String url) throws MessagingException {
        String getUrlsXml = "<xml>GetUrls</xml>";
        when(mockStore.getMessageUrlsXml(new String[]{uid})).thenReturn(getUrlsXml);
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Brief", "t");
        when(mockStore.processRequest("https://localhost/webDavStoreUrl/testFolder", "SEARCH", getUrlsXml, headers))
                .thenReturn(mockDataSet);
        Map<String, String> urlUids = new HashMap<>();
        urlUids.put(uid, url);
        when(mockDataSet.getUidToUrl()).thenReturn(urlUids);
    }

    @Test
    public void folder_can_fetch_less_than_10_envelopes() throws MessagingException {
        when(mockStore.processRequest(anyString(), anyString(), anyString(), anyMapOf(String.class, String.class)))
                .thenReturn(mockDataSet);

        List<WebDavMessage> messages = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            WebDavMessage mockMessage = mock(WebDavMessage.class);
            messages.add(mockMessage);
        }

        FetchProfile profile = new FetchProfile();
        profile.add(FetchProfile.Item.ENVELOPE);
        folder.fetch(messages, profile, listener);
    }

    @Test
    public void folder_can_fetch_more_than_10_envelopes() throws MessagingException {
        when(mockStore.processRequest(anyString(), anyString(), anyString(),
                anyMapOf(String.class, String.class)))
                .thenReturn(mockDataSet);

        List<WebDavMessage> messages = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            WebDavMessage mockMessage = mock(WebDavMessage.class);
            messages.add(mockMessage);
        }
        FetchProfile profile = new FetchProfile();
        profile.add(FetchProfile.Item.ENVELOPE);
        folder.fetch(messages, profile, listener);
    }

    @Test
    public void folder_can_fetch_less_than_20_flags() throws MessagingException {
        when(mockStore.processRequest(anyString(), anyString(), anyString(),
                anyMapOf(String.class, String.class)))
                .thenReturn(mockDataSet);

        List<WebDavMessage> messages = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            WebDavMessage mockMessage = mock(WebDavMessage.class);
            messages.add(mockMessage);
        }
        FetchProfile profile = new FetchProfile();
        profile.add(FetchProfile.Item.FLAGS);
        folder.fetch(messages, profile, listener);
    }

    @Test
    public void folder_can_fetch_more_than_20_flags() throws MessagingException {
        when(mockStore.processRequest(anyString(), anyString(), anyString(),
                anyMapOf(String.class, String.class)))
                .thenReturn(mockDataSet);

        List<WebDavMessage> messages = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            WebDavMessage mockMessage = mock(WebDavMessage.class);
            messages.add(mockMessage);
        }

        FetchProfile profile = new FetchProfile();
        profile.add(FetchProfile.Item.FLAGS);
        folder.fetch(messages, profile, listener);
    }

    @Test
    public void folder_can_fetch_sensible_body_data_and_notifies_listener()
            throws MessagingException, IOException, URISyntaxException {
        setupStoreForMessageFetching();
        List<WebDavMessage> messages = setup25MessagesToFetch();

        when(mockHttpClient.executeOverride(any(HttpUriRequest.class), any(HttpContext.class))).thenAnswer(
                new Answer<HttpResponse>() {
                    @Override
                    public HttpResponse answer(InvocationOnMock invocation) throws Throwable {
                        HttpResponse httpResponse = mock(HttpResponse.class);
                        StatusLine statusLine = mock(StatusLine.class);
                        when(httpResponse.getStatusLine()).thenReturn(statusLine);
                        when(statusLine.getStatusCode()).thenReturn(200);

                        BasicHttpEntity httpEntity = new BasicHttpEntity();
                        String body = "";
                        httpEntity.setContent(new ByteArrayInputStream(body.getBytes("UTF-8")));
                        when(httpResponse.getEntity()).thenReturn(httpEntity);
                        return httpResponse;
                    }
                });

        FetchProfile profile = new FetchProfile();
        profile.add(FetchProfile.Item.BODY_SANE);
        folder.fetch(messages, profile, listener);
        verify(listener, times(25)).messageStarted(any(String.class), anyInt(), eq(25));
        verify(listener, times(25)).messageFinished(any(WebDavMessage.class), anyInt(), eq(25));
    }

    @Test
    public void folder_does_not_notify_listener_twice_when_fetching_flags_and_bodies()
            throws MessagingException, IOException, URISyntaxException {
        setupStoreForMessageFetching();
        when(mockStore.processRequest(anyString(), anyString(), anyString(),
                anyMapOf(String.class, String.class)))
                .thenReturn(mockDataSet);
        List<WebDavMessage> messages = setup25MessagesToFetch();
        when(mockHttpClient.executeOverride(any(HttpUriRequest.class), any(HttpContext.class))).thenAnswer(
                new Answer<HttpResponse>() {
                    @Override
                    public HttpResponse answer(InvocationOnMock invocation) throws Throwable {
                        HttpResponse httpResponse = mock(HttpResponse.class);
                        StatusLine statusLine = mock(StatusLine.class);
                        when(httpResponse.getStatusLine()).thenReturn(statusLine);
                        when(statusLine.getStatusCode()).thenReturn(200);

                        BasicHttpEntity httpEntity = new BasicHttpEntity();
                        String body = "";
                        httpEntity.setContent(new ByteArrayInputStream(body.getBytes("UTF-8")));
                        when(httpResponse.getEntity()).thenReturn(httpEntity);
                        return httpResponse;
                    }
                });

        FetchProfile profile = new FetchProfile();
        profile.add(FetchProfile.Item.FLAGS);
        profile.add(FetchProfile.Item.BODY);
        folder.fetch(messages, profile, listener);
        verify(listener, times(25)).messageStarted(any(String.class), anyInt(), anyInt());
        verify(listener, times(25)).messageFinished(any(WebDavMessage.class), anyInt(), anyInt());
    }

    private void setupStoreForMessageFetching() {
        String authString = "authString";
        when(mockStoreConfig.getMaximumAutoDownloadMessageSize()).thenReturn(1900);
        when(mockStore.getAuthentication()).thenReturn(WebDavConstants.AUTH_TYPE_BASIC);
        when(mockStore.getAuthString()).thenReturn(authString);
    }

    private List<WebDavMessage> setup25MessagesToFetch() {

        List<WebDavMessage> messages = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            WebDavMessage message = new WebDavMessage("message" + i, folder);
            message.setUrl("http://example.org/Exchange/user/Inbox/message" + i + ".EML");
            messages.add(message);
        }
        return messages;
    }

    @Test
    public void folder_can_handle_empty_response_to_body_request() throws MessagingException, IOException {
        setupStoreForMessageFetching();
        List<WebDavMessage> messages = setup25MessagesToFetch();

        when(mockHttpClient.executeOverride(any(HttpUriRequest.class), any(HttpContext.class))).thenAnswer(
                new Answer<HttpResponse>() {
                    @Override
                    public HttpResponse answer(InvocationOnMock invocation) throws Throwable {
                        HttpResponse httpResponse = mock(HttpResponse.class);
                        StatusLine statusLine = mock(StatusLine.class);
                        when(httpResponse.getStatusLine()).thenReturn(statusLine);
                        when(statusLine.getStatusCode()).thenReturn(200);
                        return httpResponse;
                    }
                });

        FetchProfile profile = new FetchProfile();
        profile.add(FetchProfile.Item.BODY_SANE);
        folder.fetch(messages, profile, listener);
        verify(listener, times(25)).messageStarted(any(String.class), anyInt(), eq(25));
        verify(listener, times(25)).messageFinished(any(WebDavMessage.class), anyInt(), eq(25));
    }

    @Test
    public void folder_ignores_exception_thrown_when_closing() throws MessagingException, IOException {
        setupStoreForMessageFetching();
        List<WebDavMessage> messages = setup25MessagesToFetch();

        when(mockHttpClient.executeOverride(any(HttpUriRequest.class), any(HttpContext.class))).thenAnswer(
                new Answer<HttpResponse>() {
                    @Override
                    public HttpResponse answer(InvocationOnMock invocation) throws Throwable {
                        HttpResponse httpResponse = mock(HttpResponse.class);
                        StatusLine statusLine = mock(StatusLine.class);
                        when(httpResponse.getStatusLine()).thenReturn(statusLine);
                        when(statusLine.getStatusCode()).thenReturn(200);

                        BasicHttpEntity httpEntity = new BasicHttpEntity();
                        InputStream mockInputStream = mock(InputStream.class);
                        when(mockInputStream.read(any(byte[].class), anyInt(), anyInt())).thenReturn(1).thenReturn(-1);
                        doThrow(new IOException("Test")).when(mockInputStream).close();
                        httpEntity.setContent(mockInputStream);
                        when(httpResponse.getEntity()).thenReturn(httpEntity);
                        return httpResponse;
                    }
                });

        FetchProfile profile = new FetchProfile();
        profile.add(FetchProfile.Item.BODY_SANE);
        folder.fetch(messages, profile, listener);
        verify(listener, times(25)).messageStarted(any(String.class), anyInt(), eq(25));
        verify(listener, times(25)).messageFinished(any(WebDavMessage.class), anyInt(), eq(25));
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
        int messageCount = 23;
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Brief", "t");
        String messageCountXml = "<xml>MessageCountXml</xml>";
        when(mockStore.getMessageCountXml("True")).thenReturn(messageCountXml);
        when(mockStore.processRequest("https://localhost/webDavStoreUrl/testFolder",
                "SEARCH", messageCountXml, headers)).thenReturn(mockDataSet);
        when(mockDataSet.getMessageCount()).thenReturn(messageCount);

        int result = folder.getMessageCount();

        assertEquals(messageCount, result);
    }

    @Test
    public void can_fetch_unread_message_count() throws Exception {
        int unreadMessageCount = 13;
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Brief", "t");
        String messageCountXml = "<xml>MessageCountXml</xml>";
        when(mockStore.getMessageCountXml("False")).thenReturn(messageCountXml);
        when(mockStore.processRequest("https://localhost/webDavStoreUrl/testFolder",
                "SEARCH", messageCountXml, headers)).thenReturn(mockDataSet);
        when(mockDataSet.getMessageCount()).thenReturn(unreadMessageCount);

        int result = folder.getUnreadMessageCount();

        assertEquals(unreadMessageCount, result);
    }

    @Test
    public void getMessages_should_request_message_search() throws MessagingException {
        int totalMessages = 23;
        int messageStart = 1;
        int messageEnd = 11;
        setupFolderWithMessages(totalMessages);
        String messagesXml = "<xml>MessagesXml</xml>";
        buildSearchResponse(mockDataSet);
        when(mockStore.getMessagesXml()).thenReturn(messagesXml);
        when(mockStore.processRequest(eq("https://localhost/webDavStoreUrl/testFolder"), eq("SEARCH"),
                eq(messagesXml), Matchers.<Map<String, String>>any())).thenReturn(mockDataSet);

        folder.getMessages(messageStart, messageEnd, new Date(), listener);

        verify(listener, times(5)).messageStarted(anyString(), anyInt(), eq(5));
        verify(listener, times(5)).messageFinished(any(WebDavMessage.class), anyInt(), eq(5));
    }

    @Test
    public void getMessages_shouldProvideCorrectHeadersInRequest() throws MessagingException {
        int totalMessages = 23;
        int messageStart = 1;
        int messageEnd = 11;
        setupFolderWithMessages(totalMessages);
        String messagesXml = "<xml>MessagesXml</xml>";
        buildSearchResponse(mockDataSet);
        when(mockStore.getMessagesXml()).thenReturn(messagesXml);
        when(mockStore.processRequest(eq("https://localhost/webDavStoreUrl/testFolder"), eq("SEARCH"),
                eq(messagesXml), Matchers.<Map<String, String>>any())).thenReturn(mockDataSet);

        folder.getMessages(messageStart, messageEnd, new Date(), listener);

        verify(mockStore, times(2)).processRequest(anyString(), anyString(), anyString(),
                headerCaptor.capture());
        assertEquals(2, headerCaptor.getValue().size());
        assertEquals("t", headerCaptor.getValue().get("Brief"));
        assertEquals("rows=" + (totalMessages - (messageEnd)) + "-" + (totalMessages - messageStart)
                , headerCaptor.getValue().get("Range"));
    }

    private void buildSearchResponse(DataSet mockDataSet) {
        String[] uids = new String[]{"uid1", "uid2", "uid3", "uid4", "uid5"};
        HashMap<String, String> uidToUrls = new HashMap<>();
        uidToUrls.put("uid1", "url1");
        uidToUrls.put("uid2", "url2");
        uidToUrls.put("uid3", "url3");
        uidToUrls.put("uid4", "url4");
        uidToUrls.put("uid5", "url5");

        when(mockDataSet.getUids()).thenReturn(uids);
        when(mockDataSet.getUidToUrl()).thenReturn(uidToUrls);
    }

    @Test(expected = MessagingException.class)
    public void getMessages_should_throw_message_exception_if_requesting_messages_from_empty_folder()
            throws MessagingException {
        folder.getMessages(0, 10, new Date(), listener);
    }

    private void setupMoveOrCopy() throws MessagingException {
        destinationFolder = setupDestinationFolder();
        String uid = "uid1";
        String url = "url1";
        messages = singletonList(createWebDavMessage(uid));
        setupGetUrlsRequestResponse(uid, url);
        when(mockStore.getMoveOrCopyMessagesReadXml(eq(new String[]{url}), anyBoolean())).thenReturn(moveOrCopyXml);
        moveOrCopyHeaders = new HashMap<>();
        moveOrCopyHeaders.put("Destination", "https://localhost/webDavStoreUrl/destFolder");
        moveOrCopyHeaders.put("Brief", "t");
        moveOrCopyHeaders.put("If-Match", "*");
    }

    @Test
    public void moveMessages_should_requestMoveXml() throws Exception {
        setupMoveOrCopy();

        folder.moveMessages(messages, destinationFolder);

        verify(mockStore).getMoveOrCopyMessagesReadXml(eq(new String[]{"url1"}),
                eq(true));
    }

    @Test
    public void moveMessages_should_send_move_command() throws Exception {
        setupMoveOrCopy();

        folder.moveMessages(messages, destinationFolder);

        verify(mockStore).processRequest("https://localhost/webDavStoreUrl/testFolder", "BMOVE",
                moveOrCopyXml, moveOrCopyHeaders, false);
    }

    @Test
    public void copyMessages_should_requestCopyXml() throws Exception {
        setupMoveOrCopy();

        folder.copyMessages(messages, destinationFolder);

        verify(mockStore).getMoveOrCopyMessagesReadXml(eq(new String[]{"url1"}),
                eq(false));
    }

    @Test
    public void copyMessages_should_send_copy_command() throws Exception {
        setupMoveOrCopy();

        folder.copyMessages(messages, destinationFolder);

        verify(mockStore).processRequest("https://localhost/webDavStoreUrl/testFolder", "BCOPY",
                moveOrCopyXml, moveOrCopyHeaders, false);
    }

    @Test
    public void appendWebDavMessages_replaces_messages_with_WebDAV_versions() throws MessagingException, IOException {
        when(mockHttpClient.executeOverride(any(HttpUriRequest.class), any(HttpContext.class))).thenReturn(mockHttpResponse);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
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
