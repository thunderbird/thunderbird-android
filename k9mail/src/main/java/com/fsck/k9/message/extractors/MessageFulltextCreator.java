package com.fsck.k9.message.extractors;


import android.support.annotation.NonNull;

import com.fsck.k9.helper.HtmlConverter;
import com.fsck.k9.mail.FancyPart;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MessageExtractor;


public class MessageFulltextCreator {
    private static final int MAX_CHARACTERS_CHECKED_FOR_FTS = 200*1024;


    private final TextPartFinder textPartFinder;
    private final EncryptionDetector encryptionDetector;


    MessageFulltextCreator(TextPartFinder textPartFinder, EncryptionDetector encryptionDetector) {
        this.textPartFinder = textPartFinder;
        this.encryptionDetector = encryptionDetector;
    }

    public static MessageFulltextCreator newInstance() {
        TextPartFinder textPartFinder = new TextPartFinder();
        EncryptionDetector encryptionDetector = new EncryptionDetector(textPartFinder);
        return new MessageFulltextCreator(textPartFinder, encryptionDetector);
    }

    public String createFulltext(@NonNull Message message) throws MessagingException {
        if (encryptionDetector.isEncrypted(message)) {
            return null;
        }

        return extractText(message);
    }

    private String extractText(Message message) throws MessagingException {
        Part textPart = textPartFinder.findFirstTextPart(message);
        if (textPart == null || hasEmptyBody(textPart)) {
            return null;
        }

        String text = MessageExtractor.getTextFromPart(textPart, MAX_CHARACTERS_CHECKED_FOR_FTS);
        if (!FancyPart.from(textPart).isMimeType("text/html")) {
            return text;
        }

        return HtmlConverter.htmlToText(text);
    }

    private boolean hasEmptyBody(Part textPart) {
        return textPart.getBody() == null;
    }

}
