package com.fsck.k9.message;


import android.net.Uri;
import android.util.Log;

import com.fsck.k9.Account.QuoteStyle;
import com.fsck.k9.Identity;
import com.fsck.k9.K9;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mail.internet.TextBody;


public class IdentityHeaderBuilder {
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

        Uri.Builder uri = new Uri.Builder();
        if (body.getComposedMessageLength() != null && body.getComposedMessageOffset() != null) {
            // See if the message body length is already in the TextBody.
            uri.appendQueryParameter(IdentityField.LENGTH.value(), body.getComposedMessageLength().toString());
            uri.appendQueryParameter(IdentityField.OFFSET.value(), body.getComposedMessageOffset().toString());
        } else {
            // If not, calculate it now.
            uri.appendQueryParameter(IdentityField.LENGTH.value(), Integer.toString(body.getText().length()));
            uri.appendQueryParameter(IdentityField.OFFSET.value(), Integer.toString(0));
        }
        if (quotedHtmlContent != null) {
            uri.appendQueryParameter(IdentityField.FOOTER_OFFSET.value(),
                    Integer.toString(quotedHtmlContent.getFooterInsertionPoint()));
        }
        if (bodyPlain != null) {
            if (bodyPlain.getComposedMessageLength() != null && bodyPlain.getComposedMessageOffset() != null) {
                // See if the message body length is already in the TextBody.
                uri.appendQueryParameter(IdentityField.PLAIN_LENGTH.value(), bodyPlain.getComposedMessageLength().toString());
                uri.appendQueryParameter(IdentityField.PLAIN_OFFSET.value(), bodyPlain.getComposedMessageOffset().toString());
            } else {
                // If not, calculate it now.
                uri.appendQueryParameter(IdentityField.PLAIN_LENGTH.value(), Integer.toString(body.getText().length()));
                uri.appendQueryParameter(IdentityField.PLAIN_OFFSET.value(), Integer.toString(0));
            }
        }
        // Save the quote style (useful for forwards).
        uri.appendQueryParameter(IdentityField.QUOTE_STYLE.value(), quoteStyle.name());

        // Save the message format for this offset.
        uri.appendQueryParameter(IdentityField.MESSAGE_FORMAT.value(), messageFormat.name());

        // If we're not using the standard identity of signature, append it on to the identity blob.
        if (identity.getSignatureUse() && signatureChanged) {
            uri.appendQueryParameter(IdentityField.SIGNATURE.value(), signature);
        }

        if (identityChanged) {
            uri.appendQueryParameter(IdentityField.NAME.value(), identity.getName());
            uri.appendQueryParameter(IdentityField.EMAIL.value(), identity.getEmail());
        }

        if (messageReference != null) {
            uri.appendQueryParameter(IdentityField.ORIGINAL_MESSAGE.value(), messageReference.toIdentityString());
        }

        uri.appendQueryParameter(IdentityField.CURSOR_POSITION.value(), Integer.toString(cursorPosition));

        uri.appendQueryParameter(IdentityField.QUOTED_TEXT_MODE.value(), quotedTextMode.name());

        String k9identity = IdentityField.IDENTITY_VERSION_1 + uri.build().getEncodedQuery();

        if (K9.DEBUG) {
            Log.d(K9.LOG_TAG, "Generated identity: " + k9identity);
        }

        return k9identity;
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
