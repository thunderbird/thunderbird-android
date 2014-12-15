
package com.fsck.k9.mail;

import com.fsck.k9.mail.internet.MessageExtractor;
import com.fsck.k9.mail.internet.MimeUtility;

public abstract class BodyPart implements Part {
    private Multipart mParent;

    public Multipart getParent() {
        return mParent;
    }

    public void setParent(Multipart parent) {
        mParent = parent;
    }

    public abstract void setEncoding(String encoding) throws MessagingException;

    @Override
    public String getText() {
        return MessageExtractor.getTextFromPart(this);
    }

    @Override
    public Part findFirstPartByMimeType(String mimeType) throws MessagingException {
        return MimeUtility.findFirstPartByMimeType(this, mimeType);
    }
}
