package com.fsck.k9.endtoend.framework;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

/**
 * Configuration and management of a pair of stub servers for use by an account.
 */
public class StubMailServer {
    private static final ServerSetup IMAP_SERVER_SETUP = new ServerSetup(10143, "127.0.0.2", ServerSetup.PROTOCOL_IMAP);
    private static final ServerSetup SMTP_SERVER_SETUP = new ServerSetup(10587, "127.0.0.2", ServerSetup.PROTOCOL_SMTP);

    /**
     * Stub server that speaks SMTP, IMAP etc., that K-9 can talk to.
     */
    private GreenMail greenmail;

    public StubMailServer() {

        greenmail = new GreenMail(new ServerSetup[]{IMAP_SERVER_SETUP, SMTP_SERVER_SETUP});
        greenmail.setUser(UserForImap.TEST_USER.emailAddress, UserForImap.TEST_USER.loginUsername, UserForImap.TEST_USER.password);
        greenmail.start();
    }

    public String getSmtpBindAddress() {
        return SMTP_SERVER_SETUP.getBindAddress();
    }

    public int getSmtpPort() {
        return SMTP_SERVER_SETUP.getPort();
    }

    public String getImapBindAddress() {
        return IMAP_SERVER_SETUP.getBindAddress();
    }

    public int getImapPort() {
        return IMAP_SERVER_SETUP.getPort();
    }
}

