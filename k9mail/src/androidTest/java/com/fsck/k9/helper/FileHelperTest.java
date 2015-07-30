package com.fsck.k9.helper;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;


@RunWith(AndroidJUnit4.class)
public class FileHelperTest  {

    @Test
    public void testSanitize1() {
        checkSanitization(".._bla_", "../bla_");
    }

    @Test
    public void testSanitize2() {
        checkSanitization("_etc_bla", "/etc/bla");
    }

    @Test
    public void testSanitize3() {
        checkSanitization("_пPп", "+пPп");
    }

    @Test
    public void testSanitize4() {
        checkSanitization(".東京_!", ".東京?!");
    }

    @Test
    public void testSanitize5() {
        checkSanitization("Plan 9", "Plan 9");
    }

    private void checkSanitization(String expected, String actual) {
        assertEquals(expected, FileHelper.sanitizeFilename(actual));
    }
}
