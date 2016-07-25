package com.fsck.k9.mail;


import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeHeader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 21)
public class FancyPartTest {
    private static final String TEST_MIME_TYPE = "application/test";
    private static final String TEST_CONTENT_TYPE_NAME = "testname";
    public static final String TEST_CHARSET = "utf-123";
    private static final String TEST_DISPOSITION_FILENAME = "filename";
    private static final String TEST_BOUNDARY = "frontier";
    private static final String TEST_PROTOCOL = "testprotocol";
    private static final Long TEST_SIZE = 123L;

    private static final String TEST_CONTENT_TYPE_HEADER = String.format(
            "%s;\r\n boundary=\"%s\";\r\n  bogus=\"boges\"; name=\"%s\"; protocol=\"%s\"; charset=\"%s\"",
            TEST_MIME_TYPE, TEST_BOUNDARY, TEST_CONTENT_TYPE_NAME, TEST_PROTOCOL, TEST_CHARSET);
    private static final String TEST_DISPOSITION_HEADER_ATTACHMENT = String.format(
            "attachment;\r\n  filename=\"%s\";\r\n  bogus=\"boges\";  size=\"%s\"", TEST_DISPOSITION_FILENAME, TEST_SIZE);
    private static final String TEST_DISPOSITION_HEADER_NEGATIVE_SIZE = String.format(
            "attachment;\r\n  size=\"-%s\"", TEST_SIZE);
    private static final String TEST_DISPOSITION_HEADER_INLINE = String.format(
            "inline;\r\n  filename=\"%s\";\r\n  bogus=\"boges\";", TEST_DISPOSITION_FILENAME);
    private static final String TEST_ENCODING = "test-encoding";
    private static final String TEST_CONTENT_ID = "content-id";


    private MimeBodyPart part;


    @Before
    public void setUp() throws Exception {
        part = new MimeBodyPart();
    }

    @Test
    public void testGetMimeType() throws Exception {
        assertNull(FancyPart.from(part).getMimeType());
        part.setHeader(MimeHeader.HEADER_CONTENT_TYPE, TEST_CONTENT_TYPE_HEADER);
        assertEquals(TEST_MIME_TYPE, FancyPart.from(part).getMimeType());
    }

    @Test
    public void testGetBoundary() throws Exception {
        assertNull(FancyPart.from(part).getBoundary());
        part.setHeader(MimeHeader.HEADER_CONTENT_TYPE, TEST_CONTENT_TYPE_HEADER);
        assertEquals(TEST_BOUNDARY, FancyPart.from(part).getBoundary());
    }

    @Test
    public void testGetContentTypeName() throws Exception {
        assertNull(FancyPart.from(part).getContentTypeName());
        part.setHeader(MimeHeader.HEADER_CONTENT_TYPE, TEST_CONTENT_TYPE_HEADER);
        assertEquals(TEST_CONTENT_TYPE_NAME, FancyPart.from(part).getContentTypeName());
    }

    @Test
    public void testIsMultipart() throws Exception {
        assertFalse(FancyPart.from(part).isMultipart());
        part.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "multipart/mixed");
        assertTrue(FancyPart.from(part).isMultipart());
    }

    @Test
    public void testIsDispositionInline__withInline() throws Exception {
        assertTrue(FancyPart.from(part).isDispositionInline());
        part.setHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, TEST_DISPOSITION_HEADER_INLINE);
        assertTrue(FancyPart.from(part).isDispositionInline());
        assertFalse(FancyPart.from(part).isDispositionAttachment());
    }

    @Test
    public void testIsDispositionInline__withAttachment() throws Exception {
        assertTrue(FancyPart.from(part).isDispositionInline());
        part.setHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, TEST_DISPOSITION_HEADER_ATTACHMENT);
        assertTrue(FancyPart.from(part).isDispositionAttachment());
        assertFalse(FancyPart.from(part).isDispositionInline());
    }

    @Test
    public void testGetDispositionFilename() throws Exception {
        assertNull(FancyPart.from(part).getDispositionFilename());
        part.setHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, TEST_DISPOSITION_HEADER_ATTACHMENT);
        assertEquals(TEST_DISPOSITION_FILENAME, FancyPart.from(part).getDispositionFilename());
    }

    @Test
    public void testGetPartName() throws Exception {
        assertNull(FancyPart.from(part).getPartName());

        part.setHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, TEST_DISPOSITION_HEADER_ATTACHMENT);
        assertEquals(TEST_DISPOSITION_FILENAME, FancyPart.from(part).getPartName());

        // content type header has priority, so if we set that we should get a different result
        part.setHeader(MimeHeader.HEADER_CONTENT_TYPE, TEST_CONTENT_TYPE_HEADER);
        assertEquals(TEST_CONTENT_TYPE_NAME, FancyPart.from(part).getPartName());
    }

    @Test
    public void testGetDispositionSize() throws Exception {
        assertNull(FancyPart.from(part).getDispositionSize());

        part.setHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, TEST_DISPOSITION_HEADER_ATTACHMENT);
        assertEquals(TEST_SIZE, FancyPart.from(part).getDispositionSize());

        part.setHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, TEST_DISPOSITION_HEADER_NEGATIVE_SIZE);
        assertNull(FancyPart.from(part).getDispositionSize());
    }

    @Test
    public void testGetContentTransferEncoding() throws Exception {
        assertNull(FancyPart.from(part).getContentTransferEncoding());
        part.setHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING, TEST_ENCODING);
        assertEquals(TEST_ENCODING, FancyPart.from(part).getContentTransferEncoding());
    }

    @Test
    public void testGetContentId() throws Exception {
        assertNull(FancyPart.from(part).getContentId());
        part.setHeader(MimeHeader.HEADER_CONTENT_ID, TEST_CONTENT_ID);
        assertEquals(TEST_CONTENT_ID, FancyPart.from(part).getContentId());
    }

    @Test
    public void testGetCharset() throws Exception {
        assertNull(FancyPart.from(part).getCharset());
        part.setHeader(MimeHeader.HEADER_CONTENT_TYPE, TEST_CONTENT_TYPE_HEADER);
        assertEquals(TEST_CHARSET, FancyPart.from(part).getCharset());
    }

    @Test
    public void testGetContentTypeProtocol() throws Exception {
        part.setHeader(MimeHeader.HEADER_CONTENT_TYPE, TEST_CONTENT_TYPE_HEADER);
        assertEquals(TEST_PROTOCOL, FancyPart.from(part).getContentTypeProtocol());
    }

    @Test
    public void testIsMimeType() throws Exception {
        part.setHeader(MimeHeader.HEADER_CONTENT_TYPE, TEST_CONTENT_TYPE_HEADER);
        assertTrue(FancyPart.from(part).isMimeType(TEST_MIME_TYPE));
    }

    @Test
    public void testIsMatchingMimeType() throws Exception {
        part.setHeader(MimeHeader.HEADER_CONTENT_TYPE, TEST_CONTENT_TYPE_HEADER);
        assertTrue(FancyPart.from(part).isMatchingMimeType("application/*"));
    }

    @Test
    public void testIsMimeTypeAnyOf() throws Exception {
        part.setHeader(MimeHeader.HEADER_CONTENT_TYPE, TEST_CONTENT_TYPE_HEADER);

        assertFalse(FancyPart.from(part).isMimeTypeAnyOf("fake1", "fake2"));
        assertTrue(FancyPart.from(part).isMimeTypeAnyOf("fake1", TEST_MIME_TYPE, "fake2"));
    }

    @Test
    public void testGetWrappedPart() throws Exception {
        assertSame(part, FancyPart.from(part).getWrappedPart());
    }
}