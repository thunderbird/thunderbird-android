package com.fsck.k9.helper;


import java.lang.reflect.Array;
import java.util.ArrayList;

import com.fsck.k9.Account;
import com.fsck.k9.helper.ReplyToParser.ReplyToAddresses;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.internet.ListHeaders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(RobolectricTestRunner.class)
public class ReplyToParserTest {
    private static final Address[] REPLY_TO_ADDRESSES = Address.parse("replyTo1@example.com, replyTo2@example.com");
    private static final Address[] LIST_POST_ADDRESSES = Address.parse("listPost@example.com");
    private static final Address[] FROM_ADDRESSES = Address.parse("from@example.com");
    private static final Address[] TO_ADDRESSES = Address.parse("to1@example.com, to2@example.com");
    private static final Address[] CC_ADDRESSES = Address.parse("cc1@example.com, cc2@example.com");
    private static final String[] LIST_POST_HEADER_VALUES = new String[] { "<mailto:listPost@example.com>" };
    public static final Address[] EMPTY_ADDRESSES = new Address[0];


    private ReplyToParser replyToParser;
    private Message message;
    private Account account;


    @Before
    public void setUp() throws Exception {
        message = mock(Message.class);
        account = mock(Account.class);

        replyToParser = new ReplyToParser();
    }

    @Test
    public void getRecipientsToReplyTo_should_prefer_replyTo_over_any_other_field() throws Exception {
        when(message.getReplyTo()).thenReturn(REPLY_TO_ADDRESSES);
        when(message.getHeader(ListHeaders.LIST_POST_HEADER)).thenReturn(LIST_POST_HEADER_VALUES);
        when(message.getFrom()).thenReturn(FROM_ADDRESSES);

        ReplyToAddresses result = replyToParser.getRecipientsToReplyTo(message, account);

        assertArrayEquals(REPLY_TO_ADDRESSES, result.to);
        assertArrayEquals(EMPTY_ADDRESSES, result.cc);
        verify(account).isAnIdentity(result.to);
    }

    @Test
    public void getRecipientsToReplyTo_should_prefer_from_ifOtherIsIdentity() throws Exception {
        when(message.getReplyTo()).thenReturn(REPLY_TO_ADDRESSES);
        when(message.getHeader(ListHeaders.LIST_POST_HEADER)).thenReturn(LIST_POST_HEADER_VALUES);
        when(message.getFrom()).thenReturn(FROM_ADDRESSES);
        when(message.getRecipients(RecipientType.TO)).thenReturn(TO_ADDRESSES);
        when(account.isAnIdentity(any(Address[].class))).thenReturn(true);

        ReplyToAddresses result = replyToParser.getRecipientsToReplyTo(message, account);

        assertArrayEquals(TO_ADDRESSES, result.to);
        assertArrayEquals(EMPTY_ADDRESSES, result.cc);
    }

    @Test
    public void getRecipientsToReplyTo_should_prefer_listPost_over_from_field() throws Exception {
        when(message.getReplyTo()).thenReturn(EMPTY_ADDRESSES);
        when(message.getHeader(ListHeaders.LIST_POST_HEADER)).thenReturn(LIST_POST_HEADER_VALUES);
        when(message.getFrom()).thenReturn(FROM_ADDRESSES);

        ReplyToAddresses result = replyToParser.getRecipientsToReplyTo(message, account);

        assertArrayEquals(LIST_POST_ADDRESSES, result.to);
        assertArrayEquals(EMPTY_ADDRESSES, result.cc);
        verify(account).isAnIdentity(result.to);
    }

    @Test
    public void getRecipientsToReplyTo_should_return_from_otherwise() throws Exception {
        when(message.getReplyTo()).thenReturn(EMPTY_ADDRESSES);
        when(message.getHeader(ListHeaders.LIST_POST_HEADER)).thenReturn(new String[0]);
        when(message.getFrom()).thenReturn(FROM_ADDRESSES);

        ReplyToAddresses result = replyToParser.getRecipientsToReplyTo(message, account);

        assertArrayEquals(FROM_ADDRESSES, result.to);
        assertArrayEquals(EMPTY_ADDRESSES, result.cc);
        verify(account).isAnIdentity(result.to);
    }

