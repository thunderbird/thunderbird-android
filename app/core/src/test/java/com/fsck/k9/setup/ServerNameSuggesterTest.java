package com.fsck.k9.setup;


import com.fsck.k9.preferences.Protocols;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class ServerNameSuggesterTest {
    private ServerNameSuggester serverNameSuggester;


    @Before
    public void setUp() throws Exception {
        serverNameSuggester = new ServerNameSuggester();
    }

    @Test
    public void suggestServerName_forImapServer() throws Exception {
        String serverType = Protocols.IMAP;
        String domainPart = "example.org";

        String result = serverNameSuggester.suggestServerName(serverType, domainPart);

        assertEquals("imap.example.org", result);
    }

    @Test
    public void suggestServerName_forPop3Server() throws Exception {
        String serverType = Protocols.POP3;
        String domainPart = "example.org";

        String result = serverNameSuggester.suggestServerName(serverType, domainPart);

        assertEquals("pop3.example.org", result);
    }

    @Test
    public void suggestServerName_forWebDavServer() throws Exception {
        String serverType = Protocols.WEBDAV;
        String domainPart = "example.org";

        String result = serverNameSuggester.suggestServerName(serverType, domainPart);

        assertEquals("exchange.example.org", result);
    }

    @Test
    public void suggestServerName_forSmtpServer() throws Exception {
        String serverType = Protocols.SMTP;
        String domainPart = "example.org";

        String result = serverNameSuggester.suggestServerName(serverType, domainPart);

        assertEquals("smtp.example.org", result);
    }
}
