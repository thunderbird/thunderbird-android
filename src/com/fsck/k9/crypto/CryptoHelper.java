
package com.fsck.k9.crypto;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;

public class CryptoHelper {

    public static Pattern PGP_MESSAGE =
            Pattern.compile(".*?(-----BEGIN PGP MESSAGE-----.*?-----END PGP MESSAGE-----).*",
                    Pattern.DOTALL);

    public static Pattern PGP_SIGNED_MESSAGE =
            Pattern.compile(
                    ".*?(-----BEGIN PGP SIGNED MESSAGE-----.*?-----BEGIN PGP SIGNATURE-----.*?-----END PGP SIGNATURE-----).*",
                    Pattern.DOTALL);

    public CryptoHelper() {
        super();
    }

    /**
     * TODO: use new parseMessage() from PgpUtils to actually parse!
     * @param message
     * @return
     */
    public boolean isEncrypted(Message message) {
        String data = null;
        try {
            Part part = message.findFirstPartByMimeType("text/plain");
            if (part == null) {
                part = message.findFirstPartByMimeType("text/html");
            }
            if (part != null) {
                data = part.getText();
            }
        } catch (MessagingException e) {
            // guess not...
            // TODO: maybe log this?
        }

        if (data == null) {
            return false;
        }

        Matcher matcher = PGP_MESSAGE.matcher(data);
        return matcher.matches();
    }

    public boolean isSigned(Message message) {
        String data = null;
        try {
            Part part = message.findFirstPartByMimeType("text/plain");
            if (part == null) {
                part = message.findFirstPartByMimeType("text/html");
            }
            if (part != null) {
                data = part.getText();
            }
        } catch (MessagingException e) {
            // guess not...
            // TODO: maybe log this?
        }

        if (data == null) {
            return false;
        }

        Matcher matcher = PGP_SIGNED_MESSAGE.matcher(data);
        return matcher.matches();
    }
}
