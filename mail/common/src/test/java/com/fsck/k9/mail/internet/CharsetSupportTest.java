package com.fsck.k9.mail.internet;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class CharsetSupportTest {

    @Test
    public void testFixupCharset() throws Exception {
        String charsetOnMail;
        String expect;

        charsetOnMail = "CP932";
        expect = "shift_jis";
        assertEquals(expect, CharsetSupport.fixupCharset(charsetOnMail, new MimeMessage()));

//        charsetOnMail = "koi8-u";
//        expect = "koi8-r";
//        assertEquals(expect, CharsetSupport.fixupCharset(charsetOnMail, new MimeMessage()));

        MimeMessage message;

        message = new MimeMessage();
        message.setHeader("From", "aaa@docomo.ne.jp");
        charsetOnMail = "shift_jis";
        expect = "x-docomo-shift_jis-2007";
        assertEquals(expect, CharsetSupport.fixupCharset(charsetOnMail, message));

        message = new MimeMessage();
        message.setHeader("From", "aaa@dwmail.jp");
        charsetOnMail = "shift_jis";
        expect = "x-docomo-shift_jis-2007";
        assertEquals(expect, CharsetSupport.fixupCharset(charsetOnMail, message));

        message = new MimeMessage();
        message.setHeader("From", "aaa@pdx.ne.jp");
        charsetOnMail = "shift_jis";
        expect = "x-docomo-shift_jis-2007";
        assertEquals(expect, CharsetSupport.fixupCharset(charsetOnMail, message));

        message = new MimeMessage();
        message.setHeader("From", "aaa@willcom.com");
        charsetOnMail = "shift_jis";
        expect = "x-docomo-shift_jis-2007";
        assertEquals(expect, CharsetSupport.fixupCharset(charsetOnMail, message));

        message = new MimeMessage();
        message.setHeader("From", "aaa@emnet.ne.jp");
        charsetOnMail = "shift_jis";
        expect = "x-docomo-shift_jis-2007";
        assertEquals(expect, CharsetSupport.fixupCharset(charsetOnMail, message));

        message = new MimeMessage();
        message.setHeader("From", "aaa@emobile.ne.jp");
        charsetOnMail = "shift_jis";
        expect = "x-docomo-shift_jis-2007";
        assertEquals(expect, CharsetSupport.fixupCharset(charsetOnMail, message));

        message = new MimeMessage();
        message.setHeader("From", "aaa@softbank.ne.jp");
        charsetOnMail = "shift_jis";
        expect = "x-softbank-shift_jis-2007";
        assertEquals(expect, CharsetSupport.fixupCharset(charsetOnMail, message));

        message = new MimeMessage();
        message.setHeader("From", "aaa@vodafone.ne.jp");
        charsetOnMail = "shift_jis";
        expect = "x-softbank-shift_jis-2007";
        assertEquals(expect, CharsetSupport.fixupCharset(charsetOnMail, message));

        message = new MimeMessage();
        message.setHeader("From", "aaa@disney.ne.jp");
        charsetOnMail = "shift_jis";
        expect = "x-softbank-shift_jis-2007";
        assertEquals(expect, CharsetSupport.fixupCharset(charsetOnMail, message));

        message = new MimeMessage();
        message.setHeader("From", "aaa@vertuclub.ne.jp");
        charsetOnMail = "shift_jis";
        expect = "x-softbank-shift_jis-2007";
        assertEquals(expect, CharsetSupport.fixupCharset(charsetOnMail, message));

        message = new MimeMessage();
        message.setHeader("From", "aaa@ezweb.ne.jp");
        charsetOnMail = "shift_jis";
        expect = "x-kddi-shift_jis-2007";
        assertEquals(expect, CharsetSupport.fixupCharset(charsetOnMail, message));

        message = new MimeMessage();
        message.setHeader("From", "aaa@ido.ne.jp");
        charsetOnMail = "shift_jis";
        expect = "x-kddi-shift_jis-2007";
        assertEquals(expect, CharsetSupport.fixupCharset(charsetOnMail, message));
    }

    @Test
    public void testFixupCharset_shiftJisAliases() throws Exception {
        MimeMessage message = new MimeMessage();
        assertEquals("shift_jis", CharsetSupport.fixupCharset("shift-jis", message));
        assertEquals("shift_jis", CharsetSupport.fixupCharset("sjis", message));
        assertEquals("shift_jis", CharsetSupport.fixupCharset("ms932", message));
        assertEquals("shift_jis", CharsetSupport.fixupCharset("windows-31j", message));
        assertEquals("shift_jis", CharsetSupport.fixupCharset("x-sjis", message));
        assertEquals("shift_jis", CharsetSupport.fixupCharset("x-ms-cp932", message));
    }

    @Test
    public void readToString_withXEucJpAlias_shouldFallBackToEucJp() throws IOException {
        // "test" in ASCII — just verifies the alias is recognized without throwing
        InputStream inputStream = new ByteArrayInputStream("test".getBytes());
        String result = CharsetSupport.readToString(inputStream, "x-euc-jp");
        assertEquals("test", result);
    }

    @Test
    public void readToString_withEucJpUnderscoreAlias_shouldFallBackToEucJp() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("test".getBytes());
        String result = CharsetSupport.readToString(inputStream, "euc_jp");
        assertEquals("test", result);
    }

    @Test
    public void readToString_withUnsupportedCharset_shouldFallBackToAscii() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("input".getBytes());
        String charset = "unsupported";

        String result = CharsetSupport.readToString(inputStream, charset);

        assertEquals("input", result);
    }

    @Test
    public void readToString_withInvalidCharset_shouldFallBackToAscii() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("input".getBytes());
        String charset = "invalid\n";

        String result = CharsetSupport.readToString(inputStream, charset);

        assertEquals("input", result);
    }

    // hasIso2022JpEscapeSequence

    @Test
    public void hasIso2022JpEscapeSequence_withEscDollarB_returnsTrue() {
        byte[] data = {0x1B, '$', 'B', 0x25, 0x46};
        assertTrue(CharsetSupport.hasIso2022JpEscapeSequence(data));
    }

    @Test
    public void hasIso2022JpEscapeSequence_withEscDollarAt_returnsTrue() {
        byte[] data = {0x1B, '$', '@', 0x25, 0x46};
        assertTrue(CharsetSupport.hasIso2022JpEscapeSequence(data));
    }

    @Test
    public void hasIso2022JpEscapeSequence_withNoEscSequence_returnsFalse() {
        byte[] data = "Hello, world!".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        assertFalse(CharsetSupport.hasIso2022JpEscapeSequence(data));
    }

    @Test
    public void hasIso2022JpEscapeSequence_withEscOpenParenB_returnsFalse() {
        // ESC ( B is the return-to-ASCII sequence; alone it should not trigger detection
        byte[] data = {0x1B, '(', 'B'};
        assertFalse(CharsetSupport.hasIso2022JpEscapeSequence(data));
    }

    @Test
    public void hasIso2022JpEscapeSequence_withEmptyArray_returnsFalse() {
        assertFalse(CharsetSupport.hasIso2022JpEscapeSequence(new byte[0]));
    }

    @Test
    public void hasIso2022JpEscapeSequence_withTooShortForSequence_returnsFalse() {
        byte[] data = {0x1B, '$'};   // only 2 bytes, need at least 3
        assertFalse(CharsetSupport.hasIso2022JpEscapeSequence(data));
    }
}
