package com.fsck.k9.mail.autoconfiguration;


import com.fsck.k9.mail.autoconfiguration.AutoConfigure.ProviderInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.Assert;
import org.junit.Test;


public class AutodiscoverTest {
    @Test
    public void test() {
        String xml = "\n" +
                "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
                "<Autodiscover xmlns=\"http://schemas.microsoft.com/exchange/autodiscover/responseschema/2006\">\n" +
                "<Response xmlns=\"http://schemas.microsoft.com/exchange/autodiscover/outlook/responseschema/2006a\">\n" +
                "<Account>\n" +
                "<AccountType>email</AccountType>\n" +
                "<Action>settings</Action>\n" +
                "<Protocol>\n" +
                "<Type>POP3</Type>\n" +
                "<Server>mail.contoso.com</Server>\n" +
                "<DomainRequired>off</DomainRequired>\n" +
                "<SPA>off</SPA>\n" +
                "<SSL>on</SSL>\n" +
                "<AuthRequired>on</AuthRequired>\n" +
                "</Protocol>\n" +
                "<Protocol>\n" +
                "<Type>SMTP</Type>\n" +
                "<Server>mail.contoso.com</Server>\n" +
                "<Port>587</Port>\n" +
                "<DomainRequired>off</DomainRequired>\n" +
                "<SPA>off</SPA>\n" +
                "<SSL>on</SSL>\n" +
                "<AuthRequired>on</AuthRequired>\n" +
                "<UsePOPAuth>on</UsePOPAuth>\n" +
                "<SMTPLast>on</SMTPLast>\n" +
                "</Protocol>\n" +
                "</Account>\n" +
                "</Response>\n" +
                "</Autodiscover>";

        AutoConfigureAutodiscover autodiscover = new AutoConfigureAutodiscover();

        Element account = Jsoup.parse(xml).select("Account").first();
        ProviderInfo actual = autodiscover.parse(account).fillDefaultPorts();

        ProviderInfo expected = new ProviderInfo();
        expected.incomingType = ProviderInfo.INCOMING_TYPE_POP3;
        expected.incomingSocketType = ProviderInfo.SOCKET_TYPE_SSL_OR_TLS;
        expected.incomingAddr = "mail.contoso.com";
        expected.incomingPort = 995;
        expected.incomingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_USER;
        expected.outgoingType = ProviderInfo.OUTGOING_TYPE_SMTP;
        expected.outgoingSocketType = ProviderInfo.SOCKET_TYPE_SSL_OR_TLS;
        expected.outgoingAddr = "mail.contoso.com";
        expected.outgoingPort = 587;
        expected.outgoingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_USER;

        Assert.assertEquals(expected, actual);
    }
    @Test
    public void testSocketTypeAndDefaultPort() {
        String xml = "\n" +
                "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
                "<Autodiscover xmlns=\"http://schemas.microsoft.com/exchange/autodiscover/responseschema/2006\">\n" +
                "<Response xmlns=\"http://schemas.microsoft.com/exchange/autodiscover/outlook/responseschema/2006a\">\n" +
                "<Account>\n" +
                "<AccountType>email</AccountType>\n" +
                "<Action>settings</Action>\n" +
                "<Protocol>\n" +
                "<Type>IMAP</Type>\n" +
                "<Server>mail.contoso.com</Server>\n" +
                "<DomainRequired>off</DomainRequired>\n" +
                "<SPA>off</SPA>\n" +
                "<SSL>off</SSL>\n" +
                "<AuthRequired>on</AuthRequired>\n" +
                "</Protocol>\n" +
                "<Protocol>\n" +
                "<Type>SMTP</Type>\n" +
                "<Server>smtp.contoso.com</Server>\n" +
                "<Port>587</Port>\n" +
                "<DomainRequired>off</DomainRequired>\n" +
                "<SPA>off</SPA>\n" +
                "<SSL>on</SSL>\n" +
                "<AuthRequired>on</AuthRequired>\n" +
                "<SMTPLast>on</SMTPLast>\n" +
                "</Protocol>\n" +
                "</Account>\n" +
                "</Response>\n" +
                "</Autodiscover>";

        AutoConfigureAutodiscover autodiscover = new AutoConfigureAutodiscover();

        Element account = Jsoup.parse(xml).select("Account").first();
        ProviderInfo actual = autodiscover.parse(account).fillDefaultPorts();

        ProviderInfo expected = new ProviderInfo();
        expected.incomingType = ProviderInfo.INCOMING_TYPE_IMAP;
        expected.incomingSocketType = ProviderInfo.SOCKET_TYPE_STARTTLS;
        expected.incomingAddr = "mail.contoso.com";
        expected.incomingPort = ProviderInfo.IMAP_STARTTLS_DEFAULT_PORT;
        expected.incomingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_USER;
        expected.outgoingType = ProviderInfo.OUTGOING_TYPE_SMTP;
        expected.outgoingSocketType = ProviderInfo.SOCKET_TYPE_SSL_OR_TLS;
        expected.outgoingAddr = "smtp.contoso.com";
        expected.outgoingPort = 587;
        expected.outgoingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_USER;

        Assert.assertEquals(expected, actual);
    }
    @Test
    public void testProtocolPreference() {
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
                "<Autodiscover xmlns=\"http://schemas.microsoft.com/exchange/autodiscover/responseschema/2006\">\n" +
                "<Response xmlns=\"http://schemas.microsoft.com/exchange/autodiscover/outlook/responseschema/2006a\">\n" +
                "<Account>\n" +
                "<AccountType>email</AccountType>\n" +
                "<Action>settings</Action>\n" +
                "<Protocol>\n" +
                "<Type>POP3</Type>\n" +
                "<Server>mail.contoso.com</Server>\n" +
                "<Port>995</Port>\n" +
                "<DomainRequired>off</DomainRequired>\n" +
                "<SPA>off</SPA>\n" +
                "<SSL>on</SSL>\n" +
                "<AuthRequired>on</AuthRequired>\n" +
                "</Protocol>\n" +
                "<Protocol>\n" +
                "<Type>IMAP</Type>\n" +
                "<Server>mail.contoso.com</Server>\n" +
                "<Port>993</Port>\n" +
                "<DomainRequired>off</DomainRequired>\n" +
                "<SPA>off</SPA>\n" +
                "<SSL>on</SSL>\n" +
                "<AuthRequired>on</AuthRequired>\n" +
                "</Protocol>\n" +
                "<Protocol>\n" +
                "<Type>SMTP</Type>\n" +
                "<Server>mail.contoso.com</Server>\n" +
                "<Port>587</Port>\n" +
                "<DomainRequired>off</DomainRequired>\n" +
                "<SPA>off</SPA>\n" +
                "<SSL>on</SSL>\n" +
                "<AuthRequired>on</AuthRequired>\n" +
                "<UsePOPAuth>on</UsePOPAuth>\n" +
                "<SMTPLast>on</SMTPLast>\n" +
                "</Protocol>\n" +
                "</Account>\n" +
                "</Response>\n" +
                "</Autodiscover>";

        AutoConfigureAutodiscover autodiscover = new AutoConfigureAutodiscover();

        Element account = Jsoup.parse(xml).select("Account").first();
        ProviderInfo actual = autodiscover.parse(account).fillDefaultPorts();

        ProviderInfo expected = new ProviderInfo();
        expected.incomingType = ProviderInfo.INCOMING_TYPE_POP3;
        expected.incomingSocketType = ProviderInfo.SOCKET_TYPE_SSL_OR_TLS;
        expected.incomingAddr = "mail.contoso.com";
        expected.incomingPort = 995;
        expected.incomingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_USER;
        expected.outgoingType = ProviderInfo.OUTGOING_TYPE_SMTP;
        expected.outgoingSocketType = ProviderInfo.SOCKET_TYPE_SSL_OR_TLS;
        expected.outgoingAddr = "mail.contoso.com";
        expected.outgoingPort = 587;
        expected.outgoingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_USER;

        Assert.assertEquals(expected, actual);
    }
    @Test
    public void testDomainRequiredAndCustomLoginNameAndDomainName() {
        String xml = "\n" +
                "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
                "<Autodiscover xmlns=\"http://schemas.microsoft.com/exchange/autodiscover/responseschema/2006\">\n" +
                "<Response xmlns=\"http://schemas.microsoft.com/exchange/autodiscover/outlook/responseschema/2006a\">\n" +
                "<Account>\n" +
                "<AccountType>email</AccountType>\n" +
                "<Action>settings</Action>\n" +
                "<Protocol>\n" +
                "<Type>IMAP</Type>\n" +
                "<Server>mail.contoso.com</Server>\n" +
                "<DomainRequired>on</DomainRequired>\n" +
                "<SPA>off</SPA>\n" +
                "<SSL>off</SSL>\n" +
                "<AuthRequired>on</AuthRequired>\n" +
                "</Protocol>\n" +
                "<Protocol>\n" +
                "<Type>SMTP</Type>\n" +
                "<Server>smtp.contoso.com</Server>\n" +
                "<Port>587</Port>\n" +
                "<LoginName>abc</LoginName>\n" +
                "<DomainRequired>on</DomainRequired>\n" +
                "<DomainName>def.com</DomainName>\n" +
                "<SPA>off</SPA>\n" +
                "<SSL>on</SSL>\n" +
                "<AuthRequired>on</AuthRequired>\n" +
                "<SMTPLast>on</SMTPLast>\n" +
                "</Protocol>\n" +
                "</Account>\n" +
                "</Response>\n" +
                "</Autodiscover>";

        AutoConfigureAutodiscover autodiscover = new AutoConfigureAutodiscover();

        Element account = Jsoup.parse(xml).select("Account").first();
        ProviderInfo actual = autodiscover.parse(account).fillDefaultPorts();

        ProviderInfo expected = new ProviderInfo();
        expected.incomingType = ProviderInfo.INCOMING_TYPE_IMAP;
        expected.incomingSocketType = ProviderInfo.SOCKET_TYPE_STARTTLS;
        expected.incomingAddr = "mail.contoso.com";
        expected.incomingPort = ProviderInfo.IMAP_STARTTLS_DEFAULT_PORT;
        expected.incomingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_USER + "@" +
                ProviderInfo.USERNAME_TEMPLATE_DOMAIN;
        expected.outgoingType = ProviderInfo.OUTGOING_TYPE_SMTP;
        expected.outgoingSocketType = ProviderInfo.SOCKET_TYPE_SSL_OR_TLS;
        expected.outgoingAddr = "smtp.contoso.com";
        expected.outgoingPort = 587;
        expected.outgoingUsernameTemplate = "abc@def.com";

        Assert.assertEquals(expected, actual);
    }
    @Test
    public void testUnusualPorts() {
        String xml = "\n" +
                "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
                "<Autodiscover xmlns=\"http://schemas.microsoft.com/exchange/autodiscover/responseschema/2006\">\n" +
                "<Response xmlns=\"http://schemas.microsoft.com/exchange/autodiscover/outlook/responseschema/2006a\">\n" +
                "<Account>\n" +
                "<AccountType>email</AccountType>\n" +
                "<Action>settings</Action>\n" +
                "<Protocol>\n" +
                "<Type>POP3</Type>\n" +
                "<Server>mail.contoso.com</Server>\n" +
                "<Port>123</Port>\n" +
                "<DomainRequired>off</DomainRequired>\n" +
                "<SPA>off</SPA>\n" +
                "<SSL>on</SSL>\n" +
                "<AuthRequired>on</AuthRequired>\n" +
                "</Protocol>\n" +
                "<Protocol>\n" +
                "<Type>SMTP</Type>\n" +
                "<Server>mail.contoso.com</Server>\n" +
                "<Port>456</Port>\n" +
                "<DomainRequired>off</DomainRequired>\n" +
                "<SPA>off</SPA>\n" +
                "<SSL>on</SSL>\n" +
                "<AuthRequired>on</AuthRequired>\n" +
                "<UsePOPAuth>on</UsePOPAuth>\n" +
                "<SMTPLast>on</SMTPLast>\n" +
                "</Protocol>\n" +
                "</Account>\n" +
                "</Response>\n" +
                "</Autodiscover>";

        AutoConfigureAutodiscover autodiscover = new AutoConfigureAutodiscover();

        Element account = Jsoup.parse(xml).select("Account").first();
        ProviderInfo actual = autodiscover.parse(account).fillDefaultPorts();

        ProviderInfo expected = new ProviderInfo();
        expected.incomingType = ProviderInfo.INCOMING_TYPE_POP3;
        expected.incomingSocketType = ProviderInfo.SOCKET_TYPE_SSL_OR_TLS;
        expected.incomingAddr = "mail.contoso.com";
        expected.incomingPort = 123;
        expected.incomingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_USER;
        expected.outgoingType = ProviderInfo.OUTGOING_TYPE_SMTP;
        expected.outgoingSocketType = ProviderInfo.SOCKET_TYPE_SSL_OR_TLS;
        expected.outgoingAddr = "mail.contoso.com";
        expected.outgoingPort = 456;
        expected.outgoingUsernameTemplate = ProviderInfo.USERNAME_TEMPLATE_USER;

        Assert.assertEquals(expected, actual);
    }
    @Test
    public void testMalformedXml() {
        String xml = "\n" +
                "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
                "<Autodiscover xmlns=\"http://schemas.microsoft.com/exchange/autodiscover/responseschema/2006\">\n" +
                "<Response xmlns=\"http://schemas.microsoft.com/exchange/autodiscover/outlook/responseschema/2006a\">\n" +
                "<Account>\n" +
                "<AccountType>email</AccountType>\n" +
                "<Action>settings</Action>\n" +
                "<Protocol>\n" +
                "<Type>POP3</Type>\n" +
                "<Server>mail.contoso.com</Server>\n" +
                "<DomainRequired>off</DomainRequired>\n" +
                "<SPA>off</SPA>\n" +
                "<SSL>on</SSL>\n" +
                "<AuthRequired>on</AuthRequired>\n" +
                "</Protocol>\n" +
                "<Protocol>\n" +
                "<Type>SMTP</Type>\n" +
                "<Port>587</Port>\n" +
                "<DomainRequired>off</DomainRequired>\n" +
                "<SPA>off</SPA>\n" +
                "<SSL>on</SSL>\n" +
                "<AuthRequired>on</AuthRequired>\n" +
                "<SMTPLast>on</SMTPLast>\n" +
                "</Protocol>\n" +
                "</Account>\n" +
                "</Response>\n" +
                "</Autodiscover>";

        AutoConfigureAutodiscover autodiscover = new AutoConfigureAutodiscover();

        Element account = Jsoup.parse(xml).select("Account").first();
        ProviderInfo actual = autodiscover.parse(account);

        Assert.assertNull(actual);
    }
}
