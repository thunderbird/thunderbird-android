package com.fsck.k9.mail.internet;


import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 21)
public class ListHeadersTest {
    private static final String[] TEST_EMAIL_ADDRESSES = new String[] {
            "prettyandsimple@example.com",
            "very.common@example.com",
            "disposable.style.email.with+symbol@example.com",
            "other.email-with-dash@example.com",
            //TODO: Fix Address.parse()
            /*
            "\"much.more unusual\"@example.com",
            "\"very.unusual.@.unusual.com\"@example.com",
            //"very.(),:;<>[]\".VERY.\"very@\\ \"very\".unusual"@strange.example.com
            "\"very.(),:;<>[]\\\".VERY.\\\"very@\\\\ \\\"very\\\".unusual\"@strange.example.com",
            "admin@mailserver1",
            "#!$%&'*+-/=?^_`{}|~@example.org",
            "\"()<>[]:,;@\\\\\\\"!#$%&'*+-/=?^_`{}| ~.a\"@example.org",
            "\" \"@example.org",
            "example@localhost",
            "example@s.solutions",
            "user@com",
            "user@localserver",
            "user@[IPv6:2001:db8::1]"
             */
    };


    @Test
    public void getListPostAddresses_withMailTo_shouldReturnCorrectAddress() throws Exception {
        for (String emailAddress : TEST_EMAIL_ADDRESSES) {
            String headerValue = "<mailto:" + emailAddress + ">";
            Message message = buildMimeMessageWithListPostValue(headerValue);

            Address[] result = ListHeaders.getListPostAddresses(message);

            assertExtractedAddressMatchesEmail(emailAddress, result);
        }
    }

    @Test
    public void getListPostAddresses_withMailtoWithNote_shouldReturnCorrectAddress() throws Exception {
        for (String emailAddress : TEST_EMAIL_ADDRESSES) {
            String headerValue = "<mailto:" + emailAddress + "> (Postings are Moderated)";
            Message message = buildMimeMessageWithListPostValue(headerValue);

            Address[] result = ListHeaders.getListPostAddresses(message);

            assertExtractedAddressMatchesEmail(emailAddress, result);
        }
    }

    @Test
    public void getListPostAddresses_withMailtoWithSubject_shouldReturnCorrectAddress() throws Exception {
        for (String emailAddress : TEST_EMAIL_ADDRESSES) {
            String headerValue = "<mailto:" + emailAddress + "?subject=list%20posting>";
            Message message = buildMimeMessageWithListPostValue(headerValue);

            Address[] result = ListHeaders.getListPostAddresses(message);

            assertExtractedAddressMatchesEmail(emailAddress, result);
        }
    }

    @Test
    public void getListPostAddresses_withMessageWithNo_shouldReturnEmptyList() throws Exception {
        MimeMessage message = buildMimeMessageWithListPostValue("NO (posting not allowed on this list)");

        Address[] result = ListHeaders.getListPostAddresses(message);

        assertEquals(0, result.length);
    }

    @Test
    public void getListPostAddresses_shouldProvideAllListPostHeaders() throws Exception {
        MimeMessage message = buildMimeMessageWithListPostValue(
                "<mailto:list1@example.org>", "<mailto:list2@example.org>");

        Address[] result = ListHeaders.getListPostAddresses(message);

        assertNotNull(result);
        assertEquals(2, result.length);
        assertNotNull(result[0]);
        assertEquals("list1@example.org", result[0].getAddress());
        assertNotNull(result[1]);
        assertEquals("list2@example.org", result[1].getAddress());
    }

    @Test
    public void getListPostAddresses_withoutMailtoUriInBrackets_shouldReturnEmptyList() throws Exception {
        MimeMessage message = buildMimeMessageWithListPostValue("<x-mailto:something>");

        Address[] result = ListHeaders.getListPostAddresses(message);

        assertEquals(0, result.length);
    }

    private void assertExtractedAddressMatchesEmail(String emailAddress, Address[] result) {
        assertNotNull(result);
        assertEquals(1, result.length);
        assertNotNull(result[0]);
        assertEquals(emailAddress, result[0].getAddress());
    }

    private MimeMessage buildMimeMessageWithListPostValue(String... values) throws MessagingException {
        MimeMessage message = new MimeMessage();
        for (String value : values) {
            message.addHeader("List-Post", value);
        }

        return message;
    }
}
