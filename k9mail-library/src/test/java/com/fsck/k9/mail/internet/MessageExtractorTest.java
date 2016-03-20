package com.fsck.k9.mail.internet;

import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;

import org.apache.james.mime4j.util.MimeUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 21)
public class MessageExtractorTest {
    @Test
    public void should_return_null_withNoBody()
            throws MessagingException {
        Part part = mock(Part.class);
        when(part.getBody()).thenReturn(null);
        MessageExtractor.getTextFromPart(part);
    }

    @Test
    public void should_return_null_withNonTextPart()
            throws MessagingException {
        Part part = mock(Part.class);
        Body body = mock(RawDataBody.class);
        when(part.getMimeType()).thenReturn("image/jpeg");
        when(part.getBody()).thenReturn(body);
        MessageExtractor.getTextFromPart(part);
    }

    @Test
    public void should_return_null_if_decodeBody_fails_because_the_file_doesnt_exist()
            throws MessagingException {
        Part part = mock(Part.class);
        Body body = mock(RawDataBody.class);
        when(part.getMimeType()).thenReturn("text/html");
        when(body.getInputStream()).thenThrow(new MessagingException("Test"));
        when(part.getBody()).thenReturn(body);
        MessageExtractor.getTextFromPart(part);
    }

    @Test(expected = RuntimeException.class)
    public void should_throw_runtime_exception_for_unknown_encoding()
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
    public void should_return_text_from_plaintext()
            throws MessagingException, UnsupportedEncodingException {
        Part part = mock(Part.class);
        RawDataBody body = mock(RawDataBody.class);
        when(part.getMimeType()).thenReturn("text/plain");
        when(part.getContentType()).thenReturn("UTF-8");
        when(body.getInputStream()).thenReturn(new ByteArrayInputStream("Sample text body".getBytes("UTF-8")));
        when(part.getBody()).thenReturn(body);
        when(body.getEncoding()).thenReturn(MimeUtil.ENC_8BIT);
        String content = MessageExtractor.getTextFromPart(part);
        assertEquals("Sample text body", content);
    }
}
