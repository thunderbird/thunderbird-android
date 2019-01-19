package com.fsck.k9.mail.internet;


import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class MimeUtilityTest {
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
}
