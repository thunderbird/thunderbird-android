package com.fsck.k9.mail.internet;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class JisSupportTest {

    // getJisVariantFromMessage via From header

    @Test
    public void getJisVariantFromMessage_docomoSender_returnsDocomo() throws Exception {
        MimeMessage message = new MimeMessage();
        message.setHeader("From", "user@docomo.ne.jp");
        assertEquals("docomo", JisSupport.getJisVariantFromMessage(message));
    }

    @Test
    public void getJisVariantFromMessage_softbankSender_returnsSoftbank() throws Exception {
        MimeMessage message = new MimeMessage();
        message.setHeader("From", "user@softbank.ne.jp");
        assertEquals("softbank", JisSupport.getJisVariantFromMessage(message));
    }

    @Test
    public void getJisVariantFromMessage_kddiSender_returnsKddi() throws Exception {
        MimeMessage message = new MimeMessage();
        message.setHeader("From", "user@ezweb.ne.jp");
        assertEquals("kddi", JisSupport.getJisVariantFromMessage(message));
    }

    @Test
    public void getJisVariantFromMessage_unknownSender_returnsNull() throws Exception {
        MimeMessage message = new MimeMessage();
        message.setHeader("From", "user@example.com");
        assertNull(JisSupport.getJisVariantFromMessage(message));
    }

    @Test
    public void getJisVariantFromMessage_iPhoneMailer_returnsIphone() throws Exception {
        MimeMessage message = new MimeMessage();
        message.setHeader("From", "user@example.com");
        message.setHeader("X-Mailer", "iPhone Mail A380");
        assertEquals("iphone", JisSupport.getJisVariantFromMessage(message));
    }

    // getJisVariantFromMessage via Received header FOR clause

    @Test
    public void getJisVariantFromMessage_receivedForDocomoAngleBracket_returnsDocomo() throws Exception {
        MimeMessage message = new MimeMessage();
        message.setHeader("From", "user@example.com");
        message.setHeader("Received", "from mail.example.com (mail.example.com [1.2.3.4]) for <user@docomo.ne.jp>;");
        assertEquals("docomo", JisSupport.getJisVariantFromMessage(message));
    }

    @Test
    public void getJisVariantFromMessage_receivedForDocomoNoAngleBracket_returnsDocomo() throws Exception {
        MimeMessage message = new MimeMessage();
        message.setHeader("From", "user@example.com");
        message.setHeader("Received", "from mail.example.com (mail.example.com [1.2.3.4]) for user@docomo.ne.jp;");
        assertEquals("docomo", JisSupport.getJisVariantFromMessage(message));
    }

    @Test
    public void getJisVariantFromMessage_receivedForEzwebAddress_returnsKddi() throws Exception {
        MimeMessage message = new MimeMessage();
        message.setHeader("From", "user@example.com");
        message.setHeader("Received", "from smtp.example.net for <user@ezweb.ne.jp>;");
        assertEquals("kddi", JisSupport.getJisVariantFromMessage(message));
    }

    @Test
    public void getJisVariantFromMessage_receivedForUnknownAddress_returnsNull() throws Exception {
        MimeMessage message = new MimeMessage();
        message.setHeader("From", "user@example.com");
        message.setHeader("Received", "from smtp.example.net for <user@example.com>;");
        assertNull(JisSupport.getJisVariantFromMessage(message));
    }

    @Test
    public void getJisVariantFromMessage_receivedWithoutFor_returnsNull() throws Exception {
        MimeMessage message = new MimeMessage();
        message.setHeader("From", "user@example.com");
        message.setHeader("Received", "from smtp.example.net by mx.example.com;");
        assertNull(JisSupport.getJisVariantFromMessage(message));
    }
}
