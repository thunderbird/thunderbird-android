package com.fsck.k9.mail.transport.mockServer;


import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeMessage;


public class TestMessage extends MimeMessage {
    private long mId;
    private int mAttachmentCount;
    private String mSubject;
    private String mPreview = "";
    private long mThreadId;
    private long mRootId;
    private long messagePartId;
    private String mimeType;


    public long getMessagePartId() {
        return messagePartId;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }


    public String getPreview() {
        return mPreview;
    }

    @Override
    public String getSubject() {
        return mSubject;
    }


    @Override
    public void setSubject(String subject) {
        mSubject = subject;
    }


    @Override
    public void setMessageId(String messageId) {
        mMessageId = messageId;
    }

    @Override
    public boolean hasAttachments() {
        return (mAttachmentCount > 0);
    }

    public void setAttachmentCount(int i) {
        mAttachmentCount = i;
    }

    public int getAttachmentCount() {
        return mAttachmentCount;
    }

    @Override
    public void setFrom(Address from) {
        this.mFrom = new Address[] { from };
    }

    @Override
    public void setReplyTo(Address[] replyTo) {
        if (replyTo == null || replyTo.length == 0) {
            mReplyTo = null;
        } else {
            mReplyTo = replyTo;
        }
    }

    /*
     * For performance reasons, we add headers instead of setting them (see super implementation)
     * which removes (expensive) them before adding them
     */
    @Override
    public void setRecipients(RecipientType type, Address[] addresses) {
        if (type == RecipientType.TO) {
            if (addresses == null || addresses.length == 0) {
                this.mTo = null;
            } else {
                this.mTo = addresses;
            }
        } else if (type == RecipientType.CC) {
            if (addresses == null || addresses.length == 0) {
                this.mCc = null;
            } else {
                this.mCc = addresses;
            }
        } else if (type == RecipientType.BCC) {
            if (addresses == null || addresses.length == 0) {
                this.mBcc = null;
            } else {
                this.mBcc = addresses;
            }
        } else {
            throw new IllegalArgumentException("Unrecognized recipient type.");
        }
    }

    public void setFlagInternal(Flag flag, boolean set) throws MessagingException {
        super.setFlag(flag, set);
    }

    @Override
    public long getId() {
        return mId;
    }

    @Override
    public TestMessage clone() {
        TestMessage message = new TestMessage();
        super.copy(message);

        message.mId = mId;
        message.mAttachmentCount = mAttachmentCount;
        message.mSubject = mSubject;
        message.mPreview = mPreview;

        return message;
    }

    public long getThreadId() {
        return mThreadId;
    }

    public long getRootId() {
        return mRootId;
    }

    @Override
    protected void copy(MimeMessage destination) {
        super.copy(destination);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        return result;
    }

    public boolean isBodyMissing() {
        return getBody() == null;
    }
}
