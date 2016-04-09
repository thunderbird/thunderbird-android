package com.fsck.k9.mail.internet;


import java.io.ByteArrayInputStream;
import java.io.InputStream;

import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import org.apache.james.mime4j.util.MimeUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 21)
public class MessageExtractorTest {
    private Part part;


    @Before
    public void setUp() throws Exception {
        part = mock(Part.class);
    }

    @Test
    public void getTextFromPart_withNoBody_shouldReturnNull() throws Exception {
        withBody(null);

        String result = MessageExtractor.getTextFromPart(part);

        assertNull(result);
    }

    @Test
    public void getTextFromPart_withTextBody_shouldReturnText() throws Exception {
        withMimeType("text/plain");
        withContentType("UTF-8");
        withBody(createTextBody("Sample text body"));

        String result = MessageExtractor.getTextFromPart(part);

        assertEquals("Sample text body", result);
    }

    @Test
    public void getTextFromPart_withRawDataBodyWithNonText_shouldReturnNull() throws Exception {
        withMimeType("image/jpeg");
        withBody(createRawDataBody());

        String result = MessageExtractor.getTextFromPart(part);

        assertNull(result);
    }

    @Test
    public void getTextFromPart_withExceptionThrownGettingInputStream_shouldReturnNull() throws Exception {
        withMimeType("text/html");
        Body body = createRawDataBody();
        when(body.getInputStream()).thenThrow(new MessagingException("Test"));
        withBody(body);

        String result = MessageExtractor.getTextFromPart(part);

        assertNull(result);
    }

    @Test
    public void getTextFromPart_withUnknownEncoding_shouldThrowRuntimeException() throws Exception {
        withMimeType("text/plain");
        withContentType("UTF-8");
        RawDataBody body = createRawDataBody();
        withBodyContent(body, "Sample text body");
        withEncoding(body, "Unknown encoding");
        withBody(body);

        try {
            MessageExtractor.getTextFromPart(part);
            fail("Expected exception");
        } catch (RuntimeException e) {
            assertEquals("Encoding for RawDataBody not supported: Unknown encoding", e.getMessage());
        }
    }

    @Test
    public void getTextFromPart_withPlainTextWithCharsetInContentTypeRawDataBody_shouldReturnText() throws Exception {
        RawDataBody body = createRawDataBody();
        withMimeType("text/plain");
        withContentType("text/html; charset=UTF-8");
        withBodyContent(body, "Sample text body");
        withBody(body);
        withEncoding(body, MimeUtil.ENC_8BIT);

        String result = MessageExtractor.getTextFromPart(part);

        assertEquals("Sample text body", result);
    }

    @Test
    public void getTextFromPart_withHtmlWithCharsetInContentTypeRawDataBody_shouldReturnHtmlText() throws Exception {
        RawDataBody body = createRawDataBody();
        withMimeType("text/html");
        withContentType("text/html; charset=UTF-8");
        withBodyContent(body, "<html><body>Sample text body</body></html>");
        withBody(body);
        withEncoding(body, MimeUtil.ENC_8BIT);

        String result = MessageExtractor.getTextFromPart(part);

        assertEquals("<html><body>Sample text body</body></html>", result);
    }

    @Test
    public void getTextFromPart_withHtmlWithCharsetInHtmlRawDataBody_shouldReturnHtmlText() throws Exception {
        String bodyText = "<html><head>" +
                "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">" +
                "</head><body>Sample text body</body></html>";
        RawDataBody body = createRawDataBody();
        withMimeType("text/html");
        withContentType("text/html");
        withBodyContent(body, bodyText);
        withBody(body);
        withEncoding(body, MimeUtil.ENC_8BIT);

        String result = MessageExtractor.getTextFromPart(part);

        assertNotNull(result);
        assertEquals(bodyText, result);
    }

    private void withMimeType(String mimeType) {
        when(part.getMimeType()).thenReturn(mimeType);
    }

    private void withContentType(String contentType) {
        when(part.getContentType()).thenReturn(contentType);
    }

    private void withBody(Body body) {
        when(part.getBody()).thenReturn(body);
    }

    private void withEncoding(RawDataBody body, String encoding) {
        when(body.getEncoding()).thenReturn(encoding);
    }

    private void withBodyContent(RawDataBody body, final String text) throws Exception {
        when(body.getInputStream()).then(new Answer<InputStream>() {
            @Override
            public InputStream answer(InvocationOnMock invocation) throws Throwable {
                return createInputStream(text);
            }
        });
    }

    private TextBody createTextBody(String text) {
        TextBody body = mock(TextBody.class);
        when(body.getText()).thenReturn(text);

        return body;
    }

    private InputStream createInputStream(String text) throws Exception {
        return new ByteArrayInputStream(text.getBytes("UTF-8"));
    }

    private RawDataBody createRawDataBody() {
        return mock(RawDataBody.class);
    }
}
