package com.fsck.k9.mailstore;


import java.util.Map;

import android.net.Uri;

import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.message.extractors.AttachmentInfoExtractor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;


@SuppressWarnings("unchecked")
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 21)
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
        Multipart multipartBody = new MimeMultipart();
        Part multipartPart = new MimeBodyPart(multipartBody);

        Map<String,Uri> result = AttachmentResolver.buildCidToAttachmentUriMap(attachmentInfoExtractor, multipartPart);

        assertTrue(result.isEmpty());
    }

    @Test
    public void buildCidMap__onMultipartWithEmptyBodyPart__shouldReturnEmptyMap() throws Exception {
        Multipart multipartBody = new MimeMultipart();
        BodyPart bodyPart = spy(new MimeBodyPart());
        Part multipartPart = new MimeBodyPart(multipartBody);
        multipartBody.addBodyPart(bodyPart);

        Map<String,Uri> result = AttachmentResolver.buildCidToAttachmentUriMap(attachmentInfoExtractor, multipartPart);

        assertTrue(result.isEmpty());
    }

    @Test
    public void buildCidMap__onTwoPart__shouldReturnBothUris() throws Exception {
        Multipart multipartBody = new MimeMultipart();
        Part multipartPart = new MimeBodyPart(multipartBody);

        BodyPart subPart1 = new MimeBodyPart();
        BodyPart subPart2 = new MimeBodyPart();
        multipartBody.addBodyPart(subPart1);
        multipartBody.addBodyPart(subPart2);

        when(attachmentInfoExtractor.extractAttachmentInfo(subPart1)).thenReturn(new AttachmentViewInfo(
                        null, null, AttachmentViewInfo.UNKNOWN_SIZE, ATTACHMENT_TEST_URI_1, true, subPart1, true, "cid-1"));
        when(attachmentInfoExtractor.extractAttachmentInfo(subPart2)).thenReturn(new AttachmentViewInfo(
                        null, null, AttachmentViewInfo.UNKNOWN_SIZE, ATTACHMENT_TEST_URI_2, true, subPart2, true, "cid-2"));


        Map<String,Uri> result = AttachmentResolver.buildCidToAttachmentUriMap(attachmentInfoExtractor, multipartPart);


        assertEquals(2, result.size());
        assertEquals(ATTACHMENT_TEST_URI_1, result.get("cid-1"));
        assertEquals(ATTACHMENT_TEST_URI_2, result.get("cid-2"));
    }
}
