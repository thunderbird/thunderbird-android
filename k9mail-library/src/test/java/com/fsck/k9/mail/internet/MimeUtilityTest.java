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
    public void isFormatFlowed_withTextPlainFormatFlowed__shouldReturnTrue() throws Exception {
        assertTrue(MimeUtility.isFormatFlowed("text/plain; format=flowed"));
    }

    @Test
    public void isFormatFlowed_withTextPlain__shouldReturnFalse() throws Exception {
        assertFalse(MimeUtility.isFormatFlowed("text/plain"));
    }

    @Test
    public void isFormatFlowed_withTextHtmlFormatFlowed__shouldReturnFalse() throws Exception {
        assertFalse(MimeUtility.isFormatFlowed("text/html; format=flowed"));
    }
}
