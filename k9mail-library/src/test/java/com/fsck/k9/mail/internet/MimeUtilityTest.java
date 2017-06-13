package com.fsck.k9.mail.internet;


import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class MimeUtilityTest {
    @Test
    public void testGetHeaderParameter() {
        String result;

        /* Test edge cases */
        result = MimeUtility.getHeaderParameter(";", null);
        assertEquals(null, result);

        result = MimeUtility.getHeaderParameter("name", "name");
        assertEquals(null, result);

        result = MimeUtility.getHeaderParameter("name=", "name");
        assertEquals("", result);

        result = MimeUtility.getHeaderParameter("name=\"", "name");
        assertEquals("\"", result);

        /* Test expected cases */
        result = MimeUtility.getHeaderParameter("name=value", "name");
        assertEquals("value", result);

        result = MimeUtility.getHeaderParameter("name = value", "name");
        assertEquals("value", result);

        result = MimeUtility.getHeaderParameter("name=\"value\"", "name");
        assertEquals("value", result);

        result = MimeUtility.getHeaderParameter("name = \"value\"", "name");
        assertEquals("value", result);

        result = MimeUtility.getHeaderParameter("name=\"\"", "name");
        assertEquals("", result);

        result = MimeUtility.getHeaderParameter("text/html ; charset=\"windows-1251\"", null);
        assertEquals("text/html", result);

        result = MimeUtility.getHeaderParameter("text/HTML ; charset=\"windows-1251\"", null);
        assertEquals("text/HTML", result);
    }

    @Test
    public void isMultipart_withLowerCaseMultipart_shouldReturnTrue() throws Exception {
        assertTrue(MimeUtility.isMultipart("multipart/mixed"));
    }

    @Test
    public void isMultipart_withUpperCaseMultipart_shouldReturnTrue() throws Exception {
        assertTrue(MimeUtility.isMultipart("MULTIPART/ALTERNATIVE"));
    }

    @Test
    public void isMultipart_withMixedCaseMultipart_shouldReturnTrue() throws Exception {
        assertTrue(MimeUtility.isMultipart("Multipart/Alternative"));
    }

    @Test
    public void isMultipart_withoutMultipart_shouldReturnFalse() throws Exception {
        assertFalse(MimeUtility.isMultipart("message/rfc822"));
    }

    @Test
    public void isMultipart_withNullArgument_shouldReturnFalse() throws Exception {
        assertFalse(MimeUtility.isMultipart(null));
    }

    @Test
    public void isMessage_withLowerCaseMessage_shouldReturnTrue() throws Exception {
        assertTrue(MimeUtility.isMessage("message/rfc822"));
    }

    @Test
    public void isMessage_withUpperCaseMessage_shouldReturnTrue() throws Exception {
        assertTrue(MimeUtility.isMessage("MESSAGE/RFC822"));
    }

    @Test
    public void isMessage_withMixedCaseMessage_shouldReturnTrue() throws Exception {
        assertTrue(MimeUtility.isMessage("Message/Rfc822"));
    }

    @Test
    public void isMessage_withoutMessageRfc822_shouldReturnFalse() throws Exception {
        assertFalse(MimeUtility.isMessage("Message/Partial"));
    }

    @Test
    public void isMessage_withoutMessage_shouldReturnFalse() throws Exception {
        assertFalse(MimeUtility.isMessage("multipart/mixed"));
    }

    @Test
    public void isMessage_withNullArgument_shouldReturnFalse() throws Exception {
        assertFalse(MimeUtility.isMessage(null));
    }

    @Test
    public void isSameMimeType_withSameTypeAndCase_shouldReturnTrue() throws Exception {
        assertTrue(MimeUtility.isSameMimeType("text/plain", "text/plain"));
    }

    @Test
    public void isSameMimeType_withSameTypeButMixedCase_shouldReturnTrue() throws Exception {
        assertTrue(MimeUtility.isSameMimeType("text/plain", "Text/Plain"));
    }

    @Test
    public void isSameMimeType_withSameTypeAndLowerAndUpperCase_shouldReturnTrue() throws Exception {
        assertTrue(MimeUtility.isSameMimeType("TEXT/PLAIN", "text/plain"));
    }

    @Test
    public void isSameMimeType_withDifferentType_shouldReturnFalse() throws Exception {
        assertFalse(MimeUtility.isSameMimeType("text/plain", "text/html"));
    }

    @Test
    public void isSameMimeType_withFirstArgumentBeingNull_shouldReturnFalse() throws Exception {
        assertFalse(MimeUtility.isSameMimeType(null, "text/html"));
    }

    @Test
    public void isSameMimeType_withSecondArgumentBeingNull_shouldReturnFalse() throws Exception {
        assertFalse(MimeUtility.isSameMimeType("text/html", null));
    }

    @Test
    public void foldAndEncode_shortHeader_shouldBeEqual() throws Exception {
        assertEquals(MimeUtility.foldAndEncode("short header"), "short header");
    }

    @Test
    public void foldAndEncode_headerLongerThan78CharactersWithSpaces_shouldBeFolded() throws Exception {
        // build test data
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < 2000; i++) {
            if (i%10 == 0)
                sb.append(" ");
            else
                sb.append("a");
        }
        String[] lines = MimeUtility.foldAndEncode(sb.toString()).split("\n");
        sb.setLength(0);
        for (int i = 0; i < 80; i++) {
            if (i%10 == 0)
                sb.append(" ");
            else
                sb.append("a");
        }
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (i == 0) {
                // first line is folded without prepending a space
                assertEquals(line, sb.toString().trim() + "\r");
            } else if (i == lines.length - 1) {
                // last line is folded without terminating linefeed
                assertEquals(line, " " + sb.toString().trim());
            } else {
                assertEquals(line, " " + sb.toString().trim() + "\r");
            }
        }
    }

    @Test
    public void foldAndEncode_headerLongerThan78CharactersButShorterThan998WithoutSpaces_shouldBeKept() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            sb.append("a");
        }
        assertEquals(MimeUtility.foldAndEncode(sb.toString()), sb.toString());
    }

    @Test
    public void foldAndEncode_headerLongerThan998CharactersWithoutSpaces_shouldBeFoldedOnce() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 2000; i++) {
             sb.append("a");
        }
        assertEquals(MimeUtility.foldAndEncode(sb.toString()), "\r\n " + sb.toString());
    }
}
