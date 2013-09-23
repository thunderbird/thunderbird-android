package com.fsck.k9.mail.internet;

import java.util.Locale;

import com.fsck.k9.K9;
import com.fsck.k9.mail.MessagingException;

import junit.framework.TestCase;

public class MimeUtilityTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

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

    public void testFixupCharset() throws MessagingException {
        String charsetOnMail;
        String expect;

        charsetOnMail = "CP932";
        expect = "shift_jis";
        assertEquals(expect, MimeUtility.fixupCharset(charsetOnMail, new MimeMessage()));

//        charsetOnMail = "koi8-u";
//        expect = "koi8-r";
//        assertEquals(expect, MimeUtility.fixupCharset(charsetOnMail, new MimeMessage()));

        MimeMessage message;

        message= new MimeMessage();
        message.setHeader("From", "aaa@docomo.ne.jp");
        charsetOnMail = "shift_jis";
        expect = "x-docomo-shift_jis-2007";
        assertEquals(expect, MimeUtility.fixupCharset(charsetOnMail, message));

        message = new MimeMessage();
        message.setHeader("From", "aaa@dwmail.jp");
        charsetOnMail = "shift_jis";
        expect = "x-docomo-shift_jis-2007";
        assertEquals(expect, MimeUtility.fixupCharset(charsetOnMail, message));

        message = new MimeMessage();
        message.setHeader("From", "aaa@pdx.ne.jp");
        charsetOnMail = "shift_jis";
        expect = "x-docomo-shift_jis-2007";
        assertEquals(expect, MimeUtility.fixupCharset(charsetOnMail, message));

        message = new MimeMessage();
        message.setHeader("From", "aaa@willcom.com");
        charsetOnMail = "shift_jis";
        expect = "x-docomo-shift_jis-2007";
        assertEquals(expect, MimeUtility.fixupCharset(charsetOnMail, message));

        message = new MimeMessage();
        message.setHeader("From", "aaa@emnet.ne.jp");
        charsetOnMail = "shift_jis";
        expect = "x-docomo-shift_jis-2007";
        assertEquals(expect, MimeUtility.fixupCharset(charsetOnMail, message));

        message = new MimeMessage();
        message.setHeader("From", "aaa@emobile.ne.jp");
        charsetOnMail = "shift_jis";
        expect = "x-docomo-shift_jis-2007";
        assertEquals(expect, MimeUtility.fixupCharset(charsetOnMail, message));

        message = new MimeMessage();
        message.setHeader("From", "aaa@softbank.ne.jp");
        charsetOnMail = "shift_jis";
        expect = "x-softbank-shift_jis-2007";
        assertEquals(expect, MimeUtility.fixupCharset(charsetOnMail, message));

        message = new MimeMessage();
        message.setHeader("From", "aaa@vodafone.ne.jp");
        charsetOnMail = "shift_jis";
        expect = "x-softbank-shift_jis-2007";
        assertEquals(expect, MimeUtility.fixupCharset(charsetOnMail, message));

        message = new MimeMessage();
        message.setHeader("From", "aaa@disney.ne.jp");
        charsetOnMail = "shift_jis";
        expect = "x-softbank-shift_jis-2007";
        assertEquals(expect, MimeUtility.fixupCharset(charsetOnMail, message));

        message = new MimeMessage();
        message.setHeader("From", "aaa@vertuclub.ne.jp");
        charsetOnMail = "shift_jis";
        expect = "x-softbank-shift_jis-2007";
        assertEquals(expect, MimeUtility.fixupCharset(charsetOnMail, message));

        message = new MimeMessage();
        message.setHeader("From", "aaa@ezweb.ne.jp");
        charsetOnMail = "shift_jis";
        expect = "x-kddi-shift_jis-2007";
        assertEquals(expect, MimeUtility.fixupCharset(charsetOnMail, message));

        message = new MimeMessage();
        message.setHeader("From", "aaa@ido.ne.jp");
        charsetOnMail = "shift_jis";
        expect = "x-kddi-shift_jis-2007";
        assertEquals(expect, MimeUtility.fixupCharset(charsetOnMail, message));

    }

}
