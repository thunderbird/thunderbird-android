package com.fsck.k9.setup;


import com.fsck.k9.K9RobolectricTestRunner;
import com.fsck.k9.mail.ServerSettings.Type;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;


@RunWith(K9RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ServerNameSuggesterTest {
    private ServerNameSuggester serverNameSuggester;


    @Before
    public void setUp() throws Exception {
        serverNameSuggester = new ServerNameSuggester();
    }

    @Test
    public void suggestServerName_forImapServer() throws Exception {
        Type serverType = Type.IMAP;
        String domainPart = "example.org";

        String result = serverNameSuggester.suggestServerName(serverType, domainPart);

        assertEquals("imap.example.org", result);
    }

    @Test
    public void suggestServerName_forPop3Server() throws Exception {
        Type serverType = Type.POP3;
        String domainPart = "example.org";

        String result = serverNameSuggester.suggestServerName(serverType, domainPart);

        assertEquals("pop3.example.org", result);
    }

    @Test
    public void suggestServerName_forWebDavServer() throws Exception {
        Type serverType = Type.WebDAV;
        String domainPart = "example.org";

        String result = serverNameSuggester.suggestServerName(serverType, domainPart);

        assertEquals("exchange.example.org", result);
    }

    @Test
    public void suggestServerName_forSmtpServer() throws Exception {
        Type serverType = Type.SMTP;
        String domainPart = "example.org";

        String result = serverNameSuggester.suggestServerName(serverType, domainPart);

        assertEquals("smtp.example.org", result);
    }
}
