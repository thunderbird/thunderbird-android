package com.fsck.k9.mail.internet;


import com.fsck.k9.mail.Body;
import net.thunderbird.core.common.exception.MessagingException;
import com.fsck.k9.mailstore.BinaryMemoryBody;
import net.thunderbird.core.logging.legacy.Log;
import net.thunderbird.core.logging.testing.TestLogger;
import org.apache.james.mime4j.util.MimeUtil;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

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

    /**
     * Regression test: ISO-2022-JP body with QP encoding.
     *
     * "テスト" in ISO-2022-JP:
     *   ESC$B (0x1B 0x24 0x42) = switch to JIS X 0208
     *   テ = 0x25 0x46, ス = 0x25 0x39, ト = 0x25 0x48
     *   ESC(B (0x1B 0x28 0x42) = switch back to ASCII
     *
     * QP-encoded: ESC is written as =1B; $,B,(,% are printable ASCII and left as-is.
     * Android's ICU4J ISO-2022-JP decoder can silently mishandle this, showing "$B" and "(B"
     * as literal text. We bypass the platform decoder with Iso2022JpToShiftJisInputStream.
     */
    @Test
    public void getTextFromPart_withIso2022JpQuotedPrintable_shouldDecodeToJapanese() throws Exception {
        // QP-encoded "テスト" in ISO-2022-JP: =1B$B%F%9%H=1B(B
        byte[] qpBytes = "=1B$B%F%9%H=1B(B".getBytes(StandardCharsets.US_ASCII);
        part.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "text/plain; charset=iso-2022-jp");
        BinaryMemoryBody body = new BinaryMemoryBody(qpBytes, MimeUtil.ENC_QUOTED_PRINTABLE);
        part.setBody(body);

        String result = MessageExtractor.getTextFromPart(part);

        assertEquals("テスト", result);
    }

    /**
     * Regression test: multi-line ISO-2022-JP body with QP soft line breaks.
     * Each line re-emits ESC$B because hard line breaks reset to ASCII in ISO-2022-JP.
     */
    @Test
    public void getTextFromPart_withIso2022JpQuotedPrintableMultiLine_shouldDecodeToJapanese() throws Exception {
        // Line 1: "テスト", Line 2: "テスト" — each line wraps with ESC(B ... ESC$B
        String qpBody = "=1B$B%F%9%H=1B(B\r\n=1B$B%F%9%H=1B(B";
        byte[] qpBytes = qpBody.getBytes(StandardCharsets.US_ASCII);
        part.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "text/plain; charset=iso-2022-jp");
        BinaryMemoryBody body = new BinaryMemoryBody(qpBytes, MimeUtil.ENC_QUOTED_PRINTABLE);
        part.setBody(body);

        String result = MessageExtractor.getTextFromPart(part);

        assertNotNull(result);
        assertEquals("テスト\r\nテスト", result);
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

    /**
     * Regression test: forwarded ISO-2022-JP message with no charset in Content-Type.
     *
     * Japanese feature phones and carrier webmail systems often omit "charset=iso-2022-jp"
     * from the Content-Type header.  Without auto-detection the body defaults to US-ASCII and
     * the ESC byte is silently dropped, leaving the literal "$B" escape sequence remnants
     * visible (e.g. "$BJIC...").
     */
    @Test
    public void getTextFromPart_withIso2022Jp7bitNoCharset_shouldAutoDetectAndDecode() throws Exception {
        // Raw 7-bit ISO-2022-JP bytes for "テスト" — no QP encoding, no charset header
        byte[] raw = new byte[] {
            0x1B, '$', 'B',          // ESC $ B  → switch to JIS X 0208
            0x25, 0x46,              // テ
            0x25, 0x39,              // ス
            0x25, 0x48,              // ト
            0x1B, '(', 'B'           // ESC ( B  → switch back to ASCII
        };
        part.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "text/plain");   // no charset!
        BinaryMemoryBody body = new BinaryMemoryBody(raw, MimeUtil.ENC_7BIT);
        part.setBody(body);

        String result = MessageExtractor.getTextFromPart(part);

        assertEquals("テスト", result);
    }

    /**
     * Regression test: forwarded ISO-2022-JP message with QP encoding and no charset.
     * This is the most common form: the =1B escape is QP-encoded, charset is absent.
     */
    @Test
    public void getTextFromPart_withIso2022JpQuotedPrintableNoCharset_shouldAutoDetectAndDecode() throws Exception {
        byte[] qpBytes = "=1B$B%F%9%H=1B(B".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        part.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "text/plain");   // no charset!
        BinaryMemoryBody body = new BinaryMemoryBody(qpBytes, MimeUtil.ENC_QUOTED_PRINTABLE);
        part.setBody(body);

        String result = MessageExtractor.getTextFromPart(part);

        assertEquals("テスト", result);
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
