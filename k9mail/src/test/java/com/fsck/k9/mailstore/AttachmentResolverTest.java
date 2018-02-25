package com.fsck.k9.mailstore;


import java.util.Map;

import android.net.Uri;

import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.message.extractors.AttachmentInfoExtractor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@SuppressWarnings("unchecked")
@RunWith(RobolectricTestRunner.class)
public class AttachmentResolverTest {
    public static final Uri ATTACHMENT_TEST_URI_1 = Uri.parse("uri://test/1");
    public static final Uri ATTACHMENT_TEST_URI_2 = Uri.parse("uri://test/2");


    private AttachmentInfoExtractor attachmentInfoExtractor;


    @Before
    public void setUp() throws Exception {
        attachmentInfoExtractor = mock(AttachmentInfoExtractor.class);
    }

    @Test
    public void buildCidMap__onPartWithNoBody__shouldReturnEmptyMap() throws Exception {
        Part part = new MimeBodyPart();

        Map<String,Uri> result = AttachmentResolver.buildCidToAttachmentUriMap(attachmentInfoExtractor, part);

        assertTrue(result.isEmpty());
    }

    @Test
    public void buildCidMap__onMultipartWithNoParts__shouldReturnEmptyMap() throws Exception {
        Multipart multipartBody = MimeMultipart.newInstance();
        Part multipartPart = new MimeBodyPart(multipartBody);

        Map<String,Uri> result = AttachmentResolver.buildCidToAttachmentUriMap(attachmentInfoExtractor, multipartPart);

        assertTrue(result.isEmpty());
    }

    @Test
    public void buildCidMap__onMultipartWithEmptyBodyPart__shouldReturnEmptyMap() throws Exception {
        Multipart multipartBody = MimeMultipart.newInstance();
        BodyPart bodyPart = spy(new MimeBodyPart());
        Part multipartPart = new MimeBodyPart(multipartBody);
        multipartBody.addBodyPart(bodyPart);

        Map<String,Uri> result = AttachmentResolver.buildCidToAttachmentUriMap(attachmentInfoExtractor, multipartPart);

        verify(bodyPart).getContentId();
        assertTrue(result.isEmpty());
    }

    @Test
    public void buildCidMap__onTwoPart__shouldReturnBothUris() throws Exception {
        Multipart multipartBody = MimeMultipart.newInstance();
        Part multipartPart = new MimeBodyPart(multipartBody);

        BodyPart subPart1 = new MimeBodyPart();
        BodyPart subPart2 = new MimeBodyPart();
        multipartBody.addBodyPart(subPart1);
        multipartBody.addBodyPart(subPart2);

        subPart1.setHeader(MimeHeader.HEADER_CONTENT_ID, "cid-1");
        subPart2.setHeader(MimeHeader.HEADER_CONTENT_ID, "cid-2");

        when(attachmentInfoExtractor.extractAttachmentInfo(subPart1)).thenReturn(new AttachmentViewInfo(
                        null, null, AttachmentViewInfo.UNKNOWN_SIZE, ATTACHMENT_TEST_URI_1, false, subPart1, true));
        when(attachmentInfoExtractor.extractAttachmentInfo(subPart2)).thenReturn(new AttachmentViewInfo(
                        null, null, AttachmentViewInfo.UNKNOWN_SIZE, ATTACHMENT_TEST_URI_2, false, subPart2, true));


        Map<String,Uri> result = AttachmentResolver.buildCidToAttachmentUriMap(attachmentInfoExtractor, multipartPart);


        assertEquals(2, result.size());
        assertEquals(ATTACHMENT_TEST_URI_1, result.get("cid-1"));
        assertEquals(ATTACHMENT_TEST_URI_2, result.get("cid-2"));
    }
}
