package com.fsck.k9.controller;


import android.support.annotation.NonNull;

import com.fsck.k9.controller.MessagingController.UidReverseComparator;
import com.fsck.k9.mail.Message;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
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
        Message messageRight = mockMessage(1);

        int result = comparator.compare(messageLeft, messageRight);

        assertEquals("result must be 1 when both arguments are null", 1, result);
    }

    @Test
    public void compare_withRightNullArgument_shouldReturnNegative() throws Exception {
        Message messageLeft = mockMessage(1);
        Message messageRight = null;

        int result = comparator.compare(messageLeft, messageRight);

        assertEquals("result must be -1 when both arguments are null", -1, result);
    }

    @Test
    public void compare_twoMessages_shouldReturnOrderByUid() throws Exception {
        Message messageLeft = mockMessage(5);
        Message messageRight = mockMessage(15);

        int result = comparator.compare(messageLeft, messageRight);

        assertEquals("result must be 1 when right message has larger uid than left message", 1, result);
    }

    @NonNull
    private static Message mockMessage(int uid1) {
        Message msg1 = mock(Message.class);
        when(msg1.getUid()).thenReturn(Integer.toString(uid1));
        return msg1;
    }
}
