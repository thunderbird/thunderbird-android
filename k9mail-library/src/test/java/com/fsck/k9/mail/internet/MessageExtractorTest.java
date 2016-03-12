package com.fsck.k9.mail.internet;

import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;

import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MessageExtractorTest {

    @Test(expected = MessagingException.class)
    public void should_throw_exception_if_decodeBody_fails_because_the_file_doesnt_exist()
            throws MessagingException {
        Part part = mock(Part.class);
        Body body = mock(RawDataBody.class);
        when(body.getInputStream()).thenThrow(new MessagingException("Test"));
        when(part.getBody()).thenReturn(body);
        MessageExtractor.getTextFromPart(part);
    }
}
