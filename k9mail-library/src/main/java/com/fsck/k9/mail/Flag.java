package com.fsck.k9.mail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

import timber.log.Timber;


public class Flag {

    private static final HashMap<String, Flag> mapCodeToPredefinedFlag =
        new HashMap<String, Flag>();

    private static final HashMap<String, Flag> mapExternalCodeToSystemFlag =
        new HashMap<String, Flag>();


    /*
     * IMAP system flags
     */
    public static final Flag DELETED = createSystemFlag("DELETED", "\\Deleted");
    public static final Flag SEEN = createSystemFlag("SEEN", "\\Seen");
    public static final Flag ANSWERED = createSystemFlag("ANSWERED", "\\Answered");
    public static final Flag FLAGGED = createSystemFlag("FLAGGED", "\\Flagged");

    public static final Flag DRAFT = createSystemFlag("DRAFT", "\\Draft");
    public static final Flag RECENT = createSystemFlag("RECENT", "\\Recent");
    // NOTE: Strictly speaking '$forwarded' is /not/ an IMAP system flag.
    public static final Flag FORWARDED = createSystemFlag("FORWARDED", "$forwarded");

    /*
     * The following flags are for internal library use only.
     */
    /**
     * Delete and remove from the LocalStore immediately.
     */
    public static final Flag X_DESTROYED = createInternalFlag("X_DESTROYED");

    /**
     * Sending of an unsent message failed. It will be retried. Used to show status.
     */
    public static final Flag X_SEND_FAILED = createInternalFlag("X_SEND_FAILED");

    /**
     * Sending of an unsent message is in progress.
     */
    public static final Flag X_SEND_IN_PROGRESS = createInternalFlag("X_SEND_IN_PROGRESS");

    /**
     * Indicates that a message is fully downloaded from the server and can be viewed normally.
     * This does not include attachments, which are never downloaded fully.
     */
    public static final Flag X_DOWNLOADED_FULL = createInternalFlag("X_DOWNLOADED_FULL");

    /**
     * Indicates that a message is partially downloaded from the server and can be viewed but
     * more content is available on the server.
     * This does not include attachments, which are never downloaded fully.
     */
    public static final Flag X_DOWNLOADED_PARTIAL = createInternalFlag("X_DOWNLOADED_PARTIAL");

    /**
     * Indicates that the copy of a message to the Sent folder has started.
     */
    public static final Flag X_REMOTE_COPY_STARTED = createInternalFlag("X_REMOTE_COPY_STARTED");

    /**
     * Messages with this flag have been migrated from database version 50 or earlier.
     * This earlier database format did not preserve the original mime structure of a
     * mail, which means messages migrated to the newer database structure may be
     * incomplete or broken.
     * TODO Messages with this flag should be redownloaded, if possible.
     */
    public static final Flag X_MIGRATED_FROM_V50 = createInternalFlag("X_MIGRATED_FROM_V50");

    /**
     * This flag is used for drafts where the message should be sent as PGP/INLINE.
     */
    public static final Flag X_DRAFT_OPENPGP_INLINE = createInternalFlag("X_DRAFT_OPENPGP_INLINE");

    public static final Flag X_GOT_ALL_HEADERS = createInternalFlag("X_GOT_ALL_HEADERS");


    // Internal unique code for use in k9 database, e.g. "DELETED"
    private final String code;

    // Code for use in external storage, e.g. "\\Deleted"
    // For IMAP: A 'system flag' or a 'keyword' in terms of RFC 3501, 2.3.2.
    private final String externalCode;


    protected Flag(String code, String externalCode) {
        // The caller has verified that code does not yet exist and that
        // externalCode is either empty or also does not yet exist.
        this.code = code;
        this.externalCode = externalCode;
    }


    private static Flag createInternalFlag(String code) {
        Flag flag = new Flag(code, /*externalCode*/ "");
        mapCodeToPredefinedFlag.put(code, flag);
        return flag;
    }

    private static Flag createSystemFlag(String code, String externalCode) {
        Flag flag = new Flag(code, externalCode);
        mapCodeToPredefinedFlag.put(code, flag);
        mapExternalCodeToSystemFlag.put(externalCode, flag);
        return flag;
    }


    // Get flag by code; create a new keyword, if needed.
    public static Flag valueOf(String code) throws IllegalArgumentException {
        if (mapCodeToPredefinedFlag.containsKey(code)) {
            return mapCodeToPredefinedFlag.get(code);
        }
        return Keyword.valueOf(code);
    }

    // Get server flag by external code; create a new keyword, if needed.
    public static Flag getFlagByExternalCode(String externalCode)
       throws IllegalArgumentException
    {
        if (mapExternalCodeToSystemFlag.containsKey(externalCode)) {
            return mapExternalCodeToSystemFlag.get(externalCode);
        }
        return Keyword.getKeywordByExternalCode(externalCode);
    }

    protected static boolean isExternalCodeOfSystemFlag(String externalCode) {
        return mapExternalCodeToSystemFlag.containsKey(externalCode);
    }

    public static List<Flag> parseCodeList(String codeList) {
        LinkedHashSet<Flag> flags = new LinkedHashSet<Flag>();
        if (codeList != null && codeList.length() > 0) {
            for (String code : codeList.split(",")) {
                try {
                    flags.add(valueOf(code));
                }
                catch (Exception e) {
                    if (!"X_BAD_FLAG".equals(code)) {
                        Timber.w("Unable to parse flag %s", code);
                    }
                }
            }
        }
        return new ArrayList<Flag>(flags);
    }


    public String getCode() {
        return code;
    }

    public String getExternalCode() {
        return externalCode;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Flag) {
            final Flag flag = (Flag) o;
            return (flag == this || (
                flag.code.equals(this.code) &&
                flag.externalCode.equals(this.externalCode)));
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }

    @Override
    public String toString() {
        return code;
    }

}
