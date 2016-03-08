package com.fsck.k9.mail.store.webdav;

import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.filter.Base64;
import com.fsck.k9.mail.store.StoreConfig;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.BasicHttpEntity;
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
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import javax.net.ssl.SSLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 21)
public class WebDavStoreTest {

    private static final HttpResponse OK_200_RESPONSE =
            new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, null));
    private static final Answer<HttpResponse> OK_200_RESPONSE_WITH_COOKIE =
            new Answer<HttpResponse>() {
        @Override
        public HttpResponse answer(InvocationOnMock invocation) throws Throwable {
            HttpContext context = (HttpContext) invocation.getArguments()[1];
            if(context.getAttribute(ClientContext.COOKIE_STORE) != null) {
                BasicCookieStore cookieStore = (BasicCookieStore) context.
                        getAttribute(ClientContext.COOKIE_STORE);
                BasicClientCookie cookie = new BasicClientCookie("cookie", "meLikeCookie");
                cookieStore.addCookie(cookie);
            }
            return OK_200_RESPONSE;
        }
    };
    private static final HttpResponse UNAUTHORIZARD_401_RESPONSE =
            new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 401, null));
    private static final HttpResponse SERVER_ERROR_500_RESPONSE =
            new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 500, null));
    @Mock private WebDavHttpClient.WebDavHttpClientFactory mockHttpClientFactory;

    private StoreConfig storeConfig;
    private WebDavStore webDavStore;
    private HttpParams httpParams;

    @Mock
    private WebDavHttpClient mockHttpClient;
    @Mock
    private ClientConnectionManager mockClientConnectionManager;
    @Mock
    private SchemeRegistry mockSchemeRegistry;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        httpParams = new BasicHttpParams();
        when(mockHttpClientFactory.create()).thenReturn(mockHttpClient);
        when(mockHttpClient.getParams()).thenReturn(httpParams);
        when(mockHttpClient.getConnectionManager()).thenReturn(mockClientConnectionManager);
        when(mockClientConnectionManager.getSchemeRegistry()).thenReturn(mockSchemeRegistry);

        storeConfig = createStoreConfig("webdav://user:password@example.org:80");
        webDavStore = new WebDavStore(storeConfig, mockHttpClientFactory);
    }

    private StoreConfig createStoreConfig(String storeUri) {
        StoreConfig storeConfig = mock(StoreConfig.class);
        when(storeConfig.getInboxFolderName()).thenReturn("INBOX");
        when(storeConfig.getStoreUri()).thenReturn(storeUri);
        return storeConfig;
    }

    private boolean clientUsesSSLConnection() throws IOException, MessagingException {
        ArgumentCaptor<HttpGeneric> requestCaptor = ArgumentCaptor.forClass(HttpGeneric.class);
        when(mockHttpClient.executeOverride(requestCaptor.capture(), any(HttpContext.class)))
                .thenReturn(UNAUTHORIZARD_401_RESPONSE).thenReturn(OK_200_RESPONSE);
        webDavStore.checkSettings();
        return requestCaptor.getValue().getURI().getScheme().startsWith("https");
    }

    @Test(expected = MessagingException.class)
    public void should_throw_MessagingException_when_passed_IMAP_store_URI()
    throws MessagingException {
        storeConfig = createStoreConfig ("imap://user:password@imap.example.org");
        webDavStore = new WebDavStore(storeConfig, mockHttpClientFactory);
    }

    @Test
    public void should_support_http_prefixed_webdav_url() throws MessagingException, IOException {
        storeConfig = createStoreConfig ("webdav://user:password@http://server:123456");
        webDavStore = new WebDavStore(storeConfig, mockHttpClientFactory);
        assertFalse(clientUsesSSLConnection());
    }

    @Test
    public void should_provide_insecure_connection_when_passed_webdav_url() throws MessagingException, IOException {
        storeConfig = createStoreConfig ("webdav://user:password@server:123456");
        webDavStore = new WebDavStore(storeConfig, mockHttpClientFactory);
        assertFalse(clientUsesSSLConnection());
    }

    @Test
    public void should_provide_secure_connection_when_passed_webdavSsl_url() throws MessagingException, IOException {
        storeConfig = createStoreConfig ("webdav+ssl://user:password@server:123456");
        webDavStore = new WebDavStore(storeConfig, mockHttpClientFactory);
        assertTrue(clientUsesSSLConnection());
    }

    @Test
    public void should_provide_secure_connection_when_passed_webdavTls_url() throws MessagingException, IOException {
        storeConfig = createStoreConfig ("webdav+tls://user:password@server:123456");
        webDavStore = new WebDavStore(storeConfig, mockHttpClientFactory);
        assertTrue(clientUsesSSLConnection());
    }

    @Test
    public void checkSettings_should_create_initial_connection_and_perform_form_based_auth_if_200_is_returned()
            throws Exception {
        ArgumentCaptor<HttpGeneric> requestCaptor = ArgumentCaptor.forClass(HttpGeneric.class);

        BasicHttpResponse okayResponseWithForm =
                new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, null));
        BasicHttpEntity okayResponseWithFormEntity = new BasicHttpEntity();
        String form = "<form action=\"owaauth.dll\"></form>";
        okayResponseWithFormEntity.setContent(new ByteArrayInputStream(form.getBytes()));
        okayResponseWithForm.setEntity(okayResponseWithFormEntity);

        when(mockHttpClient.executeOverride(requestCaptor.capture(), any(HttpContext.class)))
                .thenReturn(OK_200_RESPONSE)
                .thenReturn(okayResponseWithForm)
                .thenAnswer(OK_200_RESPONSE_WITH_COOKIE)
                .thenReturn(OK_200_RESPONSE);
        webDavStore.checkSettings();
        List<HttpGeneric> requests = requestCaptor.getAllValues();
        assertEquals(4, requests.size());
        assertEquals("GET", requests.get(0).getMethod()); // Checking auth type
        assertEquals("POST", requests.get(1).getMethod()); // Posting form data
        assertEquals("http://example.org:80/exchweb/bin/auth/owaauth.dll", requests.get(1).getURI().toString());
        assertEquals("POST", requests.get(2).getMethod()); // Confirming login
        assertEquals("http://example.org:80/exchweb/bin/auth/owaauth.dll", requests.get(2).getURI().toString());
        assertEquals("GET", requests.get(3).getMethod()); // Getting response
    }

    @Test
    public void checkSettings_should_create_initial_connection_and_perform_basic_auth_if_401_is_returned()
            throws Exception {
        ArgumentCaptor<HttpGeneric> requestCaptor = ArgumentCaptor.forClass(HttpGeneric.class);
        when(mockHttpClient.executeOverride(requestCaptor.capture(), any(HttpContext.class)))
                .thenReturn(UNAUTHORIZARD_401_RESPONSE).thenReturn(OK_200_RESPONSE);
        webDavStore.checkSettings();
        List<HttpGeneric> requests = requestCaptor.getAllValues();
        assertEquals(2, requests.size());
        assertEquals("GET", requests.get(0).getMethod());
        assertEquals("GET", requests.get(1).getMethod());
        assertEquals("Basic " + Base64.encode("user:password"),
                requests.get(1).getHeaders("Authorization")[0].getValue());
    }

    @Test(expected = MessagingException.class)
    public void checkSettings_should_throw_MessagingException_if_unauthorized_following_basic_auth()
            throws Exception {
        ArgumentCaptor<HttpGeneric> requestCaptor = ArgumentCaptor.forClass(HttpGeneric.class);
        when(mockHttpClient.executeOverride(requestCaptor.capture(), any(HttpContext.class)))
                .thenReturn(UNAUTHORIZARD_401_RESPONSE).thenReturn(UNAUTHORIZARD_401_RESPONSE);
        webDavStore.checkSettings();
        assertEquals("GET", requestCaptor.getValue().getMethod());
    }

    @Test(expected = MessagingException.class)
    public void checkSettings_should_throw_MessagingException_if_error_following_basic_auth()
            throws Exception {
        ArgumentCaptor<HttpGeneric> requestCaptor = ArgumentCaptor.forClass(HttpGeneric.class);
        when(mockHttpClient.executeOverride(requestCaptor.capture(), any(HttpContext.class)))
                .thenReturn(UNAUTHORIZARD_401_RESPONSE).thenReturn(SERVER_ERROR_500_RESPONSE);
        webDavStore.checkSettings();
    }

    @Test(expected = CertificateValidationException.class)
    public void checkSettings_should_throw_CertificateValidationException_if_SSLException_thrown()
            throws Exception {
        ArgumentCaptor<HttpGeneric> requestCaptor = ArgumentCaptor.forClass(HttpGeneric.class);
        when(mockHttpClient.executeOverride(requestCaptor.capture(), any(HttpContext.class)))
                .thenThrow(new SSLException("Test"));
        webDavStore.checkSettings();
    }

    @Test
    public void registers_https_scheme_with_registry_when_creating_client() throws IOException, MessagingException {
        when(mockHttpClient.executeOverride(any(HttpGeneric.class), any(HttpContext.class)))
                .thenReturn(UNAUTHORIZARD_401_RESPONSE).thenReturn(OK_200_RESPONSE);
        webDavStore.checkSettings();
        ArgumentCaptor<Scheme> schemeCaptor = ArgumentCaptor.forClass(Scheme.class);
        verify(mockSchemeRegistry).register(schemeCaptor.capture());
        assertEquals("https", schemeCaptor.getValue().getName());
        assertEquals(WebDavSocketFactory.class, schemeCaptor.getValue().getSocketFactory().getClass());
    }

    @Test
    public void getFolder_should_return_WebDavFolder_instance() throws Exception {
        Folder result = webDavStore.getFolder("INBOX");
        assertEquals(WebDavFolder.class, result.getClass());
    }

    @Test
    public void getFolder_calledTwice_shouldReturnFirstInstance() throws Exception {
        String folderName = "Trash";
        Folder webDavFolder = webDavStore.getFolder(folderName);
        Folder result = webDavStore.getFolder(folderName);
        assertEquals(webDavFolder, result);
    }


}
