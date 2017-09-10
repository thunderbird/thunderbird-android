
package com.fsck.k9.mail;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Set;

import android.support.annotation.NonNull;

import com.fsck.k9.mail.filter.CountingOutputStream;
import com.fsck.k9.mail.filter.EOLConvertingOutputStream;
import timber.log.Timber;


public abstract class Message implements Part, Body {

    public enum RecipientType {
        TO, CC, BCC, X_ORIGINAL_TO, DELIVERED_TO, X_ENVELOPE_TO
    }

    protected String mUid;

    private Set<Flag> mFlags = EnumSet.noneOf(Flag.class);

    private Date mInternalDate;

    protected Folder mFolder;

    public boolean olderThan(Date earliestDate) {
        if (earliestDate == null) {
            return false;
        }
        Date myDate = getSentDate();
        if (myDate == null) {
            myDate = getInternalDate();
        }
        if (myDate != null) {
            return myDate.before(earliestDate);
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Message)) {
            return false;
        }
        Message other = (Message)o;
        return (getUid().equals(other.getUid())
                && getFolder().getId().equals(other.getFolder().getId()));
    }

    @Override
    public int hashCode() {
        final int MULTIPLIER = 31;

        int result = 1;
        result = MULTIPLIER * result + (mFolder != null ? mFolder.getId().hashCode() : 0);
        result = MULTIPLIER * result + mUid.hashCode();
        return result;
    }

    public String getUid() {
        return mUid;
    }

    public void setUid(String uid) {
        this.mUid = uid;
    }

    public Folder getFolder() {
        return mFolder;
    }

    public abstract String getSubject();

    public abstract void setSubject(String subject);

    public Date getInternalDate() {
        return mInternalDate;
    }

    public void setInternalDate(Date internalDate) {
        this.mInternalDate = internalDate;
    }

    public abstract Date getSentDate();

    public abstract void setSentDate(Date sentDate, boolean hideTimeZone);

    public abstract Address[] getRecipients(RecipientType type);

    public abstract void setRecipients(RecipientType type, Address[] addresses);

    public void setRecipient(RecipientType type, Address address) {
        setRecipients(type, new Address[] {
                          address
                      });
    }

    public abstract Address[] getFrom();

    public abstract void setFrom(Address from);

    public abstract Address[] getSender();

    public abstract void setSender(Address sender);

    public abstract Address[] getReplyTo();

    public abstract void setReplyTo(Address[] from);

    public abstract String getMessageId();

    public abstract void setInReplyTo(String inReplyTo);

    public abstract String[] getReferences();

    public abstract void setReferences(String references);

    @Override
    public abstract Body getBody();

    @Override
    public abstract void addHeader(String name, String value);

    @Override
    public abstract void addRawHeader(String name, String raw);

    @Override
    public abstract void setHeader(String name, String value);

    @NonNull
    @Override
    public abstract String[] getHeader(String name);

    public abstract Set<String> getHeaderNames();

    @Override
    public abstract void removeHeader(String name);

    @Override
    public abstract void setBody(Body body);

    public abstract boolean hasAttachments();

    public abstract long getSize();

    public void delete(String trashFolderName) throws MessagingException {}

    /*
     * TODO Refactor Flags at some point to be able to store user defined flags.
     */
    public Set<Flag> getFlags() {
        return Collections.unmodifiableSet(mFlags);
    }

    /**
     * @param flag
     *            Flag to set. Never <code>null</code>.
     * @param set
     *            If <code>true</code>, the flag is added. If <code>false</code>
     *            , the flag is removed.
     * @throws MessagingException
     */
    public void setFlag(Flag flag, boolean set) throws MessagingException {
        if (set) {
            mFlags.add(flag);
        } else {
            mFlags.remove(flag);
        }
    }

    /**
     * This method calls setFlag(Flag, boolean)
     * @param flags
     * @param set
     */
    public void setFlags(final Set<Flag> flags, boolean set) throws MessagingException {
        for (Flag flag : flags) {
            setFlag(flag, set);
        }
    }

    public boolean isSet(Flag flag) {
        return mFlags.contains(flag);
    }


    public void destroy() throws MessagingException {}

    @Override
    public abstract void setEncoding(String encoding) throws MessagingException;

    public abstract void setCharset(String charset) throws MessagingException;

    public long calculateSize() {
        try {

            CountingOutputStream out = new CountingOutputStream();
            EOLConvertingOutputStream eolOut = new EOLConvertingOutputStream(out);
            writeTo(eolOut);
            eolOut.flush();
            return out.getCount();
        } catch (IOException e) {
            Timber.e(e, "Failed to calculate a message size");
        } catch (MessagingException e) {
            Timber.e(e, "Failed to calculate a message size");
        }
        return 0;
    }

    /**
     * Copy the contents of this object into another {@code Message} object.
     *
     * @param destination The {@code Message} object to receive the contents of this instance.
     */
    protected void copy(Message destination) {
        destination.mUid = mUid;
        destination.mInternalDate = mInternalDate;
        destination.mFolder = mFolder;

        // mFlags contents can change during the object lifetime, so copy the Set
        destination.mFlags = EnumSet.copyOf(mFlags);
    }

    /**
     * Creates a new {@code Message} object with the same content as this object.
     *
     * <p>
     * <strong>Note:</strong>
     * This method was introduced as a hack to prevent {@code ConcurrentModificationException}s. It
     * shouldn't be used unless absolutely necessary. See the comment in
     * {@link com.fsck.k9.activity.MessageView.Listener#loadMessageForViewHeadersAvailable(com.fsck.k9.Account, String, String, Message)}
     * for more information.
     * </p>
     */
    @Override
    public abstract Message clone();

}
