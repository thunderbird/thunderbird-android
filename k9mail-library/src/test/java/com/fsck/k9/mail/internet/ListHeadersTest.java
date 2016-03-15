package com.fsck.k9.mail.internet;

import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 21)
public class ListHeadersTest {
    private String[] testEmailAddresses = new String [] {
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

    private MimeMessage buildMimeMessageWithListPostValue(String value) throws MessagingException {
        MimeMessage message = new MimeMessage();
        message.addHeader("List-Post", value);
        return message;
    }

    @Test
    public void getListPostAddresses_withMailTo_shouldReturnCorrectAddress() throws MessagingException {
        MimeMessage[] messages = new MimeMessage[testEmailAddresses.length];
        for (int i = 0; i < testEmailAddresses.length; i++)
            messages[i] = buildMimeMessageWithListPostValue(
                    "<mailto:" + testEmailAddresses[i] + ">");
        Address[][] results = new Address[testEmailAddresses.length][];

        for (int i = 0; i < messages.length; i++) {
            results[i] =  ListHeaders.getListPostAddresses(messages[i]);
        }

        for (int i = 0; i < results.length; i++) {
            assertEquals(1, results[i].length);
            assertEquals(testEmailAddresses[i], results[i][0].getAddress());
        }
    }
    @Test
    public void parsePostAddress_withMailtoWithNote_shouldReturnCorrectAddress() throws MessagingException {
        MimeMessage[] messages = new MimeMessage[testEmailAddresses.length];
        for (int i = 0; i < testEmailAddresses.length; i++)
            messages[i] = buildMimeMessageWithListPostValue(
                    "<mailto:" + testEmailAddresses[i] + "> (Postings are Moderated)");
        Address[][] results = new Address[testEmailAddresses.length][];

        for (int i = 0; i < messages.length; i++) {
            results[i] =  ListHeaders.getListPostAddresses(messages[i]);
        }

        for (int i = 0; i < results.length; i++) {
            assertEquals(1, results[i].length);
            assertEquals(testEmailAddresses[i], results[i][0].getAddress());
        }
    }

    @Test
    public void getListPostAddresses_withMailtoWithSubject_shouldReturnCorrectAddress() throws MessagingException {
        MimeMessage[] messages = new MimeMessage[testEmailAddresses.length];
        for (int i = 0; i < testEmailAddresses.length; i++)
            messages[i] = buildMimeMessageWithListPostValue(
                    "<mailto:" + testEmailAddresses[i] + "?subject=list%20posting>");
        Address[][] results = new Address[testEmailAddresses.length][];

        for (int i = 0; i < messages.length; i++) {
            results[i] =  ListHeaders.getListPostAddresses(messages[i]);
        }

        for (int i = 0; i < results.length; i++) {
            assertEquals(1, results[i].length);
            assertEquals(testEmailAddresses[i], results[i][0].getAddress());
        }
    }

    @Test
    public void getListPostAddresses_withMessageWithNo_shouldReturnEmptyList() throws MessagingException {
        MimeMessage message = buildMimeMessageWithListPostValue("NO (posting not allowed on this list)");

        Address[] result = ListHeaders.getListPostAddresses(message);

        assertEquals(0, result.length);
    }

    @Test
    public void getListPostAddresses_withExceptionThrownGettingHeader_shouldReturnEmptyList() throws MessagingException {
        MimeMessage message = mock(MimeMessage.class);
        when(message.getHeader(ListHeaders.LIST_POST_HEADER)).thenThrow(new MessagingException("Test"));

        Address[] result = ListHeaders.getListPostAddresses(message);

        assertEquals(0, result.length);
    }

    @Test
    public void getListPostAddresses_shouldProvideAllListPostHeaders() throws MessagingException {
        MimeMessage message = buildMimeMessageWithListPostValue("<mailto:list1@example.org>");
        message.addHeader("List-Post","<mailto:list2@example.org>");

        Address[] result = ListHeaders.getListPostAddresses(message);

        assertEquals(2, result.length);
        assertEquals("list1@example.org", result[0].getAddress());
        assertEquals("list2@example.org", result[1].getAddress());
    }
}
