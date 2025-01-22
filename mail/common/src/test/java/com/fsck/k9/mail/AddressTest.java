package com.fsck.k9.mail;


import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class AddressTest {
    /**
     * test the possibility to parse "From:" fields with no email.
     * for example: From: News for Vector Limited - Google Finance
     * http://code.google.com/p/k9mail/issues/detail?id=3814
     */
    @Test
    public void parse_withMissingEmail__shouldSetPersonal() {
        Address[] addresses = Address.parse("NAME ONLY");

        assertEquals(0, addresses.length);
    }

    /**
     * test name + valid email
     */
    @Test
    public void parse_withValidEmailAndPersonal_shouldSetBoth() {
        Address[] addresses = Address.parse("Max Mustermann <maxmuster@mann.com>");

        assertEquals(1, addresses.length);
        assertEquals("maxmuster@mann.com", addresses[0].getAddress());
        assertEquals("Max Mustermann", addresses[0].getPersonal());
    }

    @Test
    public void parse_withUnusualEmails_shouldSetAddress() {
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

            assertEquals(testEmail, 1, addresses.length);
            assertEquals(testEmail, testEmail, addresses[0].getAddress());
        }
    }

    @Test
    public void parse_withEncodedPersonal_shouldDecode() {
        Address[] addresses = Address.parse(
                "=?UTF-8?B?WWFob28h44OA44Kk44Os44Kv44OI44Kq44OV44Kh44O8?= <directoffer-master@mail.yahoo.co.jp>");

        assertEquals("Yahoo!ダイレクトオファー", addresses[0].getPersonal());
        assertEquals("directoffer-master@mail.yahoo.co.jp", addresses[0].getAddress());

    }

    @Test
    public void parse_withQuotedEncodedPersonal_shouldDecode() {
        Address[] addresses = Address.parse(
                "\"=?UTF-8?B?WWFob28h44OA44Kk44Os44Kv44OI44Kq44OV44Kh44O8?= \"<directoffer-master@mail.yahoo.co.jp>");

        assertEquals("Yahoo!ダイレクトオファー ", addresses[0].getPersonal());
        assertEquals("directoffer-master@mail.yahoo.co.jp", addresses[0].getAddress());

    }

    /**
     * test with multi email addresses
     */
    @Test
    public void parse_withMultipleEmails_shouldDecodeBoth() {
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
        Address[] addresses = Address.parse("name only");

        assertEquals(0, addresses.length);
    }

    @Test
    public void hashCode_withoutPersonal() throws Exception {
        Address address = Address.parse("alice@example.org")[0];
        assertNull(address.getPersonal());
        
        address.hashCode();
    }

    @Test
    public void equals_withoutPersonal_matchesSame() throws Exception {
        Address address = Address.parse("alice@example.org")[0];
        Address address2 = Address.parse("alice@example.org")[0];
        assertNull(address.getPersonal());

        boolean result = address.equals(address2);

        assertTrue(result);
    }

    @Test
    public void equals_withoutPersonal_doesNotMatchWithAddress() throws Exception {
        Address address = Address.parse("alice@example.org")[0];
        Address address2 = Address.parse("Alice <alice@example.org>")[0];

        boolean result = address.equals(address2);

        assertFalse(result);
    }

    @Test
    public void handlesInvalidBase64Encoding() throws Exception {
        Address address = Address.parse("=?utf-8?b?invalid#?= <oops@example.com>")[0];
        assertEquals("oops@example.com", address.getAddress());
    }
}
