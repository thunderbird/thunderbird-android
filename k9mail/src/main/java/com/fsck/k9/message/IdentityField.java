package com.fsck.k9.message;


// FYI, there's nothing in the code that requires these variables to one letter. They're one
// letter simply to save space.  This name sucks.  It's too similar to Account.Identity.
public enum IdentityField {
    LENGTH("l"),
    OFFSET("o"),
    FOOTER_OFFSET("fo"),
    PLAIN_LENGTH("pl"),
    PLAIN_OFFSET("po"),
    MESSAGE_FORMAT("f"),
    MESSAGE_READ_RECEIPT("r"),
    SIGNATURE("s"),
    NAME("n"),
    EMAIL("e"),
    // TODO - store a reference to the message being replied so we can mark it at the time of send.
    ORIGINAL_MESSAGE("m"),
    CURSOR_POSITION("p"),   // Where in the message your cursor was when you saved.
    QUOTED_TEXT_MODE("q"),
    QUOTE_STYLE("qs");

    private final String value;

    IdentityField(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    /**
     * Get the list of IdentityFields that should be integer values.
     *
     * <p>
     * These values are sanity checked for integer-ness during decoding.
     * </p>
     *
     * @return The list of integer {@link IdentityField}s.
     */
    public static IdentityField[] getIntegerFields() {
        return new IdentityField[] { LENGTH, OFFSET, FOOTER_OFFSET, PLAIN_LENGTH, PLAIN_OFFSET };
    }

    // Version identifier for "new style" identity. ! is an impossible value in base64 encoding, so we
    // use that to determine which version we're in.
    static final String IDENTITY_VERSION_1 = "!";
}
