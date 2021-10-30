package com.fsck.k9.mail.store.webdav;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.FolderType;
import com.fsck.k9.mail.K9LibRobolectricTestRunner;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.filter.Base64;
import com.fsck.k9.mail.ssl.TrustManagerFactory;

import javax.net.ssl.SSLException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.OngoingStubbing;

import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(K9LibRobolectricTestRunner.class)
public class WebDavStoreTest {
    private static final HttpResponse OK_200_RESPONSE = createOkResponse();
    private static final HttpResponse UNAUTHORIZED_401_RESPONSE = createResponse(401);
    private static final HttpResponse SERVER_ERROR_500_RESPONSE = createResponse(500);


    @Mock
    private WebDavHttpClient.WebDavHttpClientFactory mockHttpClientFactory;
    @Mock
    private WebDavHttpClient mockHttpClient;
    @Mock
    private ClientConnectionManager mockClientConnectionManager;
    @Mock
    private SchemeRegistry mockSchemeRegistry;
    @Mock
    private TrustManagerFactory trustManagerFactory;
    @Mock
    private DraftsFolderProvider draftsFolderProvider;

    private ArgumentCaptor<HttpGeneric> requestCaptor;

    private ServerSettings serverSettings;
    private WebDavStore webDavStore;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        HttpParams httpParams = new BasicHttpParams();
        when(mockHttpClientFactory.create()).thenReturn(mockHttpClient);
        when(mockHttpClient.getParams()).thenReturn(httpParams);
        when(mockHttpClient.getConnectionManager()).thenReturn(mockClientConnectionManager);
        when(mockClientConnectionManager.getSchemeRegistry()).thenReturn(mockSchemeRegistry);

