package com.fsck.k9.mail.store.imap;

import com.fsck.k9.mail.MessagingException;

class NegativeImapResponseException extends MessagingException {
    private static final long serialVersionUID = 3725007182205882394L;

    private final String alertText;

    public NegativeImapResponseException(String message, String alertText) {
        super(message, true);
        this.alertText = alertText;
    }

    public String getAlertText() {
        return alertText;
    }
}
