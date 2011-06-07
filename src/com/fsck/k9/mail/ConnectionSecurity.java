package com.fsck.k9.mail;

/**
 * The currently available connection security types.
 *
 * <p>
 * Right now this enum is only used by {@link ServerSettings} and converted to store- or
 * transport-specific constants in the different {@link Store} and {@link Transport}
 * implementations. In the future we probably want to change this and use
 * {@code ConnectionSecurity} exclusively.
 * </p>
 */
public enum ConnectionSecurity {
    NONE,
    STARTTLS_OPTIONAL,
    STARTTLS_REQUIRED,
    SSL_TLS_OPTIONAL,
    SSL_TLS_REQUIRED
}
