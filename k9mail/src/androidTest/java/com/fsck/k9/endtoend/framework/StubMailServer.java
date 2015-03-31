package com.fsck.k9.endtoend.framework;

import android.util.Log;

import com.fsck.k9.K9;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

import java.util.Arrays;
import java.util.List;

import javax.mail.internet.MimeMessage;

/**
 * Configuration and management of a pair of stub servers for use by an account.
 */
public class StubMailServer {

    /**
     * Stub server that speaks SMTP, IMAP etc., that K-9 can talk to.
     */
    private GreenMail greenmail;

    private int offset;

    /**
     * ensures multiple instances do not conflict.
     */
    private static int sharedOffset = 0;

    public StubMailServer() {
        offset = ++sharedOffset;

        greenmail = new GreenMail(new ServerSetup[]{getImapServerSetup(), getSmtpServerSetup()});
        GreenMailUser user = greenmail
                .setUser(UserForImap.TEST_USER.emailAddress, UserForImap.TEST_USER.loginUsername,
                        UserForImap.TEST_USER.password);

        for (String mailbox : new String[] {"Drafts", "Spam", "Sent"}) {
            Log.d(K9.LOG_TAG, "creating mailbox "+mailbox);
            try {
                greenmail.getManagers().getImapHostManager().createMailbox(user, mailbox);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        greenmail.start();

    }

    private ServerSetup getSmtpServerSetup() {
        return new ServerSetup(10587 + offset, "127.0.0.2", ServerSetup.PROTOCOL_SMTP);
    }

    private ServerSetup getImapServerSetup() {
        return new ServerSetup(10143 + offset, "127.0.0.2", ServerSetup.PROTOCOL_IMAP);
    }

    public String getSmtpBindAddress() {
        return getSmtpServerSetup().getBindAddress();
    }

    public int getSmtpPort() {
        return getSmtpServerSetup().getPort();
    }

    public String getImapBindAddress() {
        return getImapServerSetup().getBindAddress();
    }

    public int getImapPort() {
        return getImapServerSetup().getPort();
    }

    public void stop() {
        greenmail.stop();
    }

    public List<MimeMessage> getReceivedMessages() {
        return Arrays.asList(greenmail.getReceivedMessages());
    }

    public void restart() {
        greenmail.start();

    }

    public void waitForMessage() {
        greenmail.waitForIncomingEmail(60000L, 1);
    }
}

