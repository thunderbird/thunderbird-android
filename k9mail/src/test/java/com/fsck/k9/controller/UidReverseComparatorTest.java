package com.fsck.k9.controller;


import android.support.annotation.NonNull;

import com.fsck.k9.controller.MessagingController.UidReverseComparator;
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
    public void compare_withLeftNullArgument_shouldReturnPositive() throws Exception {
        Message messageLeft = null;
        Message messageRight = createMessageWithUid(1);

        int result = comparator.compare(messageLeft, messageRight);

        assertTrue("result must be > 0 when left argument is null", result > 0);
    }

    @Test
    public void compare_withRightNullArgument_shouldReturnNegative() throws Exception {
        Message messageLeft = createMessageWithUid(1);
        Message messageRight = null;

        int result = comparator.compare(messageLeft, messageRight);

        assertTrue("result must be < 0 when right argument is null", result < 0);
    }

    @Test
    public void compare_twoMessages_shouldReturnOrderByUid() throws Exception {
        Message messageLeft = createMessageWithUid(5);
        Message messageRight = createMessageWithUid(15);

        int result = comparator.compare(messageLeft, messageRight);

        assertTrue("result must be > 0 when right message has larger UID than left message", result > 0);
    }

    @NonNull
    private Message createMessageWithUid(int uid) {
        Message message = mock(Message.class);
        when(message.getUid()).thenReturn(Integer.toString(uid));

        return message;
    }
}
