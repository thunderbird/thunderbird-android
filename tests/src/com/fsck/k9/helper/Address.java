package com.fsck.k9.helper;
import junit.framework.TestCase;

public class Address extends TestCase {
    /**
     * test the possibility to parse "From:" fields with no email.
     * for example: From: News for Vector Limited - Google Finance
     * http://code.google.com/p/k9mail/issues/detail?id=3814
     */
    public void testParseWithMissingEmail() {
        com.fsck.k9.mail.Address[] addresses =  com.fsck.k9.mail.Address.parse("NAME ONLY");
        assertEquals(1, addresses.length);
        assertEquals(null, addresses[0].getAddress());
        assertEquals("NAME ONLY", addresses[0].getPersonal());
    }

    /**
     * test name + valid email
     */
    public void testPraseWithValidEmail() {
        com.fsck.k9.mail.Address[] addresses =  com.fsck.k9.mail.Address.parse("Max Mustermann <maxmuster@mann.com>");
        assertEquals(1, addresses.length);
        assertEquals("maxmuster@mann.com", addresses[0].getAddress());
        assertEquals("Max Mustermann", addresses[0].getPersonal());
    }
    /**
     * test with multi email addresses
     */
    public void testPraseWithValidEmailMulti() {
        com.fsck.k9.mail.Address[] addresses =  com.fsck.k9.mail.Address.parse("lorem@ipsum.us,mark@twain.com");
        assertEquals(2, addresses.length);
        assertEquals("lorem@ipsum.us", addresses[0].getAddress());
        assertEquals(null, addresses[0].getPersonal());
        assertEquals("mark@twain.com", addresses[1].getAddress());
        assertEquals(null, addresses[1].getPersonal());
    }
}
