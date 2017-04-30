package com.fsck.k9.helper;


import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
}
