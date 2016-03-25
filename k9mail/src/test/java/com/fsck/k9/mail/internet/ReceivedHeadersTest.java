package com.fsck.k9.mail.internet;

import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReceivedHeadersTest {

    @Test
    public void wasMessageTransmittedSecurely_withNoHeaders_shouldReturnUnknown() throws MessagingException {
        String[] noReceivedHeaders = new String[]{};
        Message unknownMessage = mock(Message.class);
        when(unknownMessage.getHeader("Received")).thenReturn(noReceivedHeaders);

        assertEquals(SecureTransportState.UNKNOWN,
                ReceivedHeaders.wasMessageTransmittedSecurely(unknownMessage));
    }

    @Test
    public void wasMessageTransmittedSecurely_forInsecureMessage_shouldReturnFalse() throws MessagingException {
        String[] insecureReceivedHeaders = new String[]{
                " from localhost (localhost [127.0.0.1])\n" +
                        "by scarlet.richardwhiuk.com (Postfix)\n " +
                        "with ESMTP id BB1057BA98\n	" +
                        "for <philip@whiuk.com>; Fri, 25 Mar 2016 10:38:29 +0000 (GMT)",
                " from scarlet.richardwhiuk.com ([127.0.0.1])\n" +
                        "by localhost (scarlet.richardwhiuk.com [127.0.0.1]) (amavisd-new, port 10024)\n" +
                        "with ESMTP id kEaYiQPLCxiT for <philip@whiuk.com>;	Fri, 25 Mar 2016 10:38:27 +0000 (GMT)\n",
                " from serpentine.unitedhosting.co.uk (serpentine.unitedhosting.co.uk [83.223.125.16])\n" +
                        "(using TLSv1 with cipher DHE-RSA-AES256-SHA (256/256 bits))\n" +
                        "(No client certificate requested)\n" +
                        "by scarlet.richardwhiuk.com (Postfix)\n" +
                        "with ESMTPS id C19917A8E9\n" +
                        "for <philip@whiuk.com>; Fri, 25 Mar 2016 10:38:27 +0000 (GMT)\n",
                " from serpentine.unitedhosting.co.uk ([83.223.125.16]:56654 helo=serpentine.org.uk)\n" +
                        "by serpentine.unitedhosting.co.uk\n" +
                        "with esmtp (Exim 4.86_1)	(envelope-from <list-bounces@serpentine.org.uk>)\n" +
                        "id 1ajOr6-00020j-MM\n" +
                        "for philip@whiuk.com; Fri, 25 Mar 2016 10:20:36 +0000\n"
        };

        Message insecureMessage = mock(Message.class);
        when(insecureMessage.getHeader("Received")).thenReturn(insecureReceivedHeaders);


        assertEquals(SecureTransportState.INSECURE,
                ReceivedHeaders.wasMessageTransmittedSecurely(insecureMessage));
    }

    @Test
    public void wasMessageTransmittedSecurely_forSecureMessage_shouldReturnTrue() throws MessagingException {
        String[] secureReceivedHeaders = new String[]{
                " from localhost (localhost [127.0.0.1])\n" +
                        "by scarlet.richardwhiuk.com (Postfix)\n " +
                        "with ESMTP id BB1057BA98\n	" +
                        "for <philip@whiuk.com>; Fri, 25 Mar 2016 10:38:29 +0000 (GMT)",
                " from scarlet.richardwhiuk.com ([127.0.0.1])\n" +
                        "by localhost (scarlet.richardwhiuk.com [127.0.0.1]) (amavisd-new, port 10024)\n" +
                        "with ESMTP id kEaYiQPLCxiT for <philip@whiuk.com>;	Fri, 25 Mar 2016 10:38:27 +0000 (GMT)\n",
                " from serpentine.unitedhosting.co.uk (serpentine.unitedhosting.co.uk [83.223.125.16])\n" +
                        "(using TLSv1 with cipher DHE-RSA-AES256-SHA (256/256 bits))\n" +
                        "(No client certificate requested)\n" +
                        "by scarlet.richardwhiuk.com (Postfix)\n" +
                        "with ESMTPS id C19917A8E9\n" +
                        "for <philip@whiuk.com>; Fri, 25 Mar 2016 10:38:27 +0000 (GMT)\n",
                " from serpentine.unitedhosting.co.uk ([83.223.125.16]:56654 helo=serpentine.org.uk)\n" +
                        "by serpentine.unitedhosting.co.uk\n" +
                        "with esmtp (Exim 4.86_1)	(envelope-from <list-bounces@serpentine.org.uk>)\n" +
                        "id 1ajOr6-00020j-MM\n" +
                        "for philip@whiuk.com; Fri, 25 Mar 2016 10:20:36 +0000\n"
        };
        Message secureMessage = mock(Message.class);
        when(secureMessage.getHeader("Received")).thenReturn(secureReceivedHeaders);

        assertEquals(SecureTransportState.SECURE,
                ReceivedHeaders.wasMessageTransmittedSecurely(secureMessage));
    }
}
