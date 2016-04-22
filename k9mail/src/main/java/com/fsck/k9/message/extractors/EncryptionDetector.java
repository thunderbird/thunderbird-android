package com.fsck.k9.message.extractors;


import java.util.regex.Pattern;

import android.support.annotation.NonNull;

import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MessageExtractor;

import static com.fsck.k9.mail.internet.MimeUtility.isSameMimeType;


class EncryptionDetector {
    private static final Pattern PGP_MESSAGE_PATTERN = Pattern.compile(
            "(^|\\r\\n)-----BEGIN PGP MESSAGE-----\\r\\n.*?\\r\\n-----END PGP MESSAGE-----(\\r\\n|$)", Pattern.DOTALL);


    private final TextPartFinder textPartFinder;


    EncryptionDetector(TextPartFinder textPartFinder) {
        this.textPartFinder = textPartFinder;
    }

    public boolean isEncrypted(@NonNull Message message) {
        return isPgpMimeOrSMimeEncrypted(message) || containsInlinePgpEncryptedText(message);
    }

    private boolean isPgpMimeOrSMimeEncrypted(Message message) {
        return containsPartWithMimeType(message, "multipart/encrypted", "application/pkcs7-mime");
    }

    private boolean containsInlinePgpEncryptedText(Message message) {
        Part textPart = textPartFinder.findFirstTextPart(message);
        if (!isUsableTextPart(textPart)) {
            return false;
        }

        String text = MessageExtractor.getTextFromPart(textPart);
        if (text == null) {
            return false;
        }

        return PGP_MESSAGE_PATTERN.matcher(text).find();
    }

    private boolean isUsableTextPart(Part textPart) {
        return textPart != null && textPart.getBody() != null;
    }

    private boolean containsPartWithMimeType(Part part, String... wantedMimeTypes) {
        String mimeType = part.getMimeType();
        if (isMimeTypeAnyOf(mimeType, wantedMimeTypes)) {
            return true;
        }

        Body body = part.getBody();
        if (body instanceof Multipart) {
            Multipart multipart = (Multipart) body;
            for (BodyPart bodyPart : multipart.getBodyParts()) {
                if (containsPartWithMimeType(bodyPart, wantedMimeTypes)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isMimeTypeAnyOf(String mimeType, String... wantedMimeTypes) {
        for (String wantedMimeType : wantedMimeTypes) {
            if (isSameMimeType(mimeType, wantedMimeType)) {
                return true;
            }
        }

        return false;
    }
}
