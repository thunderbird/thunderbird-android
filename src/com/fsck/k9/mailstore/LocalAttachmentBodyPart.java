package com.fsck.k9.mailstore;

import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeBodyPart;

public class LocalAttachmentBodyPart extends MimeBodyPart {
    private long mAttachmentId = -1;

    public LocalAttachmentBodyPart(Body body, long attachmentId) throws MessagingException {
        super(body);
        mAttachmentId = attachmentId;
    }

    /**
     * Returns the local attachment id of this body, or -1 if it is not stored.
     * @return
     */
    public long getAttachmentId() {
        return mAttachmentId;
    }

    public void setAttachmentId(long attachmentId) {
        mAttachmentId = attachmentId;
    }

    @Override
    public String toString() {
        return "" + mAttachmentId;
    }
}
