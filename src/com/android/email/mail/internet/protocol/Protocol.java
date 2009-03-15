package com.android.email.mail.internet.protocol;

/**
 * This class represents the base object for protocols.
 * The only common interface required is the ability to determine if a
 * command (method) is supported.
 *
 * @version .1
 * @author Matthew Brace
 */
public abstract class Protocol {
    abstract boolean isCommandSupported(String command);
}
