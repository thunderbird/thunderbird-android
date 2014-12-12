package com.fsck.k9.local;

import com.fsck.k9.mail.internet.TextBody;

class LocalTextBody extends TextBody {
    /**
     * This is an HTML-ified version of the message for display purposes.
     */
    private final String mBodyForDisplay;

    public LocalTextBody(String body, String bodyForDisplay) {
        super(body);
        this.mBodyForDisplay = bodyForDisplay;
    }

    public String getBodyForDisplay() {
        return mBodyForDisplay;
    }

}//LocalTextBody
