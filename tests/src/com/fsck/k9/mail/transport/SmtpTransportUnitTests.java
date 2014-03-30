/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fsck.k9.mail.transport;

import java.net.InetAddress;
import java.net.UnknownHostException;

import android.content.Context;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.fsck.k9.Account;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.TextBody;

class FakeAccount extends Account {
    public FakeAccount(Context context) {
        super(context);
    }

    // Avoid failing with the K9.app is often null.
    @Override
    protected String getDefaultProviderId() {
        // return StorageManager.getInstance(K9.app).getDefaultProviderId()
        return null;
    }
}

/**
 * This is a series of unit tests for the SMTP Sender class.  These tests must be locally
 * complete - no server(s) required.
 *
 * These tests can be run with the following command:
 *   runtest -c com.android.email.mail.transport.SmtpSenderUnitTests email
 */
@SmallTest
public class SmtpTransportUnitTests extends AndroidTestCase {

//    EmailProvider mProvider;
//    Context mProviderContext;
    Context mContext;
    private static final String LOCAL_ADDRESS = "1.2.3.4";

    /* These values are provided by setUp() */
    private SmtpTransport mSender = null;

    /* Simple test string and its base64 equivalent */
    private final static String TEST_STRING = "Hello, world";
    private final static String TEST_STRING_BASE64 = "SGVsbG8sIHdvcmxk";

    /**
     * Setup code.  We generate a lightweight SmtpSender for testing.
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();

        Account testAccount = new FakeAccount(mContext);
        testAccount.setTransportUri("smtp://user:password:CRAM_MD5@server:999");

        mSender = new SmtpTransport(testAccount);
    }

    /**
     * Confirms simple non-SSL non-TLS login
     */
    public void testSimpleLogin() throws Exception {

        MockTransport mockTransport = openAndInjectMockTransport();

        // try to open it
        setupOpen(mockTransport, null);
        mSender.open();
        assertTrue(mockTransport.isOpen());
    }

    public void testSendMessageSimpleMessage() throws Exception {
        MockTransport mockTransport = openAndInjectMockTransport();

        setupOpen(mockTransport, null);
        mSender.open();

        Message message = setupSimpleMessage();

        // prepare for the message traffic we'll see
        // TODO The test is a bit fragile, as we are order-dependent (and headers are not)
        expectSimpleMessage(mockTransport);
        mockTransport.expect("");

        // empty body
        mockTransport.expect("");

        // trailer
        mockTransport.expect("\\.", "250 2.0.0 kv2f1a00C02Rf8w3Vv mail accepted for delivery");
        mockTransport.expect("QUIT", "221 2.0.0 closing connection");
        mockTransport.expectClose();

        // Now trigger the transmission
        mSender.sendMessage(message);
        assertFalse(mockTransport.isOpen());
    }

    public void testSendMessageWithBody() throws Exception {
        MockTransport mockTransport = openAndInjectMockTransport();

        setupOpen(mockTransport, null);
        mSender.open();

        Message message = setupSimpleMessage();

        Body body = new TextBody(TEST_STRING);
        message.setBody(body);

        expectSimpleMessage(mockTransport);

        // more headers
        mockTransport.expect("MIME-Version: 1.0");
        mockTransport.expect("Content-Transfer-Encoding: 8bit");
        mockTransport.expect("Content-Type: text/plain;");
        mockTransport.expect(" charset=UTF-8");
        mockTransport.expect("");

        // body
        mockTransport.expect(TEST_STRING);

        // trailer
        mockTransport.expect("\\.", "250 2.0.0 kv2f1a00C02Rf8w3Vv mail accepted for delivery");
        mockTransport.expect("QUIT", "221 2.0.0 closing connection");
        mockTransport.expectClose();

        // Now trigger the transmission
        mSender.sendMessage(message);
        assertFalse(mockTransport.isOpen());
    }

    /**
     * Prepare to send a simple message (see setReceiveSimpleMessage)
     * @throws MessagingException
     */
    private Message setupSimpleMessage() throws MessagingException {
        MimeMessage message = new MimeMessage();
        message.setFrom(new Address("Jones@Registry.Org"));
        message.setRecipients(RecipientType.TO, new Address[] { new Address("Smith@Registry.Org") });
        message.setMessageId("1234567890");
        return message;
    }

    /**
     * Prepare to receive a simple message (see setupSimpleMessage)
     */
    private void expectSimpleMessage(MockTransport mockTransport) {
        mockTransport.expect("MAIL FROM:<Jones@Registry.Org> BODY=8BITMIME",
                "250 2.1.0 <Jones@Registry.Org> sender ok");
        mockTransport.expect("RCPT TO:<Smith@Registry.Org>",
                "250 2.1.5 <Smith@Registry.Org> recipient ok");
        mockTransport.expect("DATA", "354 enter mail, end with . on a line by itself");
        mockTransport.expect("From: Jones@Registry.Org");
        mockTransport.expect("To: Smith@Registry.Org");
        mockTransport.expect("Message-ID: .*");
    }

    /**
     * Set up a basic MockTransport. open it, and inject it into mStore
     */
    private MockTransport openAndInjectMockTransport() throws UnknownHostException {
        // Create mock transport and inject it into the SmtpSender that's already set up
        MockTransport mockTransport = new MockTransport();
        mSender.setTransport(mockTransport);
        mockTransport.setMockLocalAddress(InetAddress.getByName(LOCAL_ADDRESS));
        return mockTransport;
    }

    /**
     * Helper which stuffs the mock with enough strings to satisfy a call to SmtpSender.open()
     *
     * @param mockTransport the mock transport we're using
     * @param capabilities if non-null, comma-separated list of capabilities
     */
    private void setupOpen(MockTransport mockTransport, String capabilities) {

        setupOpenSingle(mockTransport, capabilities);

        // Since SmtpSender.sendMessage() does a close then open, we need to preset for the open
        mockTransport.expect("QUIT", "221 2.0.0 closing connection");
        mockTransport.expectClose();

        setupOpenSingle(mockTransport, capabilities);
    }

    private void setupOpenSingle(MockTransport mockTransport, String capabilities) {
        mockTransport.expect(null, "220 MockTransport 2000 Ready To Assist You Peewee");
        mockTransport.expect("EHLO .*", "250-10.20.30.40 hello");
        if (capabilities == null) {
            mockTransport.expect(null, "250-HELP");
            mockTransport.expect(null, "250-AUTH LOGIN PLAIN CRAM-MD5");
            mockTransport.expect(null, "250-SIZE 15728640");
            mockTransport.expect(null, "250-ENHANCEDSTATUSCODES");
            mockTransport.expect(null, "250-8BITMIME");
        } else {
            for (String capability : capabilities.split(",")) {
                mockTransport.expect(null, "250-" + capability);
            }
        }
        mockTransport.expect(null, "250+OK");

        // AUTH PLAIN
        // mockTransport.expect("AUTH PLAIN .*", "235 2.7.0 ... authentication succeeded");

        // AUTH CRAM-MD5
        mockTransport.expect("AUTH CRAM-MD5", "334 PDEyMzQ1Njc4OUBzZXJ2ZXIuZG9tYWluPg==");
        mockTransport.expect("dXNlciAwZTU5YWU2ZGFiMDRkYWY4MmNhOTE0OTc2MGRmMDA3Mg==", "235 2.7.0 ... authentication succeeded");
    }
}
