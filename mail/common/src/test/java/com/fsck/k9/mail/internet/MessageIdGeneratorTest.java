package com.fsck.k9.mail.internet;


import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.K9LibRobolectricTestRunner;
import com.fsck.k9.mail.Message;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;


@RunWith(K9LibRobolectricTestRunner.class)
public class MessageIdGeneratorTest {
    private MessageIdGenerator messageIdGenerator = new MessageIdGenerator(() -> "00000000-0000-4000-0000-000000000000");


    @Test
    public void generateMessageId_withFromAndReplyToAddress() throws Exception {
        Message message = new MimeMessage();
        message.setFrom(new Address("alice@example.org"));
        message.setReplyTo(Address.parse("bob@example.com"));
        
        String result = messageIdGenerator.generateMessageId(message);
        
        assertEquals("<00000000-0000-4000-0000-000000000000@example.org>", result);
    }

    @Test
    public void generateMessageId_withReplyToAddress() throws Exception {
        Message message = new MimeMessage();
        message.setReplyTo(Address.parse("bob@example.com"));
        
        String result = messageIdGenerator.generateMessageId(message);
        
        assertEquals("<00000000-0000-4000-0000-000000000000@example.com>", result);
    }

    @Test
    public void generateMessageId_withoutRelevantHeaders() throws Exception {
        Message message = new MimeMessage();
        
        String result = messageIdGenerator.generateMessageId(message);
        
        assertEquals("<00000000-0000-4000-0000-000000000000@fallback.k9mail.app>", result);
    }
}
