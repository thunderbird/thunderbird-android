package com.fsck.k9.crypto;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeUtility;

/**
 * A CryptoProvider provides functionalities such as encryption, decryption, digital signatures.
 * It currently also stores the results of such encryption or decryption.
 * TODO: separate the storage from the provider
 */
abstract public class CryptoProvider {
    static final long serialVersionUID = 0x21071234;

    abstract public boolean isAvailable(Context context);
    abstract public boolean isEncrypted(Message message);
    abstract public boolean isSigned(Message message);
    abstract public boolean onActivityResult(Activity activity, int requestCode, int resultCode,
            Intent data, PgpData pgpData);
    abstract public boolean selectSecretKey(Activity activity, PgpData pgpData);
    abstract public boolean selectEncryptionKeys(Activity activity, String emails, PgpData pgpData);
    abstract public boolean encrypt(Activity activity, String data, PgpData pgpData);
    abstract public boolean decrypt(Activity activity, String data, PgpData pgpData);
    abstract public long[] getSecretKeyIdsFromEmail(Context context, String email);
    abstract public String getUserId(Context context, long keyId);
    abstract public String getName();
    abstract public boolean test(Context context);

    public static CryptoProvider createInstance(String name) {
        if (Apg.NAME.equals(name)) {
            return Apg.createInstance();
        }

        return None.createInstance();
    }

    /**
     * searches for useable MimeParts and then calls decrypt with the extracted
     * data. It is save to call this method on already encrypted | verfied
     * emails (it will not perform any action again)
     *
     * @param activity
     *            the activity
     * @param message
     *            the message to be parsed
     * @param pgpData
     *            the current PGP state to check if it has already verified
     * @return true if already verified | decoded ; false if it is not a PGP msg
     *         and otherwise the return value of decrypt
     * @throws MessagingException
     */
    public boolean decrypt(Activity activity, Message message, PgpData pgpData) throws MessagingException {
        if (pgpData.getDecryptedData() != null) {
            return true;
        }
        if (!isSigned(message) && !isEncrypted(message)) {
            return false;
        }
        String data = null;
        Part part;
        part = MimeUtility.findFirstPartByMimeType(message, "text/plain");

        if (part == null) {
            part = MimeUtility.findFirstPartByMimeType(message, "text/html");
        }
        if (part != null) {
            data = MimeUtility.getTextFromPart(part);
        }
        return decrypt(activity, data, pgpData);
    }
}
