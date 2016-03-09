package com.fsck.k9.mail.store.webdav;

import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.BinaryTempFileBody;
import com.fsck.k9.mail.store.StoreConfig;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.tools.ant.taskdefs.condition.Http;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyMap;
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
    private StoreConfig mockStoreConfig;
    @Mock
    private HttpResponse mockHttpResponse;
    @Mock
    private StatusLine mockStatusLine;

    private WebDavFolder folder;
    private File tempDirectory;

    @Before
    public void before() throws MessagingException, IOException {
        MockitoAnnotations.initMocks(this);
        when(mockStore.getUrl()).thenReturn("https://localhost/webDavStoreUrl");
        when(mockStore.getHttpClient()).thenReturn(mockHttpClient);
        when(mockStore.getStoreConfig()).thenReturn(mockStoreConfig);
        folder = new WebDavFolder(mockStore, "testFolder");


        tempDirectory = new File("temp");
        if (!tempDirectory.exists()) {
            assertTrue(tempDirectory.mkdir());
            tempDirectory.deleteOnExit();
        }
        BinaryTempFileBody.setTempDirectory(new File("temp"));
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

    private void setupStoreForMessageFetching() {
        String authString = "authString";
        when(mockStoreConfig.getMaximumAutoDownloadMessageSize()).thenReturn(1900);
        when(mockStore.getAuthentication()).thenReturn(WebDavConstants.AUTH_TYPE_BASIC);
        when(mockStore.getAuthString()).thenReturn(authString);
    }

    private List<WebDavMessage> setup25MessagesToFetch() {

        List<WebDavMessage> messages = new ArrayList<>();
        for(int i = 0; i < 25; i++) {
            WebDavMessage message = new WebDavMessage("message"+i, folder);
            message.setUrl("http://example.org/Exchange/user/Inbox/message"+i+".EML");
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
