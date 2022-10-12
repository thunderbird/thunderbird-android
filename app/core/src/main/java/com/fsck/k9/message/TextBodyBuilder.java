package com.fsck.k9.message;


import timber.log.Timber;

import com.fsck.k9.K9;
import com.fsck.k9.message.html.HtmlConverter;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.message.quote.InsertableHtmlContent;


class TextBodyBuilder {
    public static final String HTML_AND_BODY_START = "<!DOCTYPE html><html><body>";
    public static final String HTML_AND_BODY_END = "</body></html>";

    private boolean mIncludeQuotedText = true;
    private boolean mReplyAfterQuote = false;
    private boolean mSignatureBeforeQuotedText = false;
    private boolean mInsertSeparator = false;
    private boolean mAppendSignature = true;

    private String mMessageContent;
    private String mSignature;
    private String mQuotedText;
    private InsertableHtmlContent mQuotedTextHtml;

    public TextBodyBuilder(String messageContent) {
        mMessageContent = messageContent;
    }

    /**
     * Build the {@link Body} that will contain the text of the message.
     *
     * @return {@link com.fsck.k9.mail.internet.TextBody} instance that contains the entered text and
     *         possibly the quoted original message.
     */
    public TextBody buildTextHtml() {
        // The length of the formatted version of the user-supplied text/reply
        int composedMessageLength;

        // The offset of the user-supplied text/reply in the final text body
        int composedMessageOffset = 0;

        // Get the user-supplied text
        String text = mMessageContent;

        // Do we have to modify an existing message to include our reply?
        if (mIncludeQuotedText) {
            InsertableHtmlContent quotedHtmlContent = getQuotedTextHtml();

            if (K9.isDebugLoggingEnabled()) {
                Timber.d("insertable: %s", quotedHtmlContent.toDebugString());
            }

            // Convert the text to HTML
            text = textToHtmlFragment(text);

            // Save length of the body. This is used when thawing drafts.
            composedMessageLength = text.length();

            // Append signature to the reply
            if (mAppendSignature && (mReplyAfterQuote || mSignatureBeforeQuotedText)) {
                text += getSignatureHtml();
            }

            /*
             * Set the insertion location based upon our reply after quote
             * setting. Additionally, add some extra separators between the
             * composed message and quoted message depending on the quote
             * location. We only add the extra separators when we're
             * sending, that way when we load a draft, we don't have to know
             * the length of the separators to remove them before editing.
             */
            if (mReplyAfterQuote) {
                quotedHtmlContent.setInsertionLocation(
                        InsertableHtmlContent.InsertionLocation.AFTER_QUOTE);
                if (mInsertSeparator) {
                    final String separator = "<br clear=\"all\">";
                    text = separator + text;
                    composedMessageOffset = separator.length();
                }
            } else {
                quotedHtmlContent.setInsertionLocation(
                        InsertableHtmlContent.InsertionLocation.BEFORE_QUOTE);
                if (mInsertSeparator) {
                    text += "<br><br>";
                }
            }

            // Place signature immediately after the quoted text
            if (mAppendSignature && !(mReplyAfterQuote || mSignatureBeforeQuotedText)) {
                quotedHtmlContent.insertIntoQuotedFooter(getSignatureHtml());
            }

            quotedHtmlContent.setUserContent(text);

            // Save the body's offset. This is used when thawing drafts.
            composedMessageOffset += quotedHtmlContent.getInsertionPoint();
            text = quotedHtmlContent.toString();

        } else {
            // Convert the text to HTML
            text = textToHtmlFragment(text);

            composedMessageLength = text.length();
            composedMessageOffset = HTML_AND_BODY_START.length();

            // There is no text to quote so simply append the signature if available
            if (mAppendSignature) {
                text += getSignatureHtml();
            }

            text = HTML_AND_BODY_START + text + HTML_AND_BODY_END;
        }

        TextBody body = new TextBody(text);
        body.setComposedMessageLength(composedMessageLength);
        body.setComposedMessageOffset(composedMessageOffset);

        return body;
    }

    /**
     * Build the {@link Body} that will contain the text of the message.
     *
     * @return {@link TextBody} instance that contains the entered text and
     *         possibly the quoted original message.
     */
    public TextBody buildTextPlain() {
        // The length of the formatted version of the user-supplied text/reply
        int composedMessageLength;

        // The offset of the user-supplied text/reply in the final text body
        int composedMessageOffset;

        // Get the user-supplied text
        String text = mMessageContent;

        // Capture composed message length before we start attaching quoted parts and signatures.
        composedMessageLength = text.length();
        composedMessageOffset = 0;

        // Do we have to modify an existing message to include our reply?
        if (mIncludeQuotedText) {
            String quotedText = getQuotedText();

            // Append signature to the text/reply
            if (mAppendSignature && (mReplyAfterQuote || mSignatureBeforeQuotedText)) {
                text += getSignature();
            }

            if (mReplyAfterQuote) {
                composedMessageOffset = quotedText.length() + "\r\n".length();
                text = quotedText + "\r\n" + text;
            } else {
                text += "\r\n\r\n" + quotedText;
            }

            // Place signature immediately after the quoted text
            if (mAppendSignature && !(mReplyAfterQuote || mSignatureBeforeQuotedText)) {
                text += getSignature();
            }
        } else {
            // There is no text to quote so simply append the signature if available
            if (mAppendSignature) {
                // Append signature to the text/reply
                text += getSignature();
            }
        }

        TextBody body = new TextBody(text);
        body.setComposedMessageLength(composedMessageLength);
        body.setComposedMessageOffset(composedMessageOffset);

        return body;
    }

    private String getSignature() {
        String signature = "";
        if (!isEmpty(mSignature)) {
            signature = "\r\n" + mSignature;
        }

        return signature;
    }

    private String getSignatureHtml() {
        String signature = "";
        if (!isEmpty(mSignature)) {
            signature = HtmlConverter.textToHtmlFragment(mSignature);
        }
        return signature;
    }

    private String getQuotedText() {
        String quotedText = "";
        if (!isEmpty(mQuotedText)) {
            quotedText = mQuotedText;
        }
        return quotedText;
    }

    private InsertableHtmlContent getQuotedTextHtml() {
        return mQuotedTextHtml;
    }

    private String textToHtmlFragment(String text) {
        return HtmlConverter.textToHtmlFragment(text);
    }

    public void setSignature(String signature) {
        mSignature = signature;
    }

    public void setIncludeQuotedText(boolean includeQuotedText) {
        mIncludeQuotedText = includeQuotedText;
    }

    public void setQuotedText(String quotedText) {
        mQuotedText = quotedText;
    }

    public void setQuotedTextHtml(InsertableHtmlContent quotedTextHtml) {
        mQuotedTextHtml = quotedTextHtml;
    }

    public void setInsertSeparator(boolean insertSeparator) {
        mInsertSeparator = insertSeparator;
    }

    public void setSignatureBeforeQuotedText(boolean signatureBeforeQuotedText) {
        mSignatureBeforeQuotedText = signatureBeforeQuotedText;
    }

    public void setReplyAfterQuote(boolean replyAfterQuote) {
        mReplyAfterQuote = replyAfterQuote;
    }

    public void setAppendSignature(boolean appendSignature) {
        mAppendSignature = appendSignature;
    }

    private static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }
}
