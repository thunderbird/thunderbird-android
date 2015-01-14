package com.fsck.k9.endtoend.framework;

import android.util.Log;

import com.fsck.k9.K9;
import com.icegreen.greenmail.user.GreenMailUser;
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
        GreenMailUser user = greenmail
                .setUser(UserForImap.TEST_USER.emailAddress, UserForImap.TEST_USER.loginUsername,
                        UserForImap.TEST_USER.password);

        for (String mailbox : new String[] {"Drafts", "Spam"}) {
            Log.d(K9.LOG_TAG, "creating mailbox "+mailbox);
            try {
                greenmail.getManagers().getImapHostManager().createMailbox(user, mailbox);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
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

    public void stop() {
        greenmail.stop();
    }
}

