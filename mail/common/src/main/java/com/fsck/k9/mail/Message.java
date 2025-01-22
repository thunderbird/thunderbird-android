
package com.fsck.k9.mail;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.fsck.k9.logging.Timber;
import com.fsck.k9.mail.filter.CountingOutputStream;
import com.fsck.k9.mail.filter.EOLConvertingOutputStream;
import org.jetbrains.annotations.NotNull;


public abstract class Message implements Part, Body {
    protected static final String DEFAULT_MIME_TYPE = "text/plain";


    public enum RecipientType {
        TO, CC, BCC, X_ORIGINAL_TO, DELIVERED_TO, X_ENVELOPE_TO
    }

    protected String mUid;

    private Set<Flag> mFlags = EnumSet.noneOf(Flag.class);

    private Date mInternalDate;

    public boolean olderThan(Date earliestDate) {
        if (earliestDate == null) {
            return false;
        }
        Date myDate = getSentDate();
        if (myDate == null) {
            myDate = getInternalDate();
        }
        return myDate != null && myDate.before(earliestDate);
    }

    public String getUid() {
        return mUid;
    }

    public void setUid(String uid) {
        this.mUid = uid;
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

    @NotNull
    @Override
    public abstract String[] getHeader(String name);

    public abstract List<Header> getHeaders();

    @Override
    public abstract void removeHeader(String name);

    @Override
    public abstract void setBody(Body body);

    public abstract boolean hasAttachments();

    public abstract long getSize();

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

    public long calculateSize() {
        try (CountingOutputStream out = new CountingOutputStream()) {
            EOLConvertingOutputStream eolOut = new EOLConvertingOutputStream(out);
            writeTo(eolOut);
            eolOut.flush();
            return out.getCount();
        } catch (IOException | MessagingException e) {
            Timber.e(e, "Failed to calculate a message size");
        }
        return 0;
    }

    /*
     * Returns true if any address in this message uses a non-ASCII
     * character in either the localpart or the domain, and false if
     * all addresses use only ASCII.
     */

    public boolean usesAnyUnicodeAddresses() {
        for (final Address a : getFrom())
            if (a.needsUnicode())
                return true;
        for (RecipientType t : RecipientType.values())
            for (final Address r : getRecipients(t))
                if (r.needsUnicode())
                    return true;
        return false;
    }
}