    @Test
    public void getRecipientsToReplyAllTo_should_returnFromAndToAndCcRecipients() throws Exception {
        when(message.getReplyTo()).thenReturn(EMPTY_ADDRESSES);
        when(message.getHeader(ListHeaders.LIST_POST_HEADER)).thenReturn(new String[0]);
        when(message.getFrom()).thenReturn(FROM_ADDRESSES);
        when(message.getRecipients(RecipientType.TO)).thenReturn(TO_ADDRESSES);
        when(message.getRecipients(RecipientType.CC)).thenReturn(CC_ADDRESSES);

        ReplyToAddresses recipientsToReplyAllTo = replyToParser.getRecipientsToReplyAllTo(message, account);

        assertArrayEquals(arrayConcatenate(FROM_ADDRESSES, TO_ADDRESSES, Address.class), recipientsToReplyAllTo.to);
        assertArrayEquals(CC_ADDRESSES, recipientsToReplyAllTo.cc);
    }

    @Test
    public void getRecipientsToReplyAllTo_should_excludeIdentityAddresses() throws Exception {
        when(message.getReplyTo()).thenReturn(EMPTY_ADDRESSES);
        when(message.getHeader(ListHeaders.LIST_POST_HEADER)).thenReturn(new String[0]);
        when(message.getFrom()).thenReturn(EMPTY_ADDRESSES);

        when(message.getRecipients(RecipientType.TO)).thenReturn(TO_ADDRESSES);
        when(message.getRecipients(RecipientType.CC)).thenReturn(CC_ADDRESSES);
        Address excludedCcAddress = CC_ADDRESSES[1];
        Address excludedToAddress = TO_ADDRESSES[0];
        when(account.isAnIdentity(eq(excludedToAddress))).thenReturn(true);
        when(account.isAnIdentity(eq(excludedCcAddress))).thenReturn(true);


        ReplyToAddresses recipientsToReplyAllTo = replyToParser.getRecipientsToReplyAllTo(message, account);


        assertArrayEquals(arrayExcept(TO_ADDRESSES, excludedToAddress), recipientsToReplyAllTo.to);
        assertArrayEquals(arrayExcept(CC_ADDRESSES, excludedCcAddress), recipientsToReplyAllTo.cc);
    }

    @Test
    public void getRecipientsToReplyAllTo_should_excludeDuplicates() throws Exception {
        when(message.getReplyTo()).thenReturn(REPLY_TO_ADDRESSES);
        when(message.getFrom()).thenReturn(arrayConcatenate(FROM_ADDRESSES, REPLY_TO_ADDRESSES, Address.class));
        when(message.getRecipients(RecipientType.TO)).thenReturn(arrayConcatenate(FROM_ADDRESSES, TO_ADDRESSES, Address.class));
        when(message.getRecipients(RecipientType.CC)).thenReturn(arrayConcatenate(CC_ADDRESSES, TO_ADDRESSES, Address.class));
        when(message.getHeader(ListHeaders.LIST_POST_HEADER)).thenReturn(new String[0]);

        ReplyToAddresses recipientsToReplyAllTo = replyToParser.getRecipientsToReplyAllTo(message, account);

        assertArrayContainsAll(REPLY_TO_ADDRESSES, recipientsToReplyAllTo.to);
        assertArrayContainsAll(FROM_ADDRESSES, recipientsToReplyAllTo.to);
        assertArrayContainsAll(TO_ADDRESSES, recipientsToReplyAllTo.to);
        int totalExpectedAddresses = REPLY_TO_ADDRESSES.length + FROM_ADDRESSES.length + TO_ADDRESSES.length;
        assertEquals(totalExpectedAddresses, recipientsToReplyAllTo.to.length);
        assertArrayEquals(CC_ADDRESSES, recipientsToReplyAllTo.cc);
    }

    public <T> void assertArrayContainsAll(T[] expecteds, T[] actual) {
        for (T expected : expecteds) {
            assertTrue("Element must be in array (" + expected + ")", Utility.arrayContains(actual, expected));
        }
    }

    public <T> T[] arrayConcatenate(T[] first, T[] second, Class<T> cls) {
        // noinspection unchecked
        T[] result = (T[]) Array.newInstance(cls, first.length + second.length);

        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);

        return result;
    }

    public <T> T[] arrayExcept(T[] in, T except) {
        ArrayList<T> result = new ArrayList<>();
        for (T element : in) {
            if (!element.equals(except)) {
                result.add(element);
            }
        }

        // noinspection unchecked, it's a hack but it works ♪
        return result.toArray((T[]) new Object[result.size()]);
    }
}
