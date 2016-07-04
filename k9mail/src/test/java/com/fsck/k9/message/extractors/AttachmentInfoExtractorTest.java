package com.fsck.k9.message.extractors;


import android.net.Uri;

import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mailstore.AttachmentViewInfo;
import com.fsck.k9.mailstore.LocalBodyPart;
import com.fsck.k9.mailstore.ProvidedTempFileBody;
import com.fsck.k9.provider.AttachmentProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 21)
public class AttachmentInfoExtractorTest {
    public static final Uri TEST_URI = Uri.parse("uri://test");
    public static final String TEST_MIME_TYPE = "text/plain";
    public static final long TEST_SIZE = 123L;
    public static final String TEST_ACCOUNT_UUID = "uuid";
    public static final long TEST_ID = 234L;
    public static final String[] TEST_CONTENT_ID = new String[] { "test-content-id" };


    private AttachmentInfoExtractor attachmentInfoExtractor;


    @Before
    public void setUp() throws Exception {
        attachmentInfoExtractor = AttachmentInfoExtractor.getInstance();
    }

    @Test(expected = IllegalArgumentException.class)
    public void extractInfo__withGenericPart_shouldThrow() throws Exception {
        Part part = mock(Part.class);

        attachmentInfoExtractor.extractAttachmentInfo(part);
    }

    @Test
    public void extractInfo__fromLocalBodyPart__shouldReturnProvidedValues() throws Exception {
        LocalBodyPart part = mock(LocalBodyPart.class);
        when(part.getId()).thenReturn(TEST_ID);
        when(part.getMimeType()).thenReturn(TEST_MIME_TYPE);
        when(part.getSize()).thenReturn(TEST_SIZE);
        when(part.getAccountUuid()).thenReturn(TEST_ACCOUNT_UUID);

        AttachmentViewInfo attachmentViewInfo = attachmentInfoExtractor.extractAttachmentInfo(part);

        assertEquals(AttachmentProvider.getAttachmentUri(TEST_ACCOUNT_UUID, TEST_ID), attachmentViewInfo.uri);
        assertEquals(TEST_SIZE, attachmentViewInfo.size);
        assertEquals(TEST_MIME_TYPE, attachmentViewInfo.mimeType);
    }

    @Test
    public void extractInfoForDb__withNoHeaders__shouldReturnEmptyValues() throws Exception {
        Part part = mock(Part.class);

        AttachmentViewInfo attachmentViewInfo = attachmentInfoExtractor.extractAttachmentInfoForDatabase(part);

        assertEquals(Uri.EMPTY, attachmentViewInfo.uri);
        assertEquals(AttachmentViewInfo.UNKNOWN_SIZE, attachmentViewInfo.size);
        assertEquals("noname", attachmentViewInfo.displayName);
        assertNull(attachmentViewInfo.mimeType);
        assertFalse(attachmentViewInfo.firstClassAttachment);
    }

    @Test
    public void extractInfoForDb__withTextMimeType__shouldReturnTxtExtension() throws Exception {
        Part part = mock(Part.class);
        when(part.getMimeType()).thenReturn("text/plain");

        AttachmentViewInfo attachmentViewInfo = attachmentInfoExtractor.extractAttachmentInfoForDatabase(part);

        // MimeUtility.getExtensionByMimeType("text/plain"); -> "txt"
        assertEquals("noname.txt", attachmentViewInfo.displayName);
        assertEquals("text/plain", attachmentViewInfo.mimeType);
    }

    @Test
    public void extractInfoForDb__withContentTypeAndName__shouldReturnNamedFirstClassAttachment() throws Exception {
        Part part = mock(Part.class);
        when(part.getMimeType()).thenReturn(TEST_MIME_TYPE);
        when(part.getContentType()).thenReturn(TEST_MIME_TYPE + "; name=\"filename.ext\"");

        AttachmentViewInfo attachmentViewInfo = attachmentInfoExtractor.extractAttachmentInfoForDatabase(part);

        assertEquals(Uri.EMPTY, attachmentViewInfo.uri);
        assertEquals(TEST_MIME_TYPE, attachmentViewInfo.mimeType);
        assertEquals("filename.ext", attachmentViewInfo.displayName);
        assertTrue(attachmentViewInfo.firstClassAttachment);
    }

