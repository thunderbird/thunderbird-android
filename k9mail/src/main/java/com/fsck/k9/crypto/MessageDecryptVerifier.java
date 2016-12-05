package com.fsck.k9.crypto;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MessageExtractor;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mailstore.CryptoResultAnnotation;
import com.fsck.k9.ui.crypto.MessageCryptoAnnotations;

import static com.fsck.k9.mail.internet.MimeUtility.isSameMimeType;


public class MessageDecryptVerifier {
    private static final String MULTIPART_ENCRYPTED = "multipart/encrypted";
    private static final String MULTIPART_SIGNED = "multipart/signed";
    private static final String PROTOCOL_PARAMETER = "protocol";
    private static final String APPLICATION_PGP_ENCRYPTED = "application/pgp-encrypted";
    private static final String APPLICATION_PGP_SIGNATURE = "application/pgp-signature";
    private static final String TEXT_PLAIN = "text/plain";
    // APPLICATION/PGP is a special case which occurs from mutt. see http://www.mutt.org/doc/PGP-Notes.txt
    private static final String APPLICATION_PGP = "application/pgp";

    public static final String PGP_INLINE_START_MARKER = "-----BEGIN PGP MESSAGE-----";
    public static final String PGP_INLINE_SIGNED_START_MARKER = "-----BEGIN PGP SIGNED MESSAGE-----";
    public static final int TEXT_LENGTH_FOR_INLINE_CHECK = 36;


    public static Part findPrimaryEncryptedOrSignedPart(Part part, List<Part> outputExtraParts) {
        if (isPartEncryptedOrSigned(part)) {
            return part;
        }

        Body body = part.getBody();
        if (part.isMimeType("multipart/mixed") && body instanceof Multipart) {
            Multipart multipart = (Multipart) body;
            BodyPart firstBodyPart = multipart.getBodyPart(0);
            if (isPartEncryptedOrSigned(firstBodyPart)) {
                if (outputExtraParts != null) {
                    for (int i = 1; i < multipart.getCount(); i++) {
                        outputExtraParts.add(multipart.getBodyPart(i));
                    }
                }
                return firstBodyPart;
            }
        }

        return null;
    }

    public static List<Part> findEncryptedParts(Part startPart) {
        List<Part> encryptedParts = new ArrayList<>();
        Stack<Part> partsToCheck = new Stack<>();
        partsToCheck.push(startPart);

        while (!partsToCheck.isEmpty()) {
            Part part = partsToCheck.pop();
            Body body = part.getBody();

            if (isPartMultipartEncrypted(part)) {
                encryptedParts.add(part);
                continue;
            }

            if (body instanceof Multipart) {
                Multipart multipart = (Multipart) body;
                for (int i = multipart.getCount() - 1; i >= 0; i--) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    partsToCheck.push(bodyPart);
                }
            }
        }

        return encryptedParts;
    }

    public static List<Part> findSignedParts(Part startPart, MessageCryptoAnnotations messageCryptoAnnotations) {
        List<Part> signedParts = new ArrayList<>();
        Stack<Part> partsToCheck = new Stack<>();
        partsToCheck.push(startPart);

        while (!partsToCheck.isEmpty()) {
            Part part = partsToCheck.pop();
            if (messageCryptoAnnotations.has(part)) {
                CryptoResultAnnotation resultAnnotation = messageCryptoAnnotations.get(part);
                MimeBodyPart replacementData = resultAnnotation.getReplacementData();
                if (replacementData != null) {
                    part = replacementData;
                }
            }
            Body body = part.getBody();

            if (isPartMultipartSigned(part)) {
                signedParts.add(part);
                continue;
            }

            if (body instanceof Multipart) {
                Multipart multipart = (Multipart) body;
                for (int i = multipart.getCount() - 1; i >= 0; i--) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    partsToCheck.push(bodyPart);
                }
            }
        }

        return signedParts;
    }

    public static List<Part> findPgpInlineParts(Part startPart) {
        List<Part> inlineParts = new ArrayList<>();
        Stack<Part> partsToCheck = new Stack<>();
        partsToCheck.push(startPart);

        while (!partsToCheck.isEmpty()) {
            Part part = partsToCheck.pop();
            Body body = part.getBody();

            if (isPartPgpInlineEncryptedOrSigned(part)) {
                inlineParts.add(part);
                continue;
            }

            if (body instanceof Multipart) {
                Multipart multipart = (Multipart) body;
                for (int i = multipart.getCount() - 1; i >= 0; i--) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    partsToCheck.push(bodyPart);
                }
            }
        }

        return inlineParts;
    }

    public static byte[] getSignatureData(Part part) throws IOException, MessagingException {
        if (isPartMultipartSigned(part)) {
            Body body = part.getBody();
            if (body instanceof Multipart) {
                Multipart multi = (Multipart) body;
                BodyPart signatureBody = multi.getBodyPart(1);
                if (isSameMimeType(signatureBody.getMimeType(), APPLICATION_PGP_SIGNATURE)) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    signatureBody.getBody().writeTo(bos);
                    return bos.toByteArray();
                }
            }
        }

        return null;
    }

    private static boolean isPartEncryptedOrSigned(Part part) {
        return isPartMultipartEncrypted(part) || isPartMultipartSigned(part) || isPartPgpInlineEncryptedOrSigned(part);
    }

    private static boolean isPartMultipartSigned(Part part) {
        return isSameMimeType(part.getMimeType(), MULTIPART_SIGNED);
    }

    private static boolean isPartMultipartEncrypted(Part part) {
        return isSameMimeType(part.getMimeType(), MULTIPART_ENCRYPTED);
    }

    // TODO also guess by mime-type of contained part?
    public static boolean isPgpMimeEncryptedOrSignedPart(Part part) {
        String contentType = part.getContentType();
        String protocolParameter = MimeUtility.getHeaderParameter(contentType, PROTOCOL_PARAMETER);

        boolean isPgpEncrypted = isSameMimeType(part.getMimeType(), MULTIPART_ENCRYPTED) &&
                APPLICATION_PGP_ENCRYPTED.equalsIgnoreCase(protocolParameter);
        boolean isPgpSigned = isSameMimeType(part.getMimeType(), MULTIPART_SIGNED) &&
                APPLICATION_PGP_SIGNATURE.equalsIgnoreCase(protocolParameter);

        return isPgpEncrypted || isPgpSigned;
    }

    @VisibleForTesting
    static boolean isPartPgpInlineEncryptedOrSigned(Part part) {
        if (!part.isMimeType(TEXT_PLAIN) && !part.isMimeType(APPLICATION_PGP)) {
            return false;
        }
        String text = MessageExtractor.getTextFromPart(part, TEXT_LENGTH_FOR_INLINE_CHECK);
        if (TextUtils.isEmpty(text)) {
            return false;
        }
        text = text.trim();
        return text.startsWith(PGP_INLINE_START_MARKER) || text.startsWith(PGP_INLINE_SIGNED_START_MARKER);
    }

    public static boolean isPartPgpInlineEncrypted(@Nullable Part part) {
        if (part == null) {
            return false;
        }
        if (!part.isMimeType(TEXT_PLAIN) && !part.isMimeType(APPLICATION_PGP)) {
            return false;
        }
        String text = MessageExtractor.getTextFromPart(part, TEXT_LENGTH_FOR_INLINE_CHECK);
        if (TextUtils.isEmpty(text)) {
            return false;
        }
        text = text.trim();
        return text.startsWith(PGP_INLINE_START_MARKER);
    }

}
