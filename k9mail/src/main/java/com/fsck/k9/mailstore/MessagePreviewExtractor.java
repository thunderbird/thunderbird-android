package com.fsck.k9.mailstore;


import java.util.List;

import android.content.Context;
import android.text.TextUtils;

import com.fsck.k9.R;
import com.fsck.k9.helper.HtmlConverter;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MessageExtractor;
import com.fsck.k9.mail.internet.Viewable;
import com.fsck.k9.mail.internet.Viewable.Alternative;
import com.fsck.k9.mail.internet.Viewable.Html;
import com.fsck.k9.mail.internet.Viewable.MessageHeader;
import com.fsck.k9.mail.internet.Viewable.Textual;


class MessagePreviewExtractor {
    private static final int MAX_PREVIEW_LENGTH = 512;
    private static final int MAX_CHARACTERS_CHECKED_FOR_PREVIEW = 8192;

    public static String extractPreview(Context context, List<Viewable> viewables) {
        StringBuilder text = new StringBuilder();
        boolean divider = false;

        for (Viewable viewable : viewables) {
            if (viewable instanceof Textual) {
                appendText(text, viewable, divider);
                divider = true;
            } else if (viewable instanceof MessageHeader) {
                appendMessagePreview(context, text, (MessageHeader) viewable, divider);
                divider = false;
            } else if (viewable instanceof Alternative) {
                appendAlternative(text, (Alternative) viewable, divider);
                divider = true;
            }

            if (hasMaxPreviewLengthBeenReached(text)) {
                break;
            }
        }

        if (hasMaxPreviewLengthBeenReached(text)) {
            text.setLength(MAX_PREVIEW_LENGTH - 1);
            text.append('…');
        }

        return text.toString();
    }

    private static void appendText(StringBuilder text, Viewable viewable, boolean prependDivider) {
        if (viewable instanceof Textual) {
            appendTextual(text, (Textual) viewable, prependDivider);
        } else if (viewable instanceof Alternative) {
            appendAlternative(text, (Alternative) viewable, prependDivider);
        } else {
            throw new IllegalArgumentException("Unknown Viewable");
        }
    }

    private static void appendTextual(StringBuilder text, Textual textual, boolean prependDivider) {
        Part part = textual.getPart();

        if (prependDivider) {
            appendDivider(text);
        }

        String textFromPart = MessageExtractor.getTextFromPart(part);
        if (textFromPart == null) {
            textFromPart = "";
        } else if (textual instanceof Html) {
            textFromPart = HtmlConverter.htmlToText(textFromPart);
        }

        text.append(stripTextForPreview(textFromPart));
    }

    private static void appendAlternative(StringBuilder text, Alternative alternative, boolean prependDivider) {
        List<Viewable> textAlternative = alternative.getText().isEmpty() ?
                alternative.getHtml() : alternative.getText();

        boolean divider = prependDivider;
        for (Viewable textViewable : textAlternative) {
            appendText(text, textViewable, divider);
            divider = true;

            if (hasMaxPreviewLengthBeenReached(text)) {
                break;
            }
        }
    }

    private static void appendMessagePreview(Context context, StringBuilder text, MessageHeader messageHeader,
            boolean divider) {
        if (divider) {
            appendDivider(text);
        }

        String subject = messageHeader.getMessage().getSubject();
        if (TextUtils.isEmpty(subject)) {
            text.append(context.getString(R.string.preview_untitled_inner_message));
        } else {
            text.append(context.getString(R.string.preview_inner_message, subject));
        }
    }

    private static void appendDivider(StringBuilder text) {
        text.append(" / ");
    }

    private static String stripTextForPreview(String text) {
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

        return (text.length() <= MAX_PREVIEW_LENGTH) ? text : text.substring(0, MAX_PREVIEW_LENGTH);
    }

    private static boolean hasMaxPreviewLengthBeenReached(StringBuilder text) {
        return text.length() >= MAX_PREVIEW_LENGTH;
    }
}
