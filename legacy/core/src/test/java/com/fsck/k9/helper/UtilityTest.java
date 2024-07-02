package com.fsck.k9.helper;


import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UtilityTest {

    @Test
    public void stripNewLines_removesLeadingCarriageReturns() {
        String result = Utility.stripNewLines("\r\rTest");

        assertEquals("Test", result);
    }

    @Test
    public void stripNewLines_removesLeadingLineFeeds() {
        String result = Utility.stripNewLines("\n\nTest\n\n");

        assertEquals("Test", result);
    }

    @Test
    public void stripNewLines_removesTrailingCarriageReturns() {
        String result = Utility.stripNewLines("Test\r\r");

        assertEquals("Test", result);
    }

    @Test
    public void stripNewLines_removesMidCarriageReturns() {
        String result = Utility.stripNewLines("Test\rTest");

        assertEquals("TestTest", result);
    }

    @Test
    public void stripNewLines_removesMidLineFeeds() {
        String result = Utility.stripNewLines("Test\nTest");

        assertEquals("TestTest", result);
    }

    @Test
    public void arrayContains_withObject_returnTrue() {
        Object[] container = { 10, 20, 30, 40, 50, 60, 71, 80, 90, 91 };

        boolean result = Utility.arrayContains(container, 10);

        assertTrue(result);
    }

    @Test
    public void arrayContains_withoutObject_returnFalse() {
        Object[] container = { 10, 20, 30, 40, 50, 60, 71, 80, 90, 91 };

        boolean result = Utility.arrayContains(container, 11);

        assertFalse(result);
    }

    @Test
    public void arrayContainsAny_withObject_returnsTrue() {
        Object[] container = { 10, 20, 30, 40, 50, 60, 71, 80, 90, 91 };

        boolean result = Utility.arrayContainsAny(container, 1, 2, 3, 10);

        assertTrue(result);
    }

    @Test
    public void arrayContainsAny_withoutObject_returnsFalse() {
        Object[] container = { 10, 20, 30, 40, 50, 60, 71, 80, 90, 91 };

        boolean result = Utility.arrayContainsAny(container, 1, 2, 3, 4);

        assertFalse(result);
    }
}
