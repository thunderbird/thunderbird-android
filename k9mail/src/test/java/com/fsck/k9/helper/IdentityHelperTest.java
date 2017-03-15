package com.fsck.k9.helper;


import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.Identity;
import com.fsck.k9.K9RobolectricTestRunner;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.internet.MimeMessage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(K9RobolectricTestRunner.class)
public class IdentityHelperTest {

    private Account account;
    private MimeMessage msg;


    @Before
    public void setUp() throws Exception {
        Context context = RuntimeEnvironment.application;
        createDummyAccount(context);
        msg = parseWithoutRecurse(toStream(
                "From: <adam@example.org>\r\n" +
                        "To: <eva@example.org>\r\n" +
                        "Subject: Testmail\r\n" +
                        "MIME-Version: 1.0\r\n" +
                        "Content-type: text/plain\r\n" +
                        "Content-Transfer-Encoding: 7bit\r\n" +
                        "\r\n" +
                        "this is some test text."));
    }


    private static MimeMessage parseWithoutRecurse(InputStream data) throws Exception {
        return MimeMessage.parseMimeMessage(data, false);
    }

    private static ByteArrayInputStream toStream(String rawMailData) throws Exception {
        return new ByteArrayInputStream(rawMailData.getBytes("ISO-8859-1"));
    }

    private void createDummyAccount(Context context) {
        account = new DummyAccount(context);
        setIdentity();
    }

    private void setIdentity() {
        Identity identity = new Identity();
        identity.setEmail("test@mail.com");
        identity.setName("test");
        Identity identity2 = new Identity();
        identity2.setEmail("test2@mail.com");
        identity2.setName("test2");
        Identity eva = new Identity();
        eva.setEmail("eva@example.org");
        eva.setName("Eva");

        List<Identity> identityList = new ArrayList<>();
        identityList.add(identity);
        identityList.add(identity2);
        identityList.add(eva);
        account.setIdentities(identityList);
    }

    @Test
    public void testXOriginalTo() throws Exception {
        Address[] addresses = {new Address("test2@mail.com")};
        msg.setRecipients(Message.RecipientType.X_ORIGINAL_TO, addresses);

        Identity identity = IdentityHelper.getRecipientIdentityFromMessage(account, msg);
        assertTrue(identity.getEmail().equalsIgnoreCase("test2@mail.com"));
    }

    @Test
    public void testTo_withoutXOriginalTo() throws Exception {
        Identity eva = IdentityHelper.getRecipientIdentityFromMessage(account, msg);
        assertTrue(eva.getEmail().equalsIgnoreCase("eva@example.org"));
    }

    @Test
    public void testDeliveredTo() throws Exception {
        Address[] addresses = {new Address("test2@mail.com")};
        msg.setRecipients(Message.RecipientType.DELIVERED_TO, addresses);
        msg.removeHeader("X-Original-To");

        Identity identity = IdentityHelper.getRecipientIdentityFromMessage(account, msg);
        assertTrue(identity.getEmail().equalsIgnoreCase("test2@mail.com"));

    }

    @Test
    public void testXEnvelopeTo() throws Exception {
        Address[] addresses = {new Address("test@mail.com")};
        msg.setRecipients(Message.RecipientType.X_ENVELOPE_TO, addresses);
        msg.removeHeader("X-Original-To");
        msg.removeHeader("Delivered-To");

        Identity identity = IdentityHelper.getRecipientIdentityFromMessage(account, msg);
        assertTrue(identity.getEmail().equalsIgnoreCase("test@mail.com"));
    }

    @Test
    public void testXEnvelopeTo_withXOriginalTo() throws Exception {
        Address[] addresses = {new Address("test@mail.com")};
        Address[] xoriginaltoaddresses = {new Address("test2@mail.com")};
        msg.setRecipients(Message.RecipientType.X_ENVELOPE_TO, addresses);
        msg.setRecipients(Message.RecipientType.X_ORIGINAL_TO, xoriginaltoaddresses);

        Identity identity = IdentityHelper.getRecipientIdentityFromMessage(account, msg);
        assertTrue(identity.getEmail().equalsIgnoreCase("test2@mail.com"));
    }


    static class DummyAccount extends Account {

        protected DummyAccount(Context context) {
            super(context);
        }
    }
}
