package com.fsck.k9.mailstore;


import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import android.net.Uri;

import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import com.fsck.k9.message.extractors.AttachmentInfoExtractor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@SuppressWarnings("unchecked")
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 21)
public class AttachmentResolverTest {
    public static final Uri ATTACHMENT_TEST_URI_1 = Uri.parse("uri://test/1");
    public static final Uri ATTACHMENT_TEST_URI_2 = Uri.parse("uri://test/2");


    @Test
    public void buildCidMap__onPartWithNoBody__shouldReturnEmptyMap() throws Exception {
        AttachmentInfoExtractor attachmentInfoExtractor = mock(AttachmentInfoExtractor.class);
        Part part = mock(Part.class);
        when(part.getContentId()).thenReturn(null);

        Map<String,Uri> result = AttachmentResolver.buildCidToAttachmentUriMap(attachmentInfoExtractor, part);

        assertTrue(result.isEmpty());
    }

    @Test
    public void buildCidMap__onMultipartWithNoParts__shouldReturnEmptyMap() throws Exception {
        AttachmentInfoExtractor attachmentInfoExtractor = mock(AttachmentInfoExtractor.class);
        Part multipartPart = mock(Part.class);
        Multipart multipartBody = mock(Multipart.class);
        when(multipartPart.getBody()).thenReturn(multipartBody);
        when(multipartBody.getBodyParts()).thenReturn(Collections.EMPTY_LIST);

        Map<String,Uri> result = AttachmentResolver.buildCidToAttachmentUriMap(attachmentInfoExtractor, multipartPart);

        assertTrue(result.isEmpty());
    }

    @Test
    public void buildCidMap__onMultipartWithEmptyBodyPart__shouldReturnEmptyMap() throws Exception {
        AttachmentInfoExtractor attachmentInfoExtractor = mock(AttachmentInfoExtractor.class);
        Part multipartPart = mock(Part.class);
        Multipart multipartBody = mock(Multipart.class);
        BodyPart bodyPart = mock(BodyPart.class);

        when(multipartPart.getBody()).thenReturn(multipartBody);
        when(multipartBody.getBodyParts()).thenReturn(Collections.singletonList(bodyPart));
        when(bodyPart.getContentId()).thenReturn(null);


        Map<String,Uri> result = AttachmentResolver.buildCidToAttachmentUriMap(attachmentInfoExtractor, multipartPart);


        verify(bodyPart).getContentId();
        assertTrue(result.isEmpty());
    }

    @Test
    public void buildCidMap__onTwoPart__shouldReturnBothUris() throws Exception {
        AttachmentInfoExtractor attachmentInfoExtractor = mock(AttachmentInfoExtractor.class);
        Part multipartPart = mock(Part.class);
        Multipart multipartBody = mock(Multipart.class);
        BodyPart bodyPart = mock(BodyPart.class);

        when(multipartPart.getBody()).thenReturn(multipartBody);
        when(multipartBody.getBodyParts()).thenReturn(Collections.singletonList(bodyPart));

        BodyPart subPart1 = mock(BodyPart.class);
        BodyPart subPart2 = mock(BodyPart.class);
        when(subPart1.getContentId()).thenReturn("cid-1");
        when(subPart2.getContentId()).thenReturn("cid-2");

        when(attachmentInfoExtractor.extractAttachmentInfo(subPart1)).thenReturn(new AttachmentViewInfo(
                null, null, AttachmentViewInfo.UNKNOWN_SIZE, ATTACHMENT_TEST_URI_1, true, subPart1));
        when(attachmentInfoExtractor.extractAttachmentInfo(subPart2)).thenReturn(new AttachmentViewInfo(
                null, null, AttachmentViewInfo.UNKNOWN_SIZE, ATTACHMENT_TEST_URI_2, true, subPart2));

        when(multipartBody.getBodyParts()).thenReturn(Arrays.asList(subPart1, subPart2));

        Map<String,Uri> result = AttachmentResolver.buildCidToAttachmentUriMap(attachmentInfoExtractor, multipartPart);

        assertEquals(2, result.size());
        assertEquals(ATTACHMENT_TEST_URI_1, result.get("cid-1"));
        assertEquals(ATTACHMENT_TEST_URI_2, result.get("cid-2"));
    }

}