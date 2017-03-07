package com.fsck.k9.helper;

import android.content.Context;
import android.test.ApplicationTestCase;
import android.test.RenamingDelegatingContext;

import com.fsck.k9.Account;
import com.fsck.k9.Identity;
import com.fsck.k9.K9;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.internet.BinaryTempFileBody;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mailstore.ReconstructMessageFromDatabaseTest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yesalam on 7/3/17.
 */

public class IdentityHelperTest extends ApplicationTestCase<K9> {

    private Account account ;
    private MimeMessage msg ;

    public IdentityHelperTest() {
        super(K9.class);
    }


    @Override
    protected void setUp() throws Exception {
        super.setUp();

        RenamingDelegatingContext context = new RenamingDelegatingContext(getContext(), "db-test-");
        setContext(context);

        createApplication();

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

    private void setIdentity(){
       Identity identity  = new Identity() ;
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


    public void testXOriginalTo() throws Exception {
        Address[] addresses = {new Address("test2@mail.com")} ;
        msg.setRecipients(Message.RecipientType.X_ORIGINAL_TO,addresses);

        Identity identity = IdentityHelper.getRecipientIdentityFromMessage(account,msg);
        assertTrue(identity.getEmail().equalsIgnoreCase("test2@mail.com"));
    }

    public void testTo_withoutXOriginalTo() throws Exception {
        Identity eva = IdentityHelper.getRecipientIdentityFromMessage(account,msg) ;
        assertTrue(eva.getEmail().equalsIgnoreCase("eva@example.org"));
    }

    public void testDeliveredTo() throws Exception {
        Address[] addresses = {new Address("test2@mail.com")} ;

        msg.setRecipients(Message.RecipientType.DELIVERED_TO,addresses);
        msg.removeHeader("X-Original-To");

        Identity identity = IdentityHelper.getRecipientIdentityFromMessage(account,msg);
        assertTrue(identity.getEmail().equalsIgnoreCase("test2@mail.com"));

    }

    public void testXEnvelopeTo() throws Exception {
        Address[] addresses = {new Address("test@mail.com")} ;
        msg.setRecipients(Message.RecipientType.X_ENVELOPE_TO,addresses);
        msg.removeHeader("X-Original-To");
        msg.removeHeader("Delivered-To");
        Identity identity = IdentityHelper.getRecipientIdentityFromMessage(account,msg);
        assertTrue(identity.getEmail().equalsIgnoreCase("test@mail.com"));
    }

    public void testXEnvelopeTo_withXOriginalTo() throws Exception {
        Address[] addresses = {new Address("test@mail.com")} ;
        Address[] xoriginaladdress = {new Address("test2@mail.com")} ;
        msg.setRecipients(Message.RecipientType.X_ENVELOPE_TO,addresses);
        msg.setRecipients(Message.RecipientType.X_ORIGINAL_TO,xoriginaladdress);

        Identity identity = IdentityHelper.getRecipientIdentityFromMessage(account,msg);
        assertFalse(identity.getEmail().equalsIgnoreCase("test@mail.com"));
        assertTrue(identity.getEmail().equalsIgnoreCase("test2@mail.com"));
    }


    static class DummyAccount extends Account {

        protected DummyAccount(Context context) {
            super(context);
        }
    }
}
