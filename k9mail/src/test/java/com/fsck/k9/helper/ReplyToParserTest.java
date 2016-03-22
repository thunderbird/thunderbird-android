package com.fsck.k9.helper;

import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.ListHeaders;
import com.fsck.k9.mail.internet.MimeMessage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 21)
public class ReplyToParserTest {
    private Message message = mock(Message.class);
    private Address[] replyToAddresses = new Address[]{new Address("replyTo@example.com")};
    private Address[] listPostAddresses = new Address[]{new Address("listPost@example.com")};
    private Address[] fromAddresses = new Address[]{new Address("from@example.com")};
    private String listPostHeaderValue = "<mailto:listPost@example.com>";

    @Before
    public void setUp() throws Exception {
        when(message.getReplyTo()).thenReturn(new Address[]{});
        when(message.getHeader(ListHeaders.LIST_POST_HEADER)).thenReturn(new String[]{});
        when(message.getFrom()).thenReturn(new Address[]{});
    }

    @Test
    public void getRecipientsToReplyTo_should_prefer_replyTo_over_any_other_field() throws MessagingException {
        when(message.getReplyTo()).thenReturn(replyToAddresses);
        when(message.getHeader(ListHeaders.LIST_POST_HEADER)).thenReturn(new String[]{listPostHeaderValue});
        when(message.getFrom()).thenReturn(fromAddresses);

        assertSame(replyToAddresses, ReplyToParser.getRecipientsToReplyTo(message));
    }

    @Test
    public void getRecipientsToReplyTo_should_prefer_listPost_over_from_field() throws MessagingException {
        when(message.getHeader(ListHeaders.LIST_POST_HEADER)).thenReturn(new String[]{listPostHeaderValue});
        when(message.getFrom()).thenReturn(fromAddresses);
        assertArrayEquals(listPostAddresses, ReplyToParser.getRecipientsToReplyTo(message));

    }

    @Test
    public void getRecipientsToReplyTo_should_return_from_otherwise() {
        when(message.getFrom()).thenReturn(fromAddresses);

        assertSame(fromAddresses, ReplyToParser.getRecipientsToReplyTo(message));

    }
}
