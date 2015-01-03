package com.fsck.k9.mail;


/**
 * A CompositeBody is a {@link Body} extension that can contain subparts that
 * may require recursing through or iterating over when converting the
 * CompositeBody from 8bit to 7bit encoding. The {@link Part} to which a
 * CompositeBody belongs is only permitted to use 8bit or 7bit content transfer
 * encoding for the CompositeBody.
 *
 */
public interface CompositeBody extends Body {

    /**
     * Called just prior to transmission, once the type of transport is known to
     * be 7bit.
     * <p>
     * All subparts that are 8bit and of type {@link CompositeBody} will be
     * converted to 7bit and recursed. All supbparts that are 8bit but not
     * of type CompositeBody will be converted to quoted-printable. Bodies with
     * encodings other than 8bit remain unchanged.
     *
     * @throws MessagingException
     *
     */
    public abstract void setUsing7bitTransport() throws MessagingException;

}