    @Test
    public void extractInfoForDb__withContentTypeAndEncodedWordName__shouldReturnDecodedName() throws Exception {
        Part part = mock(Part.class);
        when(part.getContentType()).thenReturn(TEST_MIME_TYPE + "; name=\"=?ISO-8859-1?Q?Sm=F8rrebr=F8d?=\"");

        AttachmentViewInfo attachmentViewInfo = attachmentInfoExtractor.extractAttachmentInfoForDatabase(part);

        assertEquals("Smørrebrød", attachmentViewInfo.displayName);
    }

    @Test
    public void extractInfoForDb__wWithDispositionAttach__shouldReturnNamedFirstClassAttachment() throws Exception {
        Part part = mock(Part.class);
        when(part.getDisposition()).thenReturn("attachment" + "; filename=\"filename.ext\"; meaningless=\"dummy\"");

        AttachmentViewInfo attachmentViewInfo = attachmentInfoExtractor.extractAttachmentInfoForDatabase(part);

        assertEquals(Uri.EMPTY, attachmentViewInfo.uri);
        assertEquals("filename.ext", attachmentViewInfo.displayName);
        assertTrue(attachmentViewInfo.firstClassAttachment);
    }

    @Test
    public void extractInfoForDb__withDispositionInlineAndContentId__shouldReturnNotFirstClassAttachment()
            throws Exception {
        Part part = mock(Part.class);
        when(part.getHeader(MimeHeader.HEADER_CONTENT_ID)).thenReturn(TEST_CONTENT_ID);
        when(part.getDisposition()).thenReturn("inline" + ";\n  filename=\"filename.ext\";\n  meaningless=\"dummy\"");

        AttachmentViewInfo attachmentViewInfo = attachmentInfoExtractor.extractAttachmentInfoForDatabase(part);

        assertFalse(attachmentViewInfo.firstClassAttachment);
    }

    @Test
    public void extractInfoForDb__withDispositionSizeParam__shouldReturnThatSize() throws Exception {
        Part part = mock(Part.class);
        when(part.getDisposition()).thenReturn("doesntmatter" + ";\n  size=\"" + TEST_SIZE + "\"");

        AttachmentViewInfo attachmentViewInfo = attachmentInfoExtractor.extractAttachmentInfoForDatabase(part);

        assertEquals(TEST_SIZE, attachmentViewInfo.size);
    }

    @Test
    public void extractInfoForDb__withDispositionInvalidSizeParam__shouldReturnUnknownSize() throws Exception {
        Part part = mock(Part.class);
        when(part.getDisposition()).thenReturn("doesntmatter" + "; size=\"notanint\"");

        AttachmentViewInfo attachmentViewInfo = attachmentInfoExtractor.extractAttachmentInfoForDatabase(part);

        assertEquals(AttachmentViewInfo.UNKNOWN_SIZE, attachmentViewInfo.size);
    }

    @Test
    public void extractInfo__withProvidedTempFileBody() throws Exception {
        ProvidedTempFileBody body = mock(ProvidedTempFileBody.class);
        Part part = mock(Part.class);
        when(part.getBody()).thenReturn(body);
        when(part.getMimeType()).thenReturn(TEST_MIME_TYPE);

        when(body.getSize()).thenReturn(TEST_SIZE);
        when(body.getProviderUri(TEST_MIME_TYPE)).thenReturn(TEST_URI);

        AttachmentViewInfo attachmentViewInfo = attachmentInfoExtractor.extractAttachmentInfo(part);

        assertEquals(TEST_URI, attachmentViewInfo.uri);
        assertEquals(TEST_SIZE, attachmentViewInfo.size);
        assertEquals(TEST_MIME_TYPE, attachmentViewInfo.mimeType);
        assertFalse(attachmentViewInfo.firstClassAttachment);
    }
}