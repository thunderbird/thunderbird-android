package com.fsck.k9.mail.store.webdav;

import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.StoreConfig;

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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

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
        when(mockStore.processRequest(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(mockDataSet);
        when(mockStore.getHttpClient()).thenReturn(mockHttpClient);
        when(mockHttpClient.executeOverride(any(HttpUriRequest.class), any(HttpContext.class))).thenReturn(mockHttpResponse);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        folder = new WebDavFolder(mockStore, "testFolder");
    }

    @Test
    public void folder_can_fetch_less_than_10_envelopes() throws MessagingException {
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
