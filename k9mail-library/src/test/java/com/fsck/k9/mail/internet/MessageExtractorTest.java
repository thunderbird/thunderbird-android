package com.fsck.k9.mail.internet;

import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;

import org.apache.james.mime4j.util.MimeUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 21)
public class MessageExtractorTest {
    @Test
    public void getTextFromPart_withNoBody_shouldReturnNull()
            throws MessagingException {
        Part part = mock(Part.class);
        when(part.getBody()).thenReturn(null);

        String content = MessageExtractor.getTextFromPart(part);

        assertNull(content);
    }


    @Test
    public void getTextFromPart_WithTextBody_shouldReturnText()
            throws MessagingException, UnsupportedEncodingException {
        Part part = mock(Part.class);
        TextBody body = mock(TextBody.class);
        when(part.getMimeType()).thenReturn("text/plain");
        when(part.getContentType()).thenReturn("UTF-8");
        when(part.getBody()).thenReturn(body);
        when(body.getText()).thenReturn("Sample text body");

        String content = MessageExtractor.getTextFromPart(part);

        assertEquals("Sample text body", content);
    }

    @Test
    public void getTextFromPart_WithRawDataBodyWithNonText_shouldReturnNull()
            throws MessagingException {
        Part part = mock(Part.class);
        Body body = mock(RawDataBody.class);
        when(part.getMimeType()).thenReturn("image/jpeg");
        when(part.getBody()).thenReturn(body);

        String content = MessageExtractor.getTextFromPart(part);

        assertNull(content);
    }

    @Test
    public void getTextFromPart_WithExceptionThrownGettingInputStream_shouldReturnNull()
            throws MessagingException {
        Part part = mock(Part.class);
        Body body = mock(RawDataBody.class);
        when(part.getMimeType()).thenReturn("text/html");
        when(body.getInputStream()).thenThrow(new MessagingException("Test"));
        when(part.getBody()).thenReturn(body);

        MessageExtractor.getTextFromPart(part);
    }


    @Test(expected = RuntimeException.class)
    public void getTextFromPart_WithUnknownEncoding_shouldThrowRuntimeException()
            throws MessagingException, UnsupportedEncodingException {
        Part part = mock(Part.class);
        RawDataBody body = mock(RawDataBody.class);
        when(part.getMimeType()).thenReturn("text/plain");
        when(part.getContentType()).thenReturn("UTF-8");
        when(body.getInputStream()).thenReturn(new ByteArrayInputStream("Sample text body".getBytes("UTF-8")));
        when(part.getBody()).thenReturn(body);
        when(body.getEncoding()).thenReturn("Unknown encoding");

        MessageExtractor.getTextFromPart(part);
    }

    @Test
    public void getTextFromPart_withPlainTextWithCharsetInContentTypeRawDataBody_shouldReturnText()
            throws MessagingException, UnsupportedEncodingException {
        Part part = mock(Part.class);
        RawDataBody body = mock(RawDataBody.class);
        when(part.getMimeType()).thenReturn("text/plain");
        when(part.getContentType()).thenReturn("text/html; charset=UTF-8");
        when(body.getInputStream()).thenReturn(new ByteArrayInputStream("Sample text body".getBytes("UTF-8")));
        when(part.getBody()).thenReturn(body);
        when(body.getEncoding()).thenReturn(MimeUtil.ENC_8BIT);

        String content = MessageExtractor.getTextFromPart(part);

        assertEquals("Sample text body", content);
    }

    @Test
    public void getTextFromPart_withHtmlWithCharsetInContentTypeRawDataBody_shouldReturnHtmlText()
            throws MessagingException, UnsupportedEncodingException {
        Part part = mock(Part.class);
        RawDataBody body = mock(RawDataBody.class);
        when(part.getMimeType()).thenReturn("text/html");
        when(part.getContentType()).thenReturn("text/html; charset=UTF-8");
        when(body.getInputStream()).thenReturn(new ByteArrayInputStream("<html><body>Sample text body</body></html>".getBytes("UTF-8")));
        when(part.getBody()).thenReturn(body);
        when(body.getEncoding()).thenReturn(MimeUtil.ENC_8BIT);

        String content = MessageExtractor.getTextFromPart(part);

        assertEquals("<html><body>Sample text body</body></html>", content);
    }

    @Test
    public void getTextFromPart_withHtmlWithCharsetInHtmlRawDataBody_shouldReturnHtmlText()
            throws MessagingException, UnsupportedEncodingException {
        Part part = mock(Part.class);
        RawDataBody body = mock(RawDataBody.class);
        when(part.getMimeType()).thenReturn("text/html");
        when(part.getContentType()).thenReturn("text/html");
        when(body.getInputStream()).thenAnswer(new Answer<InputStream>() {
            @Override
            public InputStream answer(InvocationOnMock invocation) throws Throwable {
                return new ByteArrayInputStream(
                    ("<html><head>" +
                            "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">" +
                    "</head><body>Sample text body</body></html>").getBytes("UTF-8"));
            }
        });
        when(part.getBody()).thenReturn(body);
        when(body.getEncoding()).thenReturn(MimeUtil.ENC_8BIT);

        String content = MessageExtractor.getTextFromPart(part);

        assertNotNull(content);
        assertEquals("<html><head>" +
                "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">" +
                "</head><body>Sample text body</body></html>", content);
    }
}
