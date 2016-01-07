package com.fsck.k9.controller;


import android.support.annotation.NonNull;

import com.fsck.k9.mail.Message;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@SuppressWarnings("ConstantConditions")
public class UidReverseComparatorTest {
    private UidReverseComparator comparator;


    @Before
    public void onBefore() {
        comparator = new UidReverseComparator();
    }

    @Test
    public void compare_withTwoNullArguments_shouldReturnZero() throws Exception {
        Message messageLeft = null;
        Message messageRight = null;

        int result = comparator.compare(messageLeft, messageRight);

        assertEquals("result must be 0 when both arguments are null", 0, result);
    }

    @Test
    public void compare_withNullArgumentAndMessageWithNullUid_shouldReturnZero() throws Exception {
        Message messageLeft = null;
        Message messageRight = createMessageWithNullUid();

        int result = comparator.compare(messageLeft, messageRight);

        assertEquals("result must be 0 when both arguments are not a message with valid UID", 0, result);
    }

    @Test
    public void compare_withMessageWithNullUidAndNullArgument_shouldReturnZero() throws Exception {
        Message messageLeft = createMessageWithNullUid();
        Message messageRight = null;

        int result = comparator.compare(messageLeft, messageRight);

        assertEquals("result must be 0 when both arguments are not a message with valid UID", 0, result);
    }

    @Test
    public void compare_withTwoMessagesWithNullUid_shouldReturnZero() throws Exception {
        Message messageLeft = createMessageWithNullUid();
        Message messageRight = createMessageWithNullUid();

        int result = comparator.compare(messageLeft, messageRight);

        assertEquals("result must be 0 when both arguments are a message with a null UID", 0, result);
    }

    @Test
    public void compare_withNullArgumentAndMessageWithInvalidUid_shouldReturnZero() throws Exception {
        Message messageLeft = null;
        Message messageRight = createMessageWithInvalidUid();

        int result = comparator.compare(messageLeft, messageRight);

        assertEquals("result must be 0 when both arguments are not a message with valid UID", 0, result);
    }

    @Test
    public void compare_withMessageWithInvalidUidAndNullArgument_shouldReturnZero() throws Exception {
        Message messageLeft = createMessageWithInvalidUid();
        Message messageRight = null;

        int result = comparator.compare(messageLeft, messageRight);

        assertEquals("result must be 0 when both arguments are not a message with valid UID", 0, result);
    }

    @Test
    public void compare_withTwoMessagesWithInvalidUid_shouldReturnZero() throws Exception {
        Message messageLeft = createMessageWithInvalidUid();
        Message messageRight = createMessageWithInvalidUid();

        int result = comparator.compare(messageLeft, messageRight);

        assertEquals("result must be 0 when both arguments are a message with an invalid UID", 0, result);
    }

    @Test
    public void compare_withMessageWithNullUidAndMessageWithInvalidUid_shouldReturnZero() throws Exception {
        Message messageLeft = createMessageWithNullUid();
        Message messageRight = createMessageWithInvalidUid();

        int result = comparator.compare(messageLeft, messageRight);

        assertEquals("result must be 0 when both arguments are not a message with valid UID", 0, result);
    }

    @Test
    public void compare_withMessageWithInvalidUidAndMessageWithNullUid_shouldReturnZero() throws Exception {
        Message messageLeft = createMessageWithInvalidUid();
        Message messageRight = createMessageWithNullUid();

        int result = comparator.compare(messageLeft, messageRight);

        assertEquals("result must be 0 when both arguments are not a message with valid UID", 0, result);
    }

    @Test
    public void compare_withLeftNullArgument_shouldReturnPositive() throws Exception {
        Message messageLeft = null;
        Message messageRight = createMessageWithUid(1);

        int result = comparator.compare(messageLeft, messageRight);

        assertTrue("result must be > 0 when left argument is null", result > 0);
    }

    @Test
    public void compare_withLeftMessageWithNullUid_shouldReturnPositive() throws Exception {
        Message messageLeft = createMessageWithNullUid();
        Message messageRight = createMessageWithUid(1);

        int result = comparator.compare(messageLeft, messageRight);

        assertTrue("result must be > 0 when left argument is message with null UID", result > 0);
    }

    @Test
    public void compare_withLeftMessageWithInvalidUid_shouldReturnPositive() throws Exception {
        Message messageLeft = createMessageWithInvalidUid();
        Message messageRight = createMessageWithUid(1);

        int result = comparator.compare(messageLeft, messageRight);

        assertTrue("result must be > 0 when left argument is message with invalid UID", result > 0);
    }

    @Test
    public void compare_withRightNullArgument_shouldReturnNegative() throws Exception {
        Message messageLeft = createMessageWithUid(1);
        Message messageRight = null;

        int result = comparator.compare(messageLeft, messageRight);

        assertTrue("result must be < 0 when right argument is null", result < 0);
    }

    @Test
    public void compare_withRightMessageWithNullUid_shouldReturnNegative() throws Exception {
        Message messageLeft = createMessageWithUid(1);
        Message messageRight = createMessageWithNullUid();

        int result = comparator.compare(messageLeft, messageRight);

        assertTrue("result must be < 0 when right argument is message with null UID", result < 0);
    }

    @Test
    public void compare_withRightMessageWithInvalidUid_shouldReturnNegative() throws Exception {
        Message messageLeft = createMessageWithUid(1);
        Message messageRight = createMessageWithInvalidUid();

        int result = comparator.compare(messageLeft, messageRight);

        assertTrue("result must be < 0 when right argument is message with invalid UID", result < 0);
    }

    @Test
    public void compare_twoMessages_shouldReturnOrderByUid() throws Exception {
        Message messageSmall = createMessageWithUid(5);
        Message messageLarge = createMessageWithUid(15);

        int resultOne = comparator.compare(messageSmall, messageLarge);
        int resultTwo = comparator.compare(messageLarge, messageSmall);

        assertTrue("result must be > 0 when right message has larger UID than left message", resultOne > 0);
        assertTrue("result must be < 0 when left message has larger UID than right message", resultTwo < 0);
    }

    @NonNull
    private Message createMessageWithUid(int uid) {
        return createMessageWithUidString(Integer.toString(uid));
    }

    @NonNull
    private Message createMessageWithNullUid() {
        return createMessageWithUidString(null);
    }

    @NonNull
    private Message createMessageWithInvalidUid() {
        return createMessageWithUidString("invalid");
    }

    private Message createMessageWithUidString(String uid) {
        Message message = mock(Message.class);
        when(message.getUid()).thenReturn(uid);

        return message;
    }
}
