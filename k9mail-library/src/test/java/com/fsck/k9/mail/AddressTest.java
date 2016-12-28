package com.fsck.k9.mail;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 21)
public class AddressTest {
    /**
     * test the possibility to parse "From:" fields with no email.
     * for example: From: News for Vector Limited - Google Finance
     * http://code.google.com/p/k9mail/issues/detail?id=3814
     */
    @Test
    public void testParseWithMissingEmail() {
        Address[] addresses = Address.parse("NAME ONLY");
        assertEquals(1, addresses.length);
        assertEquals(null, addresses[0].getAddress());
        assertEquals("NAME ONLY", addresses[0].getPersonal());
    }

    /**
     * test name + valid email
     */
    @Test
    public void testParseWithValidEmail() {
        Address[] addresses = Address.parse("Max Mustermann <maxmuster@mann.com>");
        assertEquals(1, addresses.length);
        assertEquals("maxmuster@mann.com", addresses[0].getAddress());
        assertEquals("Max Mustermann", addresses[0].getPersonal());
    }

    @Test
    public void testParseUnusualEmails() {
        String[] testEmails = new String [] {
                "prettyandsimple@example.com",
                "very.common@example.com",
                "disposable.style.email.with+symbol@example.com",
                "other.email-with-dash@example.com",
                //TODO: Handle addresses with quotes
                /*
                "\"much.more unusual\"@example.com",
                "\"very.unusual.@.unusual.com\"@example.com",
                //"very.(),:;<>[]\".VERY.\"very@\\ \"very\".unusual"@strange.example.com
                "\"very.(),:;<>[]\\\".VERY.\\\"very@\\\\ \\\"very\\\".unusual\"@strange.example.com",
                "\"()<>[]:,;@\\\\\\\"!#$%&'*+-/=?^_`{}| ~.a\"@example.org",
                "\" \"@example.org",
                */
                "admin@mailserver1",
                "#!$%&'*+-/=?^_`{}|~@example.org",
                "example@localhost",
                "example@s.solutions",
                "user@com",
                "user@localserver",
                "user@[IPv6:2001:db8::1]"
        };
        for(String testEmail: testEmails) {
            Address[] addresses = Address.parse("Anonymous <"+testEmail+">");
            assertEquals(1, addresses.length);
            assertEquals(testEmail, addresses[0].getAddress());
        }
    }

    /**
     * test with multi email addresses
     */
    @Test
    public void testParseWithValidEmailMulti() {
        Address[] addresses = Address.parse("lorem@ipsum.us,mark@twain.com");
        assertEquals(2, addresses.length);
        assertEquals("lorem@ipsum.us", addresses[0].getAddress());
        assertEquals(null, addresses[0].getPersonal());
        assertEquals("mark@twain.com", addresses[1].getAddress());
        assertEquals(null, addresses[1].getPersonal());
    }

    @Test
    public void stringQuotationShouldCorrectlyQuote() {
        assertEquals("\"sample\"", Address.quoteString("sample"));
        assertEquals("\"\"sample\"\"", Address.quoteString("\"\"sample\"\""));
        assertEquals("\"sample\"", Address.quoteString("\"sample\""));
        assertEquals("\"sa\"mp\"le\"", Address.quoteString("sa\"mp\"le"));
        assertEquals("\"sa\"mp\"le\"", Address.quoteString("\"sa\"mp\"le\""));
        assertEquals("\"\"\"", Address.quoteString("\""));
    }

    @Test
    public void hashCode_withoutAddress() throws Exception {
        Address address = Address.parse("name only")[0];
        assertNull(address.getAddress());
        
        address.hashCode();
    }

    @Test
    public void hashCode_withoutPersonal() throws Exception {
        Address address = Address.parse("alice@example.org")[0];
        assertNull(address.getPersonal());
        
        address.hashCode();
    }
}