        serverSettings = createServerSettings(ConnectionSecurity.SSL_TLS_REQUIRED);
        webDavStore = createWebDavStore();
    }

    @Test
    public void checkSettings_withWebDavUri_shouldUseInsecureConnection() throws Exception {
        WebDavStore webDavStore = createWebDavStore(ConnectionSecurity.NONE);
        configureHttpResponses(UNAUTHORIZED_401_RESPONSE, OK_200_RESPONSE);

        webDavStore.checkSettings();

        assertHttpClientUsesHttps(false);
    }

    @Test
    public void checkSettings_withWebDavSslUri_shouldUseSecureConnection() throws Exception {
        WebDavStore webDavStore = createWebDavStore(ConnectionSecurity.SSL_TLS_REQUIRED);
        configureHttpResponses(UNAUTHORIZED_401_RESPONSE, OK_200_RESPONSE);

        webDavStore.checkSettings();

        assertHttpClientUsesHttps(true);
    }

    @Test
    public void checkSettings_withWebDavTlsUri_shouldUseSecureConnection() throws Exception {
        WebDavStore webDavStore = createWebDavStore(ConnectionSecurity.STARTTLS_REQUIRED);
        configureHttpResponses(UNAUTHORIZED_401_RESPONSE, OK_200_RESPONSE);

        webDavStore.checkSettings();

        assertHttpClientUsesHttps(true);
    }

    @Test
    public void checkSettings_withOkResponse_shouldPerformFormBasedAuthentication() throws Exception {
        ArgumentCaptor<HttpGeneric> requestCaptor = ArgumentCaptor.forClass(HttpGeneric.class);
        when(mockHttpClient.executeOverride(requestCaptor.capture(), any(HttpContext.class)))
                .thenReturn(OK_200_RESPONSE)
                .thenReturn(createOkResponseWithForm())
                .thenAnswer(createOkResponseWithCookie())
                .thenReturn(OK_200_RESPONSE);

        webDavStore.checkSettings();

        List<HttpGeneric> requests = requestCaptor.getAllValues();
        assertEquals(4, requests.size());
        assertEquals("GET", requests.get(0).getMethod()); // Checking auth type
        assertEquals("POST", requests.get(1).getMethod()); // Posting form data
        assertEquals("https://webdav.example.org:443/exchweb/bin/auth/owaauth.dll", requests.get(1).getURI().toString());
        assertEquals("POST", requests.get(2).getMethod()); // Confirming login
        assertEquals("https://webdav.example.org:443/exchweb/bin/auth/owaauth.dll", requests.get(2).getURI().toString());
        assertEquals("GET", requests.get(3).getMethod()); // Getting response
    }

    @Test
    public void checkSettings_withInitialUnauthorizedResponse_shouldPerformBasicAuthentication() throws Exception {
        configureHttpResponses(UNAUTHORIZED_401_RESPONSE, OK_200_RESPONSE);

        webDavStore.checkSettings();

        List<HttpGeneric> requests = requestCaptor.getAllValues();
        assertEquals(2, requests.size());
        assertEquals("GET", requests.get(0).getMethod());
        assertEquals("GET", requests.get(1).getMethod());
        assertEquals("Basic " + Base64.encode("user:password"),
                requests.get(1).getHeaders("Authorization")[0].getValue());
    }

    @Test(expected = MessagingException.class)
    public void checkSettings_withUnauthorizedResponses_shouldThrow() throws Exception {
        configureHttpResponses(UNAUTHORIZED_401_RESPONSE, UNAUTHORIZED_401_RESPONSE);

        webDavStore.checkSettings();
    }

    @Test(expected = MessagingException.class)
    public void checkSettings_withErrorResponse_shouldThrow() throws Exception {
        configureHttpResponses(UNAUTHORIZED_401_RESPONSE, SERVER_ERROR_500_RESPONSE);

        webDavStore.checkSettings();
    }

    @Test(expected = CertificateValidationException.class)
    public void checkSettings_withSslException_shouldThrowCertificateValidationException() throws Exception {
        ArgumentCaptor<HttpGeneric> requestCaptor = ArgumentCaptor.forClass(HttpGeneric.class);
        when(mockHttpClient.executeOverride(requestCaptor.capture(), any(HttpContext.class)))
                .thenThrow(new SSLException("Test"));

        webDavStore.checkSettings();
    }

    //TODO: Is this really something we want to test?
    @Test
    public void checkSettings_shouldRegisterHttpsSchemeWithRegistry() throws Exception {
        configureHttpResponses(UNAUTHORIZED_401_RESPONSE, OK_200_RESPONSE);

        webDavStore.checkSettings();

        ArgumentCaptor<Scheme> schemeCaptor = ArgumentCaptor.forClass(Scheme.class);
        verify(mockSchemeRegistry).register(schemeCaptor.capture());
        assertEquals("https", schemeCaptor.getValue().getName());
        assertEquals(WebDavSocketFactory.class, schemeCaptor.getValue().getSocketFactory().getClass());
    }

    @Test
    public void getFolder_shouldReturnWebDavFolderInstance() {
        WebDavFolder result = webDavStore.getFolder("INBOX");

        assertEquals(WebDavFolder.class, result.getClass());
    }

    @Test
    public void getFolder_calledTwice_shouldReturnFirstInstance() {
        String folderName = "Trash";
        WebDavFolder webDavFolder = webDavStore.getFolder(folderName);

        WebDavFolder result = webDavStore.getFolder(folderName);

        assertSame(webDavFolder, result);
    }

    @Test
    public void getPersonalNamespaces_shouldRequestSpecialFolders() throws Exception {
        configureHttpResponses(UNAUTHORIZED_401_RESPONSE, OK_200_RESPONSE, createOkPropfindResponse(),
                createOkSearchResponse());

        webDavStore.getPersonalNamespaces();

        List<HttpGeneric> requests = requestCaptor.getAllValues();
        assertEquals(4, requests.size()); // AUTH + 2
        assertEquals("PROPFIND", requests.get(2).getMethod()); //Special Folders
    }

    @Test
    public void getPersonalNamespaces_shouldSetSpecialFolderNames() throws Exception {
        configureHttpResponses(UNAUTHORIZED_401_RESPONSE, OK_200_RESPONSE, createOkPropfindResponse(),
                createOkSearchResponse());

        List<WebDavFolder> folders = webDavStore.getPersonalNamespaces();

        Map<String, FolderType> folderNameToTypeMap = new HashMap<>();
        for (WebDavFolder folder : folders) {
            folderNameToTypeMap.put(folder.getName(), folder.getType());
        }
        assertEquals(FolderType.INBOX, folderNameToTypeMap.get("Inbox"));
        assertEquals(FolderType.REGULAR, folderNameToTypeMap.get("Drafts"));
        assertEquals(FolderType.REGULAR, folderNameToTypeMap.get("Folder2"));
    }

    @Test
    public void getPersonalNamespaces_shouldRequestFolderList() throws Exception {
        configureHttpResponses(UNAUTHORIZED_401_RESPONSE, OK_200_RESPONSE, createOkPropfindResponse(),
                createOkSearchResponse());

        webDavStore.getPersonalNamespaces();

        List<HttpGeneric> requests = requestCaptor.getAllValues();
        assertEquals(4, requests.size()); // AUTH + SPECIALFOLDERS + 1
        assertEquals("SEARCH", requests.get(3).getMethod());
    }

    @Test
    public void getPersonalNamespaces_shouldProvideListOfAllFoldersSentFromResponses() throws Exception {
        configureHttpResponses(UNAUTHORIZED_401_RESPONSE, OK_200_RESPONSE, createOkPropfindResponse(),
                createOkSearchResponse());

        List<WebDavFolder> folders = webDavStore.getPersonalNamespaces();

        List<HttpGeneric> requests = requestCaptor.getAllValues();

        assertEquals(3, folders.size());
    }

    private static BasicHttpResponse createResponse(int statusCode) {
        return new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, statusCode, null));
    }

    private static BasicHttpResponse createOkResponse() {
        return createResponse(200);
    }

    //TODO: Replace XML with actual XML from an Exchange server
    private BasicHttpResponse createOkSearchResponse() throws UnsupportedEncodingException {
        BasicHttpResponse okSearchResponse = createOkResponse();
        HttpEntity searchResponseEntity = new StringEntity("" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "   <D:multistatus xmlns:D=\"DAV:\"\n" +
                "      xmlns:R=\"http://example.org/propschema\">\n" +
                "     <D:response>" +
                "       <D:propstat>\n" +
                "       <uid>Inbox</uid>" +
                "       <href>http://example.org/Exchange/user/Inbox</href>\n" +
                "     </D:propstat></D:response>\n" +
                "     <D:response>" +
                "       <D:propstat>\n" +
                "       <uid>Drafts</uid>" +
                "       <href>http://example.org/Exchange/user/Drafts</href>\n" +
                "     </D:propstat></D:response>\n" +
                "     <D:response>" +
                "       <D:propstat>\n" +
                "       <uid>Folder2</uid>" +
                "       <href>http://example.org/Exchange/user/Folder2</href>\n" +
                "     </D:propstat></D:response>\n" +
                "   </D:multistatus>");
        okSearchResponse.setEntity(searchResponseEntity);

        return okSearchResponse;
    }

    //TODO: Replace XML with actual XML from an Exchange server
    private BasicHttpResponse createOkPropfindResponse() throws UnsupportedEncodingException {
        BasicHttpResponse okPropfindResponse = createOkResponse();
        HttpEntity propfindResponseEntity = new StringEntity("" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<D:multistatus xmlns:D=\"DAV:\" xmlns:e=\"urn:schemas:httpmail:\">\n" +
                "  <D:response><e:inbox>http://example.org/Exchange/user/Inbox</e:inbox></D:response>\n" +
                "</D:multistatus>");
        okPropfindResponse.setEntity(propfindResponseEntity);

        return okPropfindResponse;
    }

    private BasicHttpResponse createOkResponseWithForm() {
        BasicHttpResponse okayResponseWithForm = createOkResponse();
        BasicHttpEntity okayResponseWithFormEntity = new BasicHttpEntity();
        String form = "<form action=\"owaauth.dll\"></form>";
        okayResponseWithFormEntity.setContent(new ByteArrayInputStream(form.getBytes()));
        okayResponseWithForm.setEntity(okayResponseWithFormEntity);
        return okayResponseWithForm;
    }

    private Answer<HttpResponse> createOkResponseWithCookie() {
        return new Answer<HttpResponse>() {
            @Override
            public HttpResponse answer(InvocationOnMock invocation) {
                HttpContext context = (HttpContext) invocation.getArguments()[1];
                if (context.getAttribute(ClientContext.COOKIE_STORE) != null) {
                    BasicCookieStore cookieStore =
                            (BasicCookieStore) context.getAttribute(ClientContext.COOKIE_STORE);
                    BasicClientCookie cookie = new BasicClientCookie("cookie", "meLikeCookie");
                    cookieStore.addCookie(cookie);
                }

                return OK_200_RESPONSE;
            }
        };
    }

    private ServerSettings createServerSettings(ConnectionSecurity connectionSecurity) {
        Map<String, String> extra = WebDavStoreSettings.createExtra(null, null, null, null);
        return new ServerSettings(
                "webdav",
                "webdav.example.org",
                443,
                connectionSecurity,
                AuthType.PLAIN,
                "user",
                "password",
                null,
                extra);
    }

    private WebDavStore createWebDavStore() {
        return new WebDavStore(trustManagerFactory, serverSettings, draftsFolderProvider, mockHttpClientFactory);
    }

    private WebDavStore createWebDavStore(ConnectionSecurity connectionSecurity) {
        ServerSettings serverSettings = createServerSettings(connectionSecurity);
        return new WebDavStore(trustManagerFactory, serverSettings, draftsFolderProvider, mockHttpClientFactory);
    }

    private void configureHttpResponses(HttpResponse... responses) throws IOException {
        requestCaptor = ArgumentCaptor.forClass(HttpGeneric.class);
        OngoingStubbing<HttpResponse> stubbing =
                when(mockHttpClient.executeOverride(requestCaptor.capture(), any(HttpContext.class)));

        for (HttpResponse response : responses) {
            stubbing = stubbing.thenReturn(response);
        }
    }

    private void assertHttpClientUsesHttps(boolean expected) {
        assertEquals(expected, requestCaptor.getValue().getURI().getScheme().startsWith("https"));
    }
}
