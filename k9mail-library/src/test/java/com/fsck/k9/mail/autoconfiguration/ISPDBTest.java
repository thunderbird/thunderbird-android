package com.fsck.k9.mail.autoconfiguration;


import com.fsck.k9.mail.autoconfiguration.AutoConfigure.ProviderInfo;
import junit.framework.Assert;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;


public class ISPDBTest {
    @Test
    public void testImap() {
        String xml = "<clientConfig version=\"1.1\">\n" +
                "  <emailProvider id=\"a1.net\">\n" +
                "    <domain>a1.net</domain>\n" +
                "    <domain>aon.at</domain>\n" +
                "    <displayName>a1.net</displayName>\n" +
                "    <displayShortName>a1.net</displayShortName>\n" +
                "    <incomingServer type=\"imap\">\n" +
                "      <hostname>securemail.a1.net</hostname>\n" +
                "      <port>993</port>\n" +
                "      <socketType>SSL</socketType>\n" +
                "      <username>%EMAILADDRESS%</username>\n" +
                "      <authentication>password-cleartext</authentication>\n" +
                "    </incomingServer>\n" +
                "    <outgoingServer type=\"smtp\">\n" +
                "      <hostname>securemail.a1.net</hostname>\n" +
                "      <port>587</port>\n" +
                "      <socketType>STARTTLS</socketType>\n" +
                "      <username>%EMAILADDRESS%</username>\n" +
                "      <authentication>password-cleartext</authentication>\n" +
                "    </outgoingServer>\n" +
                "  </emailProvider>\n" +
                "</clientConfig>";

        Document document = Jsoup.parse(xml);

        AutoconfigureISPDB autoconfigureISPDB = new AutoconfigureISPDB();
        ProviderInfo actual = autoconfigureISPDB.parse(document).fillDefaultPorts();
        ProviderInfo expected = new ProviderInfo();
        expected.incomingType = ProviderInfo.INCOMING_TYPE_IMAP;
        expected.incomingSocketType = ProviderInfo.SOCKET_TYPE_SSL_OR_TLS;
        expected.incomingAddr = "securemail.a1.net";
        expected.incomingPort = 993;
        expected.incomingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_EMAIL;
        expected.outgoingType = ProviderInfo.OUTGOING_TYPE_SMTP;
        expected.outgoingSocketType = ProviderInfo.SOCKET_TYPE_STARTTLS;
        expected.outgoingAddr = "securemail.a1.net";
        expected.outgoingPort = 587;
        expected.outgoingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_EMAIL;

        Assert.assertEquals(expected, actual);
    }
    @Test
    public void testPop3() {
        String xml = "<clientConfig version=\"1.1\">\n" +
                "  <emailProvider id=\"elpasotel.net\">\n" +
                "    <domain>elpasotel.net</domain>\n" +
                "    <displayName>Elpasotel.net</displayName>\n" +
                "    <displayShortName>Elpasotel</displayShortName>\n" +
                "    <incomingServer type=\"pop3\">\n" +
                "      <hostname>pop3.elpasotel.net</hostname>\n" +
                "      <port>995</port>\n" +
                "      <socketType>SSL</socketType>\n" +
                "      <authentication>password-cleartext</authentication>\n" +
                "      <username>%EMAILADDRESS%</username>\n" +
                "      <pop3>\n" +
                "        <leaveMessagesOnServer>true</leaveMessagesOnServer>\n" +
                "        <!-- CenturyLink recommended setting. (see documentation link)-->\n" +
                "      </pop3>\n" +
                "    </incomingServer>\n" +
                "    <incomingServer type=\"imap\">\n" +
                "      <hostname>mail.elpasotel.net</hostname>\n" +
                "      <port>993</port>\n" +
                "      <socketType>SSL</socketType>\n" +
                "      <authentication>password-cleartext</authentication>\n" +
                "      <username>%EMAILADDRESS%</username>\n" +
                "    </incomingServer>\n" +
                "    <!-- CenturyLink don't support IMAP. CenturyLink are providing the IMAP settings for self-help only. \n" +
                "\t\t       (see documentation links below) -->\n" +
                "    <outgoingServer type=\"smtp\">\n" +
                "      <hostname>smtp.elpasotel.net</hostname>\n" +
                "      <port>587</port>\n" +
                "      <socketType>STARTTLS</socketType>\n" +
                "      <authentication>password-cleartext</authentication>\n" +
                "      <username>%EMAILADDRESS%</username>\n" +
                "    </outgoingServer>\n" +
                "    <documentation url=\"http://www.centurylink.com/help/index.php?assetid=239\">\n" +
                "      <descr lang=\"en\">CenturyLink | How to set up your email to go through POP3 and SMTP</descr>\n" +
                "    </documentation>\n" +
                "    <documentation url=\"http://www.centurylink.com/help/index.php?assetid=239#elpasotelnet\">\n" +
                "      <descr lang=\"en\">CenturyLink | How to set up your email to go through POP3 and SMTP #Elpasotel.net</descr>\n" +
                "    </documentation>\n" +
                "  </emailProvider>\n" +
                "</clientConfig>\n";

        Document document = Jsoup.parse(xml);

        AutoconfigureISPDB autoconfigureISPDB = new AutoconfigureISPDB();
        ProviderInfo actual = autoconfigureISPDB.parse(document).fillDefaultPorts();
        ProviderInfo expected = new ProviderInfo();
        expected.incomingType = ProviderInfo.INCOMING_TYPE_POP3;
        expected.incomingSocketType = ProviderInfo.SOCKET_TYPE_SSL_OR_TLS;
        expected.incomingAddr = "pop3.elpasotel.net";
        expected.incomingPort = 995;
        expected.incomingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_EMAIL;
        expected.outgoingType = ProviderInfo.OUTGOING_TYPE_SMTP;
        expected.outgoingSocketType = ProviderInfo.SOCKET_TYPE_STARTTLS;
        expected.outgoingAddr = "smtp.elpasotel.net";
        expected.outgoingPort = 587;
        expected.outgoingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_EMAIL;

        Assert.assertEquals(expected, actual);
    }
    @Test
    public void testManyItems() {
        String xml = "\n" +
                "<clientConfig version=\"1.1\">\n" +
                "  <emailProvider id=\"mail.com\">\n" +
                "    <incomingServer type=\"imap\">\n" +
                "      <hostname>imap.mail.com</hostname>\n" +
                "      <port>993</port>\n" +
                "      <socketType>SSL</socketType>\n" +
                "      <username>%EMAILADDRESS%</username>\n" +
                "      <authentication>password-cleartext</authentication>\n" +
                "    </incomingServer>\n" +
                "    <incomingServer type=\"imap\">\n" +
                "      <hostname>imap.mail.com</hostname>\n" +
                "      <port>143</port>\n" +
                "      <socketType>STARTTLS</socketType>\n" +
                "      <username>%EMAILADDRESS%</username>\n" +
                "      <authentication>password-cleartext</authentication>\n" +
                "    </incomingServer>\n" +
                "    <incomingServer type=\"pop3\">\n" +
                "      <hostname>pop.mail.com</hostname>\n" +
                "      <port>995</port>\n" +
                "      <socketType>SSL</socketType>\n" +
                "      <username>%EMAILADDRESS%</username>\n" +
                "      <authentication>password-cleartext</authentication>\n" +
                "    </incomingServer>\n" +
                "    <incomingServer type=\"pop3\">\n" +
                "      <hostname>pop.mail.com</hostname>\n" +
                "      <port>110</port>\n" +
                "      <socketType>STARTTLS</socketType>\n" +
                "      <username>%EMAILADDRESS%</username>\n" +
                "      <authentication>password-cleartext</authentication>\n" +
                "    </incomingServer>\n" +
                "    <outgoingServer type=\"smtp\">\n" +
                "      <hostname>smtp.mail.com</hostname>\n" +
                "      <port>465</port>\n" +
                "      <socketType>SSL</socketType>\n" +
                "      <username>%EMAILADDRESS%</username>\n" +
                "      <authentication>password-cleartext</authentication>\n" +
                "    </outgoingServer>\n" +
                "    <outgoingServer type=\"smtp\">\n" +
                "      <hostname>smtp.mail.com</hostname>\n" +
                "      <port>587</port>\n" +
                "      <socketType>STARTTLS</socketType>\n" +
                "      <username>%EMAILADDRESS%</username>\n" +
                "      <authentication>password-cleartext</authentication>\n" +
                "    </outgoingServer>\n" +
                "  </emailProvider>\n" +
                "</clientConfig>\n";

        Document document = Jsoup.parse(xml);

        AutoconfigureISPDB autoconfigureISPDB = new AutoconfigureISPDB();
        ProviderInfo actual = autoconfigureISPDB.parse(document).fillDefaultPorts();
        ProviderInfo expected = new ProviderInfo();
        expected.incomingType = ProviderInfo.INCOMING_TYPE_IMAP;
        expected.incomingSocketType = ProviderInfo.SOCKET_TYPE_SSL_OR_TLS;
        expected.incomingAddr = "imap.mail.com";
        expected.incomingPort = 993;
        expected.incomingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_EMAIL;
        expected.outgoingType = ProviderInfo.OUTGOING_TYPE_SMTP;
        expected.outgoingSocketType = ProviderInfo.SOCKET_TYPE_SSL_OR_TLS;
        expected.outgoingAddr = "smtp.mail.com";
        expected.outgoingPort = 465;
        expected.outgoingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_EMAIL;

        Assert.assertEquals(expected, actual);
    }
    @Test
    public void testAnonymousSMTP() {
        String xml = "\n" +
                "<clientConfig version=\"1.1\">\n" +
                "  <emailProvider id=\"iiyama-catv.ne.jp\">\n" +
                "    <domain>iiyama-catv.ne.jp</domain>\n" +
                "    <displayName>ケーブルテレビiネット飯山</displayName>\n" +
                "    <displayShortName>iネット飯山</displayShortName>\n" +
                "    <incomingServer type=\"pop3\">\n" +
                "      <hostname>mail.iiyama-catv.ne.jp</hostname>\n" +
                "      <port>110</port>\n" +
                "      <socketType>plain</socketType>\n" +
                "      <username>%EMAILLOCALPART%</username>\n" +
                "      <authentication>password-cleartext</authentication>\n" +
                "    </incomingServer>\n" +
                "    <outgoingServer type=\"smtp\">\n" +
                "      <hostname>smtp.iiyama-catv.ne.jp</hostname>\n" +
                "      <port>25</port>\n" +
                "      <socketType>plain</socketType>\n" +
                "      <authentication>none</authentication>\n" +
                "    </outgoingServer>\n" +
                "  </emailProvider>\n" +
                "</clientConfig>\n";

        Document document = Jsoup.parse(xml);

        AutoconfigureISPDB autoconfigureISPDB = new AutoconfigureISPDB();
        ProviderInfo actual = autoconfigureISPDB.parse(document).fillDefaultPorts();
        ProviderInfo expected = new ProviderInfo();
        expected.incomingType = ProviderInfo.INCOMING_TYPE_POP3;
        expected.incomingSocketType = "";
        expected.incomingAddr = "mail.iiyama-catv.ne.jp";
        expected.incomingPort = 110;
        expected.incomingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_USER;
        expected.outgoingType = ProviderInfo.OUTGOING_TYPE_SMTP;
        expected.outgoingSocketType = "";
        expected.outgoingAddr = "smtp.iiyama-catv.ne.jp";
        expected.outgoingPort = 25;
        expected.outgoingUsernameTemplate = "";

        Assert.assertEquals(expected, actual);
    }
    @Test
    public void testUnusualUsername() {
        String xml = "\n" +
                "<clientConfig version=\"1.1\">\n" +
                "  <emailProvider id=\"iijmio-mail.jp\">\n" +
                "    <displayName>IIJmio セーフティメール</displayName>\n" +
                "    <displayShortName>IIJmio</displayShortName>\n" +
                "    <incomingServer type=\"pop3\">\n" +
                "      <hostname>mbox.iijmio-mail.jp</hostname>\n" +
                "      <port>110</port>\n" +
                "      <socketType>STARTTLS</socketType>\n" +
                "      <username>%EMAILLOCALPART%.%EMAILDOMAIN%</username>\n" +
                "      <authentication>password-encrypted</authentication>\n" +
                "    </incomingServer>\n" +
                "    <outgoingServer type=\"smtp\">\n" +
                "      <hostname>mbox.iijmio-mail.jp</hostname>\n" +
                "      <port>587</port>\n" +
                "      <socketType>STARTTLS</socketType>\n" +
                "      <username>%EMAILLOCALPART%.%EMAILDOMAIN%</username>\n" +
                "      <authentication>password-encrypted</authentication>\n" +
                "    </outgoingServer>\n" +
                "  </emailProvider>\n" +
                "</clientConfig>";

        Document document = Jsoup.parse(xml);

        AutoconfigureISPDB autoconfigureISPDB = new AutoconfigureISPDB();
        ProviderInfo actual = autoconfigureISPDB.parse(document).fillDefaultPorts();
        ProviderInfo expected = new ProviderInfo();
        expected.incomingType = ProviderInfo.INCOMING_TYPE_POP3;
        expected.incomingSocketType = ProviderInfo.SOCKET_TYPE_STARTTLS;
        expected.incomingAddr = "mbox.iijmio-mail.jp";
        expected.incomingPort = 110;
        expected.incomingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_USER + "." +
                ProviderInfo.USERNAME_TEMPLATE_DOMAIN;
        expected.outgoingType = ProviderInfo.OUTGOING_TYPE_SMTP;
        expected.outgoingSocketType = ProviderInfo.SOCKET_TYPE_STARTTLS;
        expected.outgoingAddr = "mbox.iijmio-mail.jp";
        expected.outgoingPort = 587;
        expected.outgoingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_USER + "." +
                ProviderInfo.USERNAME_TEMPLATE_DOMAIN;

        Assert.assertEquals(expected, actual);
    }
    @Test
    public void testUnusualPorts() {
        String xml = "<clientConfig version=\"1.1\">\n" +
                "  <emailProvider id=\"a1.net\">\n" +
                "    <domain>a1.net</domain>\n" +
                "    <domain>aon.at</domain>\n" +
                "    <displayName>a1.net</displayName>\n" +
                "    <displayShortName>a1.net</displayShortName>\n" +
                "    <incomingServer type=\"imap\">\n" +
                "      <hostname>securemail.a1.net</hostname>\n" +
                "      <port>123</port>\n" +
                "      <socketType>SSL</socketType>\n" +
                "      <username>%EMAILADDRESS%</username>\n" +
                "      <authentication>password-cleartext</authentication>\n" +
                "    </incomingServer>\n" +
                "    <outgoingServer type=\"smtp\">\n" +
                "      <hostname>securemail.a1.net</hostname>\n" +
                "      <port>456</port>\n" +
                "      <socketType>STARTTLS</socketType>\n" +
                "      <username>%EMAILADDRESS%</username>\n" +
                "      <authentication>password-cleartext</authentication>\n" +
                "    </outgoingServer>\n" +
                "  </emailProvider>\n" +
                "</clientConfig>";

        Document document = Jsoup.parse(xml);

        AutoconfigureISPDB autoconfigureISPDB = new AutoconfigureISPDB();
        ProviderInfo actual = autoconfigureISPDB.parse(document).fillDefaultPorts();
        ProviderInfo expected = new ProviderInfo();
        expected.incomingType = ProviderInfo.INCOMING_TYPE_IMAP;
        expected.incomingSocketType = ProviderInfo.SOCKET_TYPE_SSL_OR_TLS;
        expected.incomingAddr = "securemail.a1.net";
        expected.incomingPort = 123;
        expected.incomingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_EMAIL;
        expected.outgoingType = ProviderInfo.OUTGOING_TYPE_SMTP;
        expected.outgoingSocketType = ProviderInfo.SOCKET_TYPE_STARTTLS;
        expected.outgoingAddr = "securemail.a1.net";
        expected.outgoingPort = 456;
        expected.outgoingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_EMAIL;

        Assert.assertEquals(expected, actual);
    }
    @Test
    public void testMalformedXmlWithoutIncoming() {
        String xml = "<clientConfig version=\"1.1\">\n" +
                "  <emailProvider id=\"a1.net\">\n" +
                "    <domain>a1.net</domain>\n" +
                "    <domain>aon.at</domain>\n" +
                "    <displayName>a1.net</displayName>\n" +
                "    <displayShortName>a1.net</displayShortName>\n" +
                "    <outgoingServer type=\"smtp\">\n" +
                "      <hostname>securemail.a1.net</hostname>\n" +
                "      <port>456</port>\n" +
                "      <socketType>STARTTLS</socketType>\n" +
                "      <username>%EMAILADDRESS%</username>\n" +
                "      <authentication>password-cleartext</authentication>\n" +
                "    </outgoingServer>\n" +
                "  </emailProvider>\n" +
                "</clientConfig>";

        Document document = Jsoup.parse(xml);

        AutoconfigureISPDB autoconfigureISPDB = new AutoconfigureISPDB();
        ProviderInfo actual = autoconfigureISPDB.parse(document);

        Assert.assertNull(actual);
    }
    @Test
    public void testMalformedXmlWithoutHostname() {
        String xml = "<clientConfig version=\"1.1\">\n" +
                "  <emailProvider id=\"a1.net\">\n" +
                "    <domain>a1.net</domain>\n" +
                "    <domain>aon.at</domain>\n" +
                "    <displayName>a1.net</displayName>\n" +
                "    <displayShortName>a1.net</displayShortName>\n" +
                "    <incomingServer type=\"imap\">\n" +
                "      <port>123</port>\n" +
                "      <socketType>SSL</socketType>\n" +
                "      <username>%EMAILADDRESS%</username>\n" +
                "      <authentication>password-cleartext</authentication>\n" +
                "    </incomingServer>\n" +
                "    <outgoingServer type=\"smtp\">\n" +
                "      <hostname>securemail.a1.net</hostname>\n" +
                "      <port>456</port>\n" +
                "      <socketType>STARTTLS</socketType>\n" +
                "      <username>%EMAILADDRESS%</username>\n" +
                "      <authentication>password-cleartext</authentication>\n" +
                "    </outgoingServer>\n" +
                "  </emailProvider>\n" +
                "</clientConfig>";

        Document document = Jsoup.parse(xml);

        AutoconfigureISPDB autoconfigureISPDB = new AutoconfigureISPDB();
        ProviderInfo actual = autoconfigureISPDB.parse(document);

        Assert.assertNull(actual);
    }
}
