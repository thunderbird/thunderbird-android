package com.fsck.k9.mail.autoconfiguration;


import java.util.ArrayList;
import java.util.List;

import com.fsck.k9.mail.autoconfiguration.AutoConfigure.ProviderInfo;
import org.junit.Assert;
import org.junit.Test;
import org.xbill.DNS.Name;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TextParseException;


/**
 * Created by daquexian on 6/19/17.
 */

public class SRVTest {
    @Test
    public void test1() throws TextParseException {
        List<SRVRecord> imapRecords = new ArrayList<>();
        List<SRVRecord> pop3Records = new ArrayList<>();
        List<SRVRecord> submissionRecords = new ArrayList<>();

        imapRecords.add(new SRVRecord(new Name("_imaps._tcp.test.com."), 0, 0, 5, 0, 993, new Name("imap.test.com.")));

        pop3Records.add(new SRVRecord(new Name("_pop3s._tcp.test.com."), 0, 0, 5, 0, 995, new Name("pop3.test.com.")));

        submissionRecords.add(
                new SRVRecord(new Name("_submission._tcp.test.com."), 0, 0, 5, 0, 587, new Name("smtp.test.com."))
        );

        AutoconfigureSrv autoconfigureSrv = new AutoconfigureSrv();
        ProviderInfo actual = autoconfigureSrv.parse(imapRecords, pop3Records, submissionRecords);

        ProviderInfo expected = new ProviderInfo();

        expected.incomingSocketType = ProviderInfo.SOCKET_TYPE_SSL_OR_TLS;
        expected.incomingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_SRV;
        expected.incomingPort = 993;
        expected.incomingType = ProviderInfo.INCOMING_TYPE_IMAP;
        expected.incomingAddr = "imap.test.com";
        expected.outgoingSocketType = ProviderInfo.SOCKET_TYPE_STARTTLS;
        expected.outgoingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_SRV;
        expected.outgoingPort = 587;
        expected.outgoingType = ProviderInfo.OUTGOING_TYPE_SMTP;
        expected.outgoingAddr = "smtp.test.com";

        Assert.assertEquals(expected, actual);
    }
    @Test
    public void test2() throws TextParseException {
        List<SRVRecord> imapRecords = new ArrayList<>();
        List<SRVRecord> pop3Records = new ArrayList<>();
        List<SRVRecord> submissionRecords = new ArrayList<>();

        imapRecords.add(
                new SRVRecord(new Name("_imaps._tcp.test.com."), 1, 0, 5, 0, 993, new Name("imap.test.com."))
        );

        imapRecords.add(
                new SRVRecord(new Name("_imaps._tcp.test.com."), 0, 0, 3, 0, 993, new Name("mail.test.com."))
        );

        pop3Records.add(
                new SRVRecord(new Name("_pop3s._tcp.test.com."), 0, 0, 5, 0, 995, new Name("pop3.test.com."))
        );

        submissionRecords.add(
                new SRVRecord(new Name("_submission._tcp.test.com."), 0, 0, 3, 0, 587, new Name("smtp.test.com."))
        );

        AutoconfigureSrv autoconfigureSrv = new AutoconfigureSrv();
        ProviderInfo actual = autoconfigureSrv.parse(imapRecords, pop3Records, submissionRecords);

        ProviderInfo expected = new ProviderInfo();

        expected.incomingSocketType = ProviderInfo.SOCKET_TYPE_SSL_OR_TLS;
        expected.incomingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_SRV;
        expected.incomingPort = 993;
        expected.incomingType = ProviderInfo.INCOMING_TYPE_IMAP;
        expected.incomingAddr = "mail.test.com";
        expected.outgoingSocketType = ProviderInfo.SOCKET_TYPE_STARTTLS;
        expected.outgoingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_SRV;
        expected.outgoingPort = 587;
        expected.outgoingType = ProviderInfo.OUTGOING_TYPE_SMTP;
        expected.outgoingAddr = "smtp.test.com";

        Assert.assertEquals(expected, actual);
    }
    @Test
    public void test3() throws TextParseException {
        List<SRVRecord> imapRecords = new ArrayList<>();
        List<SRVRecord> pop3Records = new ArrayList<>();
        List<SRVRecord> submissionRecords = new ArrayList<>();

        imapRecords.add(
                new SRVRecord(new Name("_imaps._tcp.test.com."), 1, 0, 5, 0, 993, new Name("imap.test.com."))
        );

        imapRecords.add(
                new SRVRecord(new Name("_imaps._tcp.test.com."), 0, 0, 5, 0, 993, new Name("mail.test.com."))
        );

        pop3Records.add(
                new SRVRecord(new Name("_pop3s._tcp.test.com."), 0, 0, 3, 0, 995, new Name("pop3.test.com."))
        );

        submissionRecords.add(
                new SRVRecord(new Name("_submission._tcp.test.com."), 0, 0, 3, 0, 587, new Name("smtp.test.com."))
        );

        AutoconfigureSrv autoconfigureSrv = new AutoconfigureSrv();
        ProviderInfo actual = autoconfigureSrv.parse(imapRecords, pop3Records, submissionRecords);

        ProviderInfo expected = new ProviderInfo();

        expected.incomingSocketType = ProviderInfo.SOCKET_TYPE_SSL_OR_TLS;
        expected.incomingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_SRV;
        expected.incomingPort = 995;
        expected.incomingType = ProviderInfo.INCOMING_TYPE_POP3;
        expected.incomingAddr = "pop3.test.com";
        expected.outgoingSocketType = ProviderInfo.SOCKET_TYPE_STARTTLS;
        expected.outgoingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_SRV;
        expected.outgoingPort = 587;
        expected.outgoingType = ProviderInfo.OUTGOING_TYPE_SMTP;
        expected.outgoingAddr = "smtp.test.com";

        Assert.assertEquals(expected, actual);
    }
    @Test
    public void test4() throws TextParseException {
        List<SRVRecord> imapRecords = new ArrayList<>();
        List<SRVRecord> pop3Records = new ArrayList<>();
        List<SRVRecord> submissionRecords = new ArrayList<>();

        imapRecords.add(
                new SRVRecord(new Name("_imaps._tcp.test.com."), 1, 0, 5, 0, 993, new Name("imap.test.com."))
        );

        imapRecords.add(
                new SRVRecord(new Name("_imap._tcp.test.com."), 0, 0, 3, 0, 143, new Name("mail.test.com."))
        );

        pop3Records.add(
                new SRVRecord(new Name("_pop3s._tcp.test.com."), 0, 0, 10, 0, 995, new Name("pop3.test.com."))
        );

        submissionRecords.add(
                new SRVRecord(new Name("_submission._tcp.test.com."), 0, 0, 3, 0, 587, new Name("smtp.test.com."))
        );

        AutoconfigureSrv autoconfigureSrv = new AutoconfigureSrv();
        ProviderInfo actual = autoconfigureSrv.parse(imapRecords, pop3Records, submissionRecords);

        ProviderInfo expected = new ProviderInfo();

        expected.incomingSocketType = ProviderInfo.SOCKET_TYPE_STARTTLS;
        expected.incomingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_SRV;
        expected.incomingPort = 143;
        expected.incomingType = ProviderInfo.INCOMING_TYPE_IMAP;
        expected.incomingAddr = "mail.test.com";
        expected.outgoingSocketType = ProviderInfo.SOCKET_TYPE_STARTTLS;
        expected.outgoingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_SRV;
        expected.outgoingPort = 587;
        expected.outgoingType = ProviderInfo.OUTGOING_TYPE_SMTP;
        expected.outgoingAddr = "smtp.test.com";

        Assert.assertEquals(expected, actual);
    }
    @Test
    public void testWeightSelection() throws TextParseException {
        List<SRVRecord> imapRecords = new ArrayList<>();
        List<SRVRecord> pop3Records = new ArrayList<>();
        List<SRVRecord> submissionRecords = new ArrayList<>();

        imapRecords.add(
                new SRVRecord(new Name("_imaps._tcp.test.com."), 1, 0, 5, 10, 993, new Name("imap.test.com."))
        );

        imapRecords.add(
                new SRVRecord(new Name("_imap._tcp.test.com."), 0, 0, 5, 10, 143, new Name("mail.test.com."))
        );

        pop3Records.add(
                new SRVRecord(new Name("_pop3s._tcp.test.com."), 0, 0, 10, 0, 995, new Name("pop3.test.com."))
        );

        submissionRecords.add(
                new SRVRecord(new Name("_submission._tcp.test.com."), 0, 0, 3, 0, 587, new Name("smtp.test.com."))
        );

        AutoconfigureSrv autoconfigureSrv = new AutoconfigureSrv();
        ProviderInfo actual = autoconfigureSrv.parse(imapRecords, pop3Records, submissionRecords);

        ProviderInfo expected1 = new ProviderInfo();

        expected1.incomingSocketType = ProviderInfo.SOCKET_TYPE_SSL_OR_TLS;
        expected1.incomingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_SRV;
        expected1.incomingPort = 993;
        expected1.incomingType = ProviderInfo.INCOMING_TYPE_IMAP;
        expected1.incomingAddr = "imap.test.com";
        expected1.outgoingSocketType = ProviderInfo.SOCKET_TYPE_STARTTLS;
        expected1.outgoingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_SRV;
        expected1.outgoingPort = 587;
        expected1.outgoingType = ProviderInfo.OUTGOING_TYPE_SMTP;
        expected1.outgoingAddr = "smtp.test.com";

        ProviderInfo expected2 = new ProviderInfo();

        expected2.incomingSocketType = ProviderInfo.SOCKET_TYPE_STARTTLS;
        expected2.incomingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_SRV;
        expected2.incomingPort = 143;
        expected2.incomingType = ProviderInfo.INCOMING_TYPE_IMAP;
        expected2.incomingAddr = "mail.test.com";
        expected2.outgoingSocketType = ProviderInfo.SOCKET_TYPE_STARTTLS;
        expected2.outgoingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_SRV;
        expected2.outgoingPort = 587;
        expected2.outgoingType = ProviderInfo.OUTGOING_TYPE_SMTP;
        expected2.outgoingAddr = "smtp.test.com";

        Assert.assertTrue(expected1.equals(actual) || expected2.equals(actual));
    }
    @Test
    public void testRecordsWithOnlyADot() throws TextParseException {
        List<SRVRecord> imapRecords = new ArrayList<>();
        List<SRVRecord> pop3Records = new ArrayList<>();
        List<SRVRecord> submissionRecords = new ArrayList<>();

        imapRecords.add(
                new SRVRecord(new Name("_imaps._tcp.test.com."), 1, 0, 5, 10, 993, new Name("."))
        );

        imapRecords.add(
                new SRVRecord(new Name("_imap._tcp.test.com."), 0, 0, 5, 10, 143, new Name("."))
        );

        pop3Records.add(
                new SRVRecord(new Name("_pop3s._tcp.test.com."), 0, 0, 10, 0, 995, new Name("."))
        );

        submissionRecords.add(
                new SRVRecord(new Name("_submission._tcp.test.com."), 0, 0, 3, 0, 587, new Name("."))
        );

        AutoconfigureSrv autoconfigureSrv = new AutoconfigureSrv();
        ProviderInfo actual = autoconfigureSrv.parse(imapRecords, pop3Records, submissionRecords);

        Assert.assertNull(actual);
    }
    @Test
    public void testUnusualPorts() throws TextParseException {
        List<SRVRecord> imapRecords = new ArrayList<>();
        List<SRVRecord> pop3Records = new ArrayList<>();
        List<SRVRecord> submissionRecords = new ArrayList<>();

        imapRecords.add(new SRVRecord(new Name("_imaps._tcp.test.com."), 0, 0, 5, 0, 123, new Name("imap.test.com.")));

        pop3Records.add(new SRVRecord(new Name("_pop3s._tcp.test.com."), 0, 0, 5, 0, 456, new Name("pop3.test.com.")));

        submissionRecords.add(
                new SRVRecord(new Name("_submission._tcp.test.com."), 0, 0, 5, 0, 789, new Name("smtp.test.com."))
        );

        AutoconfigureSrv autoconfigureSrv = new AutoconfigureSrv();
        ProviderInfo actual = autoconfigureSrv.parse(imapRecords, pop3Records, submissionRecords);

        ProviderInfo expected = new ProviderInfo();

        expected.incomingSocketType = ProviderInfo.SOCKET_TYPE_SSL_OR_TLS;
        expected.incomingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_SRV;
        expected.incomingPort = 123;
        expected.incomingType = ProviderInfo.INCOMING_TYPE_IMAP;
        expected.incomingAddr = "imap.test.com";
        expected.outgoingSocketType = ProviderInfo.SOCKET_TYPE_STARTTLS;
        expected.outgoingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_SRV;
        expected.outgoingPort = 789;
        expected.outgoingType = ProviderInfo.OUTGOING_TYPE_SMTP;
        expected.outgoingAddr = "smtp.test.com";

        Assert.assertEquals(expected, actual);
    }
}
