package com.fsck.k9.mail.internet;

import java.util.Locale;

import com.fsck.k9.K9;
import com.fsck.k9.mail.MessagingException;

import junit.framework.TestCase;

public class MimeUtilityTest extends TestCase {
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

        result = MimeUtility.getHeaderParameter("name = \"value\"" , "name");
        assertEquals("value", result);

        result = MimeUtility.getHeaderParameter("name=\"\"", "name");
        assertEquals("", result);

        result = MimeUtility.getHeaderParameter("text/html ; charset=\"windows-1251\"", null);
        assertEquals("text/html", result);

        result = MimeUtility.getHeaderParameter("text/HTML ; charset=\"windows-1251\"", null);
        assertEquals("text/HTML", result);
    }
}
