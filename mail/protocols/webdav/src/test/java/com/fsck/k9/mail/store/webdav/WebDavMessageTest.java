package com.fsck.k9.mail.store.webdav;


import com.fsck.k9.mail.MessagingException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class WebDavMessageTest {

    private WebDavMessage message;
    @Mock
    private WebDavFolder mockFolder;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        when(mockFolder.getServerId()).thenReturn("Inbox");
        when(mockFolder.getUrl()).thenReturn("http://example.org/Inbox");
        message = new WebDavMessage("message1", mockFolder);
    }

    @Test
    public void setUrl_tests() throws MessagingException {
        message.setUrl("message.eml");
        assertEquals("http://example.org/Inbox/message.eml", message.getUrl());
        message.setUrl("mes sage.eml");
        assertEquals("http://example.org/Inbox/mes%20sage.eml", message.getUrl());
        message.setUrl("/message.eml");
        assertEquals("http://example.org/Inbox/message.eml", message.getUrl());
        message.setUrl("http://example.com/Inbox/message.eml");
        assertEquals("http://example.com/Inbox/message.eml", message.getUrl());
        message.setUrl("mes%20sage.eml");
        assertEquals("http://example.org/Inbox/mes%20sage.eml", message.getUrl());
        message.setUrl("sub%20folder/mes%20sage.eml");
        assertEquals("http://example.org/Inbox/sub%20folder/mes%20sage.eml", message.getUrl());
    }

    @Test
    public void setNewHeaders_updates_size() throws MessagingException {
        ParsedMessageEnvelope parsedMessageEnvelope = new ParsedMessageEnvelope();
        parsedMessageEnvelope.addHeader("getcontentlength", "1024");
        message.setNewHeaders(parsedMessageEnvelope);
        assertEquals(1024, message.getSize());
    }
}
