package com.fsck.k9.message.extractors;


import androidx.annotation.NonNull;

import app.k9mail.legacy.message.extractors.PreviewResult;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;

import net.thunderbird.core.logging.legacy.Log;


public class MessagePreviewCreator {
    private final TextPartFinder textPartFinder;
    private final HTMLPartFinder htmlPartFinder;
    private final PreviewTextExtractor previewTextExtractor;


    MessagePreviewCreator(TextPartFinder textPartFinder, HTMLPartFinder htmlPartFinder, PreviewTextExtractor previewTextExtractor) {
        this.textPartFinder = textPartFinder;
        this.htmlPartFinder = htmlPartFinder;
        this.previewTextExtractor = previewTextExtractor;
    }

    public static MessagePreviewCreator newInstance() {
        TextPartFinder textPartFinder = new TextPartFinder();
        PreviewTextExtractor previewTextExtractor = new PreviewTextExtractor();
        HTMLPartFinder htmlPartFinder = new HTMLPartFinder();
        return new MessagePreviewCreator(textPartFinder, htmlPartFinder, previewTextExtractor);
    }

    public PreviewResult createPreview(@NonNull Message message) {
        Part textPart = htmlPartFinder.findFirstHTMLPart(message);
        if (textPart == null || hasEmptyBody(textPart)) {
            textPart = textPartFinder.findFirstTextPart(message);
            if (textPart == null || hasEmptyBody(textPart)) {
                return PreviewResult.none();
            }
        }

        try {
            String previewText = previewTextExtractor.extractPreview(textPart);
            return PreviewResult.text(previewText);
        } catch (PreviewExtractionException e) {
            Log.w(e, "Failed to extract preview text");
            return PreviewResult.error();
        } catch (Exception e) {
            Log.e(e, "Unexpected error while trying to extract preview text");
            return PreviewResult.error();
        }
    }

    private boolean hasEmptyBody(Part textPart) {
        return textPart.getBody() == null;
    }
}
