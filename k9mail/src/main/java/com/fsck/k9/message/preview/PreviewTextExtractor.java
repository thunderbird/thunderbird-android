package com.fsck.k9.message.preview;


import android.support.annotation.NonNull;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.helper.HtmlConverter;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MessageExtractor;

import static com.fsck.k9.mail.internet.MimeUtility.isSameMimeType;


class PreviewTextExtractor {
    private static final int MAX_PREVIEW_LENGTH = 512;
    private static final int MAX_CHARACTERS_CHECKED_FOR_PREVIEW = 8192;


    public String extractPreview(@NonNull Part textPart) {
        String text = MessageExtractor.getTextFromPart(textPart);
        String plainText = convertFromHtmlIfNecessary(textPart, text);
        return stripTextForPreview(plainText);
    }

    private String convertFromHtmlIfNecessary(Part textPart, String text) {
        String mimeType = textPart.getMimeType();
        if (!isSameMimeType(mimeType, "text/html")) {
            return text;
        }

        return HtmlConverter.htmlToText(text);
    }

    private String stripTextForPreview(String text) {
        if (text == null) {
            return "";
        }

        // Only look at the first 8k of a message when calculating
        // the preview.  This should avoid unnecessary
        // memory usage on large messages
        if (text.length() > MAX_CHARACTERS_CHECKED_FOR_PREVIEW) {
            text = text.substring(0, MAX_CHARACTERS_CHECKED_FOR_PREVIEW);
        }

        // Remove (correctly delimited by '-- \n') signatures
        text = text.replaceAll("(?ms)^-- [\\r\\n]+.*", "");
        // try to remove lines of dashes in the preview
        text = text.replaceAll("(?m)^----.*?$", "");
        // remove quoted text from the preview
        text = text.replaceAll("(?m)^[#>].*$", "");
        // Remove a common quote header from the preview
        text = text.replaceAll("(?m)^On .*wrote.?$", "");
        // Remove a more generic quote header from the preview
        text = text.replaceAll("(?m)^.*\\w+:$", "");
        // Remove horizontal rules.
        text = text.replaceAll("\\s*([-=_]{30,}+)\\s*", " ");

        // URLs in the preview should just be shown as "..." - They're not
        // clickable and they usually overwhelm the preview
        text = text.replaceAll("https?://\\S+", "...");
        // Don't show newlines in the preview
        text = text.replaceAll("(\\r|\\n)+", " ");
        // Collapse whitespace in the preview
        text = text.replaceAll("\\s+", " ");
        // Remove any whitespace at the beginning and end of the string.
        text = text.trim();

        return (text.length() > MAX_PREVIEW_LENGTH) ? text.substring(0, MAX_PREVIEW_LENGTH - 1) + "…" : text;
    }
}
