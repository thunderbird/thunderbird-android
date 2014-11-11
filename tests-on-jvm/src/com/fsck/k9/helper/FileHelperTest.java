package com.fsck.k9.helper;

import junit.framework.TestCase;

import java.lang.String;

public class FileHelperTest extends TestCase {

    public void testSanitize1() {
        checkSanitization(".._bla_", "../bla_");
    }

    public void testSanitize2() {
        checkSanitization("_etc_bla", "/etc/bla");
    }

    public void testSanitize3() {
        checkSanitization("_пPп", "+пPп");
    }

    public void testSanitize4() {
        checkSanitization(".東京_!", ".東京?!");
    }

    public void testSanitize5() {
        checkSanitization("Plan 9", "Plan 9");
    }

    private void checkSanitization(String expected, String actual) {
        assertEquals(expected, FileHelper.sanitizeFilename(actual));
    }
}
