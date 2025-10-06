package com.fsck.k9.mail.internet;


import com.fsck.k9.mail.Body;
import net.thunderbird.core.common.exception.MessagingException;
import com.fsck.k9.mailstore.BinaryMemoryBody;
import net.thunderbird.core.logging.legacy.Log;
import net.thunderbird.core.logging.testing.TestLogger;
import org.apache.james.mime4j.util.MimeUtil;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class MessageExtractorTest {
    private MimeBodyPart part;


    @Before
    public void setUp() throws Exception {
        Log.logger = new TestLogger();
        part = new MimeBodyPart();
    }

    @Test
    public void getTextFromPart_withNoBody_shouldReturnNull() throws Exception {
        part.setBody(null);

        String result = MessageExtractor.getTextFromPart(part);

        assertNull(result);
    }

    @Test
    public void getTextFromPart_withTextBody_shouldReturnText() throws Exception {
        part.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "text/plain; charset=utf-8");
        BinaryMemoryBody body = new BinaryMemoryBody("Sample text body".getBytes(), MimeUtil.ENC_8BIT);
        part.setBody(body);

        String result = MessageExtractor.getTextFromPart(part);

        assertEquals("Sample text body", result);
    }

    @Test
    public void getTextFromPart_withRawDataBodyWithNonText_shouldReturnNull() throws Exception {
        part.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "image/jpeg");
        BinaryMemoryBody body = new BinaryMemoryBody("Sample text body".getBytes(), MimeUtil.ENC_8BIT);
        part.setBody(body);

        String result = MessageExtractor.getTextFromPart(part);

        assertNull(result);
    }

    @Test
    public void getTextFromPart_withExceptionThrownGettingInputStream_shouldReturnNull() throws Exception {
        part.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "text/html");
        Body body = mock(Body.class);
        when(body.getInputStream()).thenThrow(new MessagingException("Test"));
        part.setBody(body);

        String result = MessageExtractor.getTextFromPart(part);

        assertNull(result);
    }

    @Test
    public void getTextFromPart_withUnknownEncoding_shouldReturnUnmodifiedBodyContents() throws Exception {
        part.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "text/plain");
        String bodyText = "Sample text body";
        BinaryMemoryBody body = new BinaryMemoryBody(bodyText.getBytes(), "unknown encoding");
        part.setBody(body);

        String result = MessageExtractor.getTextFromPart(part);

        assertEquals(bodyText, result);
    }

    @Test
    public void getTextFromPart_withPlainTextWithCharsetInContentTypeRawDataBody_shouldReturnText() throws Exception {
        part.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "text/plain; charset=UTF-8");
        BinaryMemoryBody body = new BinaryMemoryBody("Sample text body".getBytes(), MimeUtil.ENC_8BIT);
        part.setBody(body);

        String result = MessageExtractor.getTextFromPart(part);

        assertEquals("Sample text body", result);
    }

    @Test
    public void getTextFromPart_withHtmlWithCharsetInContentTypeRawDataBody_shouldReturnHtmlText() throws Exception {
        part.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "text/html; charset=UTF-8");
        BinaryMemoryBody body = new BinaryMemoryBody(
                "<html><body>Sample text body</body></html>".getBytes(), MimeUtil.ENC_8BIT);
        part.setBody(body);

        String result = MessageExtractor.getTextFromPart(part);

        assertEquals("<html><body>Sample text body</body></html>", result);
    }

    @Test
    public void getTextFromPart_withHtmlWithCharsetInHtmlRawDataBody_shouldReturnHtmlText() throws Exception {
        String bodyText = "<html><head>" +
                "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">" +
                "</head><body>Sample text body</body></html>";
        BinaryMemoryBody body = new BinaryMemoryBody(bodyText.getBytes(), MimeUtil.ENC_8BIT);
        part.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "text/html");
        part.setBody(body);

        String result = MessageExtractor.getTextFromPart(part);

        assertNotNull(result);
        assertEquals(bodyText, result);
    }
}
