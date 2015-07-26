
package com.fsck.k9.crypto;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MessageExtractor;
import com.fsck.k9.mail.internet.MimeUtility;
import org.openintents.openpgp.util.OpenPgpUtils;


public class CryptoHelper {

    public static final Pattern PGP_MESSAGE =
            Pattern.compile(".*?(-----BEGIN PGP MESSAGE-----.*?-----END PGP MESSAGE-----).*",
                    Pattern.DOTALL);

    public static final Pattern PGP_SIGNED_MESSAGE =
            Pattern.compile(
                    ".*?(-----BEGIN PGP SIGNED MESSAGE-----.*?-----BEGIN PGP SIGNATURE-----.*?-----END PGP SIGNATURE-----).*",
                    Pattern.DOTALL);

    public static final String APPLICATION_PKCS7_MIME = "application/pkcs7-mime";
    public static final String MULTIPART_ENCRYPTED = "multipart/encrypted";
    public static final String MULTIPART_SIGNED = "multipart/signed";
    public static final String PROTOCOL_PARAMETER = "protocol";
    public static final String APPLICATION_PGP_ENCRYPTED = "application/pgp-encrypted";
    public static final String APPLICATION_PGP_SIGNATURE = "application/pgp-signature";
    public static final String TEXT_PLAIN = "text/plain";

    public CryptoHelper() {
        super();
    }

    /**
     * @param message
     * @return
     */
    public static boolean isSMimeEncrypted(Message message) throws MessagingException {
        return contentHeaderContains(message, APPLICATION_PKCS7_MIME);
    }

    /**
     * @param message
     * @return
     */
    public static boolean isPgpMimeEncrypted(Message message) throws MessagingException {
        return contentHeaderContains(message, MULTIPART_ENCRYPTED);
    }

    /**
     * @param message
     * @return
     */
    public static boolean isPgpInlineEncrypted(Message message) throws MessagingException {
        Part part = MimeUtility.findFirstPartByMimeType(message, "text/plain");

        if (part == null) {
            return false;
        }

        String data = MessageExtractor.getTextFromPart(part);

        if (data == null) {
            return false;
        }

        switch (OpenPgpUtils.parseMessage(data)) {
            case OpenPgpUtils.PARSE_RESULT_MESSAGE:
            case OpenPgpUtils.PARSE_RESULT_SIGNED_MESSAGE:
                return true;
            default:
                return false;
        }
    }

    private static boolean contentHeaderContains(Message message, String desiredHeader) throws MessagingException {
        String[] contentHeaders = message.getHeader("Content-Type");

        if (contentHeaders == null) {
            return false;
        }

        for (String contentHeader : contentHeaders) {
            if (contentHeader.equalsIgnoreCase(desiredHeader)) {
                return true;
            }
        }

        return false;
    }
}
