package com.fsck.k9.message.quote;


import android.content.res.Resources;

import com.fsck.k9.Account.QuoteStyle;
import com.fsck.k9.CoreResourceProvider;
import com.fsck.k9.DI;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;

import static com.fsck.k9.message.quote.QuoteHelper.QUOTE_BUFFER_LENGTH;


public class TextQuoteCreator {
    private static final int REPLY_WRAP_LINE_WIDTH = 72;

    /**
     * Add quoting markup to a text message.
     * @param originalMessage Metadata for message being quoted.
     * @param messageBody Text of the message to be quoted.
     * @param quoteStyle Style of quoting.
     * @return Quoted text.
     */
    public static String quoteOriginalTextMessage(Resources resources, Message originalMessage, String messageBody, QuoteStyle quoteStyle, String prefix) {
        CoreResourceProvider resourceProvider = DI.get(CoreResourceProvider.class);
        String body = messageBody == null ? "" : messageBody;
        String sentDate = QuoteHelper.getSentDateText(resources, originalMessage);
        if (quoteStyle == QuoteStyle.PREFIX) {
            String sender = Address.toString(originalMessage.getFrom());
            StringBuilder quotedText = new StringBuilder(body.length() + QUOTE_BUFFER_LENGTH);
            if (sentDate.length() != 0) {
                String replyHeader = resourceProvider.replyHeader(sender, sentDate);
                quotedText.append(replyHeader);
            } else {
                String replyHeader = resourceProvider.replyHeader(sender);
                quotedText.append(replyHeader);
            }

            final String wrappedText = Utility.wrap(body, REPLY_WRAP_LINE_WIDTH - prefix.length());

            // "$" and "\" in the quote prefix have to be escaped for
            // the replaceAll() invocation.
            final String escapedPrefix = prefix.replaceAll("(\\\\|\\$)", "\\\\$1");
            quotedText.append(wrappedText.replaceAll("(?m)^", escapedPrefix));

            return quotedText.toString();
        } else if (quoteStyle == QuoteStyle.HEADER) {
            StringBuilder quotedText = new StringBuilder(body.length() + QUOTE_BUFFER_LENGTH);
            quotedText.append("\r\n");
            quotedText.append(resourceProvider.messageHeaderSeparator()).append("\r\n");
            if (originalMessage.getFrom() != null && Address.toString(originalMessage.getFrom()).length() != 0) {
                quotedText.append(resourceProvider.messageHeaderFrom()).append(" ").append(Address.toString(originalMessage.getFrom())).append("\r\n");
            }
            if (sentDate.length() != 0) {
                quotedText.append(resourceProvider.messageHeaderDate()).append(" ").append(sentDate).append("\r\n");
            }
            if (originalMessage.getRecipients(RecipientType.TO) != null && originalMessage.getRecipients(RecipientType.TO).length != 0) {
                quotedText.append(resourceProvider.messageHeaderTo()).append(" ").append(Address.toString(originalMessage.getRecipients(RecipientType.TO))).append("\r\n");
            }
            if (originalMessage.getRecipients(RecipientType.CC) != null && originalMessage.getRecipients(RecipientType.CC).length != 0) {
                quotedText.append(resourceProvider.messageHeaderCc()).append(" ").append(Address.toString(originalMessage.getRecipients(RecipientType.CC))).append("\r\n");
            }
            if (originalMessage.getSubject() != null) {
                quotedText.append(resourceProvider.messageHeaderSubject()).append(" ").append(originalMessage.getSubject()).append("\r\n");
            }
            quotedText.append("\r\n");

            quotedText.append(body);

            return quotedText.toString();
        } else {
            // Shouldn't ever happen.
            return body;
        }
    }
}
