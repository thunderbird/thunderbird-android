package com.fsck.k9.helper;


import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.internet.ListHeaders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 21)
public class ReplyToParserTest {
    private static final Address[] REPLY_TO_ADDRESSES = createAddressArray("replyTo@example.com");
    private static final Address[] LIST_POST_ADDRESSES = createAddressArray("listPost@example.com");
    private static final Address[] FROM_ADDRESSES = createAddressArray("from@example.com");
    private static final String[] LIST_POST_HEADER_VALUES = new String[] { "<mailto:listPost@example.com>" };


    private Message message;


    @Before
    public void setUp() throws Exception {
        message = mock(Message.class);
    }

    @Test
    public void getRecipientsToReplyTo_should_prefer_replyTo_over_any_other_field() throws Exception {
        when(message.getReplyTo()).thenReturn(REPLY_TO_ADDRESSES);
        when(message.getHeader(ListHeaders.LIST_POST_HEADER)).thenReturn(LIST_POST_HEADER_VALUES);
        when(message.getFrom()).thenReturn(FROM_ADDRESSES);

        Address[] result = ReplyToParser.getRecipientsToReplyTo(message);

        assertArrayEquals(REPLY_TO_ADDRESSES, result);
    }

    @Test
    public void getRecipientsToReplyTo_should_prefer_listPost_over_from_field() throws Exception {
        when(message.getReplyTo()).thenReturn(new Address[0]);
        when(message.getHeader(ListHeaders.LIST_POST_HEADER)).thenReturn(LIST_POST_HEADER_VALUES);
        when(message.getFrom()).thenReturn(FROM_ADDRESSES);

        Address[] result = ReplyToParser.getRecipientsToReplyTo(message);

        assertArrayEquals(LIST_POST_ADDRESSES, result);
    }

    @Test
    public void getRecipientsToReplyTo_should_return_from_otherwise() throws Exception {
        when(message.getReplyTo()).thenReturn(new Address[0]);
        when(message.getHeader(ListHeaders.LIST_POST_HEADER)).thenReturn(new String[0]);
        when(message.getFrom()).thenReturn(FROM_ADDRESSES);

        Address[] result = ReplyToParser.getRecipientsToReplyTo(message);

        assertArrayEquals(FROM_ADDRESSES, result);
    }

    private static Address[] createAddressArray(String emailAddress) {
        return new Address[] { new Address(emailAddress) };
    }
}
