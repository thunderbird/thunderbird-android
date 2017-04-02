package com.fsck.k9.mail.autoconfiguration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class ProviderInfoTest {

    @Test
    public void fillDefaultPorts_withNoIncomingPortAndTypeImapAndTLS_shouldSetPort() {
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.incomingType = ProviderInfo.INCOMING_TYPE_IMAP;
        providerInfo.incomingSocketType = ProviderInfo.SOCKET_TYPE_SSL_OR_TLS;
        providerInfo.fillDefaultPorts();
        assertEquals(ProviderInfo.IMAP_SSL_OR_TLS_DEFAULT_PORT, providerInfo.incomingPort);
    }

    @Test
    public void fillDefaultPorts_withNoIncomingPortAndTypeImapAndSTLS_shouldSetPort() {
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.incomingType = ProviderInfo.INCOMING_TYPE_IMAP;
        providerInfo.incomingSocketType = ProviderInfo.SOCKET_TYPE_STARTTLS;
        providerInfo.fillDefaultPorts();
        assertEquals(ProviderInfo.IMAP_STARTTLS_DEFAULT_PORT, providerInfo.incomingPort);
    }

    @Test
    public void fillDefaultPorts_withNoIncomingPortAndTypeImapAndPlain_shouldNotSetPort() {
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.incomingType = ProviderInfo.INCOMING_TYPE_IMAP;
        providerInfo.fillDefaultPorts();
        assertEquals(-1, providerInfo.incomingPort);
    }

    @Test
    public void fillDefaultPorts_withNoIncomingPortAndTypePop3AndTLS_shouldSetPort() {
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.incomingType = ProviderInfo.INCOMING_TYPE_POP3;
        providerInfo.incomingSocketType = ProviderInfo.SOCKET_TYPE_SSL_OR_TLS;
        providerInfo.fillDefaultPorts();
        assertEquals(ProviderInfo.POP3_SSL_OR_TLS_DEFAULT_PORT, providerInfo.incomingPort);
    }

    @Test
    public void fillDefaultPorts_withNoIncomingPortAndTypePop3AndSTLS_shouldSetPort() {
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.incomingType = ProviderInfo.INCOMING_TYPE_POP3;
        providerInfo.incomingSocketType = ProviderInfo.SOCKET_TYPE_STARTTLS;
        providerInfo.fillDefaultPorts();
        assertEquals(ProviderInfo.POP3_STARTTLS_DEFAULT_PORT, providerInfo.incomingPort);
    }

    @Test
    public void fillDefaultPorts_withNoIncomingPortAndTypePop3AndPlain_shouldNotSetPort() {
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.incomingType = ProviderInfo.INCOMING_TYPE_POP3;
        providerInfo.fillDefaultPorts();
        assertEquals(-1, providerInfo.incomingPort);
    }

    @Test
    public void fillDefaultPorts_withNoOutgoingPortAndTypeSmtpAndTLS_shouldSetPort() {
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.outgoingType = ProviderInfo.OUTGOING_TYPE_SMTP;
        providerInfo.outgoingSocketType = ProviderInfo.SOCKET_TYPE_SSL_OR_TLS;
        providerInfo.fillDefaultPorts();
        assertEquals(ProviderInfo.SMTP_SSL_OR_TLS_DEFAULT_PORT, providerInfo.outgoingPort);
    }

    @Test
    public void fillDefaultPorts_withNoOutgoingPortAndTypeSmtpAndSTLS_shouldSetPort() {
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.outgoingType = ProviderInfo.OUTGOING_TYPE_SMTP;
        providerInfo.outgoingSocketType = ProviderInfo.SOCKET_TYPE_STARTTLS;
        providerInfo.fillDefaultPorts();
        assertEquals(ProviderInfo.SMTP_STARTTLS_DEFAULT_PORT, providerInfo.outgoingPort);
    }

    @Test
    public void fillDefaultPorts_withNoOutgoingPortAndTypeSmtpAndPlain_shouldNotSetPort() {
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.outgoingSocketType = ProviderInfo.OUTGOING_TYPE_SMTP;
        providerInfo.fillDefaultPorts();
        assertEquals(-1, providerInfo.outgoingPort);
    }

    @Test
    public void equalsMatchesForBlankProviderInfos() {
        ProviderInfo providerInfo1 = new ProviderInfo();
        ProviderInfo providerInfo2 = new ProviderInfo();
        assertTrue(providerInfo1.equals(providerInfo2));
        assertEquals(providerInfo1.hashCode(), providerInfo2.hashCode());
    }

    @Test
    public void equalsAndHashcodeMatchesForProvidersWithSameArbitraryContent() {
        ProviderInfo providerInfo1 = new ProviderInfo();
        providerInfo1.outgoingSocketType = "a";
        providerInfo1.outgoingType = "b";
        providerInfo1.outgoingAddr = "c";
        providerInfo1.outgoingPort = 1;
        providerInfo1.incomingSocketType = "d";
        providerInfo1.incomingType = "e";
        providerInfo1.incomingAddr = "f";
        providerInfo1.incomingPort = 2;
        ProviderInfo providerInfo2 = new ProviderInfo();
        providerInfo2.outgoingSocketType = providerInfo1.outgoingSocketType;
        providerInfo2.outgoingType = providerInfo1.outgoingType;
        providerInfo2.outgoingAddr = providerInfo1.outgoingAddr;
        providerInfo2.outgoingPort = providerInfo1.outgoingPort;
        providerInfo2.incomingSocketType = providerInfo1.incomingSocketType;
        providerInfo2.incomingType = providerInfo1.incomingType;
        providerInfo2.incomingAddr = providerInfo1.incomingAddr;
        providerInfo2.incomingPort = providerInfo1.incomingPort;

        assertTrue(providerInfo1.equals(providerInfo2));
        assertEquals(providerInfo1.hashCode(), providerInfo2.hashCode());
    }
}
