package com.fsck.k9.message;


import java.util.Objects;

import android.net.Uri;
import android.net.Uri.Builder;

import app.k9mail.legacy.account.QuoteStyle;
import app.k9mail.legacy.account.Identity;
import com.fsck.k9.K9;
import app.k9mail.legacy.message.controller.MessageReference;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.message.quote.InsertableHtmlContent;
import timber.log.Timber;


public class IdentityHeaderBuilder {
    private static final int MAX_LINE_LENGTH = 72;
    private static final int FIRST_LINE_EXTRA_LENGTH = K9.IDENTITY_HEADER.length() + 2;

    private InsertableHtmlContent quotedHtmlContent;
    private QuoteStyle quoteStyle;
    private SimpleMessageFormat messageFormat;
    private Identity identity;
    private boolean signatureChanged;
    private String signature;
    private boolean identityChanged;
    private QuotedTextMode quotedTextMode;
    private MessageReference messageReference;
    private TextBody body;
    private TextBody bodyPlain;
    private int cursorPosition;

    private Builder uri;


    /**
     * Build the identity header string. This string contains metadata about a draft message to be
     * used upon loading a draft for composition. This should be generated at the time of saving a
     * draft.<br>
     * <br>
     * This is a URL-encoded key/value pair string.  The list of possible values are in {@link IdentityField}.
     *
     * @return Identity string.
     */
    public String build() {
        //FIXME: check arguments

        uri = new Uri.Builder();

        if (body.getComposedMessageLength() != null && body.getComposedMessageOffset() != null) {
            // See if the message body length is already in the TextBody.
            appendValue(IdentityField.LENGTH, body.getComposedMessageLength());
            appendValue(IdentityField.OFFSET, body.getComposedMessageOffset());
        } else {
            // If not, calculate it now.
            appendValue(IdentityField.LENGTH, body.getRawText().length());
            appendValue(IdentityField.OFFSET, 0);
        }

        if (quotedHtmlContent != null) {
            appendValue(IdentityField.FOOTER_OFFSET, quotedHtmlContent.getFooterInsertionPoint());
        }

        if (bodyPlain != null) {
            Integer composedMessageLength = bodyPlain.getComposedMessageLength();
            Integer composedMessageOffset = bodyPlain.getComposedMessageOffset();
            if (composedMessageLength != null && composedMessageOffset != null) {
                // See if the message body length is already in the TextBody.
                appendValue(IdentityField.PLAIN_LENGTH, composedMessageLength);
                appendValue(IdentityField.PLAIN_OFFSET, composedMessageOffset);
            } else {
                // If not, calculate it now.
                appendValue(IdentityField.PLAIN_LENGTH, body.getRawText().length());
                appendValue(IdentityField.PLAIN_OFFSET, 0);
            }
        }

        // Save the quote style (useful for forwards).
        appendValue(IdentityField.QUOTE_STYLE, quoteStyle);

        // Save the message format for this offset.
        appendValue(IdentityField.MESSAGE_FORMAT, messageFormat);

        // If we're not using the standard identity of signature, append it on to the identity blob.
        if (identity.getSignatureUse() && signatureChanged) {
            appendValue(IdentityField.SIGNATURE, signature);
        }

        if (identityChanged) {
            appendValue(IdentityField.NAME, Objects.requireNonNullElse(identity.getName(), ""));
            appendValue(IdentityField.EMAIL, identity.getEmail());
        }

        if (messageReference != null) {
            appendValue(IdentityField.ORIGINAL_MESSAGE, messageReference.toIdentityString());
        }

        appendValue(IdentityField.CURSOR_POSITION, cursorPosition);
        appendValue(IdentityField.QUOTED_TEXT_MODE, quotedTextMode);

        String k9identity = IdentityField.IDENTITY_VERSION_1 + uri.build().getEncodedQuery();
        String headerValue = foldHeaderValue(k9identity);

        Timber.d("Generated identity: %s", headerValue);
        return headerValue;
    }

    private String foldHeaderValue(String input) {
        int inputLength = input.length();
        int endOfFirstLine = MAX_LINE_LENGTH - FIRST_LINE_EXTRA_LENGTH;
        if (inputLength <= endOfFirstLine) {
            return input;
        }

        int extraLines = (inputLength - endOfFirstLine - 1) / (MAX_LINE_LENGTH - 1) + 1;
        int builderSize = inputLength + extraLines * 3 /* CR LF SPACE */;
        StringBuilder headerValue = new StringBuilder(builderSize);

        headerValue.append(input, 0, endOfFirstLine);
        int start = endOfFirstLine;
        while (start < inputLength) {
            headerValue.append("\r\n ");
            int end = start + Math.min(MAX_LINE_LENGTH - 1, inputLength - start);
            headerValue.append(input, start, end);
            start = end;
        }

        return headerValue.toString();
    }

    private void appendValue(IdentityField field, int value) {
        appendValue(field, Integer.toString(value));
    }

    private void appendValue(IdentityField field, Integer value) {
        appendValue(field, value.toString());
    }

    private void appendValue(IdentityField field, Enum<?> value) {
        appendValue(field, value.name());
    }

    private void appendValue(IdentityField field, String value) {
        uri.appendQueryParameter(field.value(), value);
    }

    public IdentityHeaderBuilder setQuotedHtmlContent(InsertableHtmlContent quotedHtmlContent) {
        this.quotedHtmlContent = quotedHtmlContent;
        return this;
    }

    public IdentityHeaderBuilder setQuoteStyle(QuoteStyle quoteStyle) {
        this.quoteStyle = quoteStyle;
        return this;
    }

    public IdentityHeaderBuilder setQuoteTextMode(QuotedTextMode quotedTextMode) {
        this.quotedTextMode = quotedTextMode;
        return this;
    }

    public IdentityHeaderBuilder setMessageFormat(SimpleMessageFormat messageFormat) {
        this.messageFormat = messageFormat;
        return this;
    }

    public IdentityHeaderBuilder setIdentity(Identity identity) {
        this.identity = identity;
        return this;
    }

    public IdentityHeaderBuilder setIdentityChanged(boolean identityChanged) {
        this.identityChanged = identityChanged;
        return this;
    }

    public IdentityHeaderBuilder setSignature(String signature) {
        this.signature = signature;
        return this;
    }

    public IdentityHeaderBuilder setSignatureChanged(boolean signatureChanged) {
        this.signatureChanged = signatureChanged;
        return this;
    }

    public IdentityHeaderBuilder setMessageReference(MessageReference messageReference) {
        this.messageReference = messageReference;
        return this;
    }

    public IdentityHeaderBuilder setBody(TextBody body) {
        this.body = body;
        return this;
    }

    public IdentityHeaderBuilder setBodyPlain(TextBody bodyPlain) {
        this.bodyPlain = bodyPlain;
        return this;
    }

    public IdentityHeaderBuilder setCursorPosition(int cursorPosition) {
        this.cursorPosition = cursorPosition;
        return this;
    }
}
