package com.fsck.k9.mail;

/**
 * The currently available connection security types.
 *
 * <p>
 * Right now this enum is only used by {@link StoreSettings} and converted to store-specific
 * constants in the different Store implementations. In the future we probably want to change this
 * and use {@code ConnectionSecurity} exclusively.
 * </p>
 */
public enum ConnectionSecurity {
    NONE,
    STARTTLS_OPTIONAL,
    STARTTLS_REQUIRED,
    SSL_TLS_OPTIONAL,
    SSL_TLS_REQUIRED
}
