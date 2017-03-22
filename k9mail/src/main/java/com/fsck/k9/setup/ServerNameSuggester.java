package com.fsck.k9.setup;


import com.fsck.k9.mail.ServerSettings.Type;


public class ServerNameSuggester {
    public String suggestServerName(Type serverType, String domainPart) {
        switch (serverType) {
            case IMAP: {
                return "imap." + domainPart;
            }
            case SMTP: {
                return "smtp." + domainPart;
            }
            case WebDAV: {
                return "exchange." + domainPart;
            }
            case POP3: {
                return "pop3." + domainPart;
            }
        }

        throw new AssertionError("Missed case: " + serverType);
    }
}
