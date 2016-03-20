package com.fsck.k9.message.preview;


import android.support.annotation.NonNull;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;


public class MessagePreviewCreator {
    private final TextPartFinder textPartFinder;
    private final PreviewTextExtractor previewTextExtractor;
    private final EncryptionDetector encryptionDetector;


    MessagePreviewCreator(TextPartFinder textPartFinder, PreviewTextExtractor previewTextExtractor,
            EncryptionDetector encryptionDetector) {
        this.textPartFinder = textPartFinder;
        this.previewTextExtractor = previewTextExtractor;
        this.encryptionDetector = encryptionDetector;
    }

    public static MessagePreviewCreator newInstance() {
        TextPartFinder textPartFinder = new TextPartFinder();
        PreviewTextExtractor previewTextExtractor = new PreviewTextExtractor();
        EncryptionDetector encryptionDetector = new EncryptionDetector(textPartFinder);
        return new MessagePreviewCreator(textPartFinder, previewTextExtractor, encryptionDetector);
    }

    public PreviewResult createPreview(@NonNull Message message) {
        if (encryptionDetector.isEncrypted(message)) {
            return PreviewResult.encrypted();
        }

        return extractText(message);
    }

    private PreviewResult extractText(Message message) {
        Part textPart = textPartFinder.findFirstTextPart(message);
        if (textPart == null || hasEmptyBody(textPart)) {
            return PreviewResult.none();
        }
        String previewText = previewTextExtractor.extractPreview(textPart);
        if (previewText == null) {
            return PreviewResult.failed();
        }
        return PreviewResult.text(previewText);
    }

    private boolean hasEmptyBody(Part textPart) {
        return textPart.getBody() == null;
    }
}
