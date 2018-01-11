package com.fsck.k9.helper;


import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.Identity;
import com.fsck.k9.K9RobolectricTestRunner;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.internet.MimeMessage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
        identity.setEmail("test@example.org");
        identity.setName("test");
        Identity identity2 = new Identity();
        identity2.setEmail("test2@example.org");
        identity2.setName("test2");
        Identity identity3 = new Identity();
        identity3.setEmail("test3@example.org");
        identity3.setName("test3");
        Identity defaultIdentity = new Identity();
        defaultIdentity.setEmail("default@example.org");
        defaultIdentity.setName("Default");

        List<Identity> identityList = new ArrayList<>();
        identityList.add(defaultIdentity);
        identityList.add(identity);
        identityList.add(identity2);
        identityList.add(identity3);
        account.setIdentities(identityList);
    }

    @Test
    public void getRecipientIdentityFromMessage_usesXOriginalTo_whenPresent() throws Exception {
        Address[] addresses = {new Address("test2@example.org")};
        msg.removeHeader("To");
        msg.setRecipients(Message.RecipientType.X_ORIGINAL_TO, addresses);

        Identity identity = IdentityHelper.getRecipientIdentityFromMessage(account, msg);
        assertTrue(identity.getEmail().equalsIgnoreCase("test2@example.org"));
    }

    @Test
    public void getRecipientIdentityFromMessage_usesTo_whenPresent() throws Exception {
        Address[] addresses = {new Address("test@example.org")};
        msg.setRecipients(RecipientType.TO, addresses);
        Identity eva = IdentityHelper.getRecipientIdentityFromMessage(account, msg);
        assertTrue(eva.getEmail().equalsIgnoreCase("test@example.org"));
    }

    @Test
    public void getRecipientIdentityFromMessage_usesCC_whenPresent() throws Exception {
        Address[] addresses = {new Address("test@example.org")};
        msg.setRecipients(RecipientType.CC, addresses);
        Identity eva = IdentityHelper.getRecipientIdentityFromMessage(account, msg);
        assertTrue(eva.getEmail().equalsIgnoreCase("test@example.org"));
    }

    @Test
    public void getRecipientIdentityFromMessage_usesDeliveredTo_whenPresent() throws Exception {
        Address[] addresses = {new Address("test2@example.org")};
        msg.setRecipients(Message.RecipientType.DELIVERED_TO, addresses);
        msg.removeHeader("To");

        Identity identity = IdentityHelper.getRecipientIdentityFromMessage(account, msg);
        assertTrue(identity.getEmail().equalsIgnoreCase("test2@example.org"));

    }

    @Test
    public void getRecipientIdentityFromMessage_usesXEnvelopeTo_whenPresent() throws Exception {
        Address[] addresses = {new Address("test2@example.org")};
        msg.setRecipients(Message.RecipientType.X_ENVELOPE_TO, addresses);
        msg.removeHeader("To");

        Identity identity = IdentityHelper.getRecipientIdentityFromMessage(account, msg);
        assertTrue(identity.getEmail().equalsIgnoreCase("test2@example.org"));
    }

    @Test
    public void getRecipientIdentityFromMessage_prefers1To_overCC() throws Exception {
        Address[] toAddresses = {new Address("test@example.org")};
        Address[] ccAddresses = {new Address("test2@example.org")};
        msg.setRecipients(RecipientType.TO, toAddresses);
        msg.setRecipients(RecipientType.CC, ccAddresses);

        Identity identity = IdentityHelper.getRecipientIdentityFromMessage(account, msg);
        assertTrue(identity.getEmail().equalsIgnoreCase("test@example.org"));
    }

    @Test
    public void getRecipientIdentityFromMessage_prefers2CC_overXOriginalTo() throws Exception {
        Address[] ccAddresses = {new Address("test@example.org")};
        Address[] xOriginalToAddresses = {new Address("test2@example.org")};
        msg.setRecipients(RecipientType.CC, ccAddresses);
        msg.setRecipients(RecipientType.X_ORIGINAL_TO, xOriginalToAddresses);

        Identity identity = IdentityHelper.getRecipientIdentityFromMessage(account, msg);
        assertTrue(identity.getEmail().equalsIgnoreCase("test@example.org"));
    }

    @Test
    public void getRecipientIdentityFromMessage_prefers3XOriginalTo_overDeliveredTo() throws Exception {
        Address[] xOriginalToAddresses = {new Address("test@example.org")};
        Address[] deliveredToAddresses = {new Address("test2@example.org")};
        msg.setRecipients(RecipientType.X_ORIGINAL_TO, xOriginalToAddresses);
        msg.setRecipients(RecipientType.DELIVERED_TO, deliveredToAddresses);

        Identity identity = IdentityHelper.getRecipientIdentityFromMessage(account, msg);
        assertTrue(identity.getEmail().equalsIgnoreCase("test@example.org"));
    }

    @Test
    public void getRecipientIdentityFromMessage_prefers4DeliveredTo_overXEnvelopeTo() throws Exception {
        Address[] deliveredToAddresses = {new Address("test@example.org")};
        Address[] xEnvelopeTo = {new Address("test2@example.org")};
        msg.setRecipients(RecipientType.DELIVERED_TO, deliveredToAddresses);
        msg.setRecipients(RecipientType.X_ENVELOPE_TO, xEnvelopeTo);

        Identity identity = IdentityHelper.getRecipientIdentityFromMessage(account, msg);
        assertTrue(identity.getEmail().equalsIgnoreCase("test@example.org"));
    }

    @Test
    public void getRecipientIdentityFromMessage_withNoApplicableHeaders_returnsFirstIdentity() throws Exception {
        Identity identity = IdentityHelper.getRecipientIdentityFromMessage(account, msg);
        assertTrue(identity.getEmail().equalsIgnoreCase("default@example.org"));
    }


    static class DummyAccount extends Account {

        protected DummyAccount(Context context) {
            super(context);
        }
    }
}
