package com.fsck.k9.mail.internet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 21)
public class ListHeadersTest {
    private String[] testEmails = new String [] {
        "prettyandsimple@example.com",
        "very.common@example.com",
        "disposable.style.email.with+symbol@example.com",
        "other.email-with-dash@example.com",
        //TODO: Fix Address.parse()
        /**
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
        **/
    };

    @Test
    public void parsePostAddress_should_be_able_to_parse_mailto() {
        for (String testEmail : testEmails) {
            assertEquals(1,
                    ListHeaders.parsePostAddress(
                            "<mailto:" + testEmail + ">").length);
            assertEquals(testEmail,
                    ListHeaders.parsePostAddress("<mailto:" + testEmail + ">")[0].getAddress());
        }
    }
    @Test
    public void parsePostAddress_should_be_able_to_parse_mailto_with_note() {
        for (String testEmail : testEmails) {
            assertEquals(1,
                    ListHeaders.parsePostAddress(
                            "<mailto:" + testEmail + "> (Postings are Moderated)").length);
            assertEquals(testEmail,
                    ListHeaders.parsePostAddress("<mailto:" + testEmail + "> (Postings are Moderated)")[0].getAddress());
        }
    }

    @Test
    public void parsePostAddress_should_be_able_to_parse_mailto_with_subject() {
        for (String testEmail : testEmails) {
            assertEquals(1,
                    ListHeaders.parsePostAddress(
                            "<mailto:" + testEmail + "?subject=list%20posting>").length);
            assertEquals(testEmail,
                    ListHeaders.parsePostAddress(
                            "<mailto:" + testEmail + "?subject=list%20posting>")[0].getAddress());
        }
    }

    @Test
    public void parsePostAddress_should_be_able_to_handle_NO() {
        assertEquals(0,
                ListHeaders.parsePostAddress("NO (posting not allowed on this list)").length);
    }
}
