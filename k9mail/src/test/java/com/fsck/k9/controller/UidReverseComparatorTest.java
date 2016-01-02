package com.fsck.k9.controller;


import java.util.Arrays;
import java.util.Random;

import android.support.annotation.NonNull;

import com.fsck.k9.controller.MessagingController.UidReverseComparator;
import com.fsck.k9.mail.Message;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;

import static org.junit.Assert.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@SuppressWarnings("ConstantConditions")
public class UidReverseComparatorTest {

    private UidReverseComparator comparator;
    private Random random;

    @Before
    public void onBefore() {
        comparator = new UidReverseComparator();
        random = new Random();
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

    @Test
    public void testRandomCompare() throws Exception {

        Message[] msgs = new Message[500];
        for (int i = 1; i < msgs.length; i++) {
            if (random.nextInt(10) < 2) {
                msgs[i] = null;

                continue;
            }
            if (random.nextInt(10) < 2) {
                Message numberFormatExceptionMessage = mock(Message.class);
                when(numberFormatExceptionMessage.getUid()).thenReturn("xyz" + i);

                msgs[i] = numberFormatExceptionMessage;
                continue;
            }
            int uid = random.nextInt(200) -100;
            msgs[i] = mockMessage(uid);
        }

        Arrays.sort(msgs, comparator);

        // all objects which are null or unparsable must appear in a contiguous segment at the end
        boolean isNullRange = false;
        for (int i = 1; i < msgs.length; i++) {
            if (msgs[i] == null) {
                isNullRange = true;
                continue;
            }
            verify(msgs[i], atLeastOnce()).getUid();
            if (!isIntParseable(msgs[i].getUid())) {
                isNullRange = true;
                continue;
            }
            assertFalse(isNullRange);
            int id1 = Integer.parseInt(msgs[i-1].getUid());
            int id2 = Integer.parseInt(msgs[i].getUid());
            assertTrue(id1 >= id2);
        }

    }

    private static boolean isIntParseable(String str) {
        try {
            // noinspection ResultOfMethodCallIgnored
            Integer.parseInt(str);
            return true;
        } catch (NullPointerException | NumberFormatException e) {
            return false;
        }
    }

    @NonNull
    private static Message mockMessage(int uid1) {
        Message msg1 = mock(Message.class);
        when(msg1.getUid()).thenReturn(Integer.toString(uid1));
        return msg1;
    }

}