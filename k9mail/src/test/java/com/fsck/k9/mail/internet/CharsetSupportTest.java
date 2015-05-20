package com.fsck.k9.mail.internet;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
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
}
