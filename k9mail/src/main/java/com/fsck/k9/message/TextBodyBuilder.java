package com.fsck.k9.message;


import android.text.TextUtils;
import timber.log.Timber;

import com.fsck.k9.K9;
import com.fsck.k9.message.html.HtmlConverter;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.message.quote.InsertableHtmlContent;


class TextBodyBuilder {
    private boolean includeQuotedText = true;
    private boolean replyAfterQuote = false;
    private boolean signatureBeforeQuotedText = false;
    private boolean insertSeparator = false;
    private boolean appendSignature = true;

    private String messageContent;
    private String signature;
    private String quotedText;
    private InsertableHtmlContent quotedTextHtml;

    public TextBodyBuilder(String messageContent) {
        this.messageContent = messageContent;
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
        int composedMessageOffset;

        // Get the user-supplied text
        String text = messageContent;

        // Do we have to modify an existing message to include our reply?
        if (includeQuotedText) {
            InsertableHtmlContent quotedHtmlContent = getQuotedTextHtml();

            if (K9.isDebug()) {
                Timber.d("insertable: %s", quotedHtmlContent.toDebugString());
            }

            if (appendSignature) {
                // Append signature to the reply
                if (replyAfterQuote || signatureBeforeQuotedText) {
                    text += getSignature();
                }
            }

            // Convert the text to HTML
            text = textToHtmlFragment(text);

            /*
             * Set the insertion location based upon our reply after quote
             * setting. Additionally, add some extra separators between the
             * composed message and quoted message depending on the quote
             * location. We only add the extra separators when we're
             * sending, that way when we load a draft, we don't have to know
             * the length of the separators to remove them before editing.
             */
            if (replyAfterQuote) {
                quotedHtmlContent.setInsertionLocation(
                        InsertableHtmlContent.InsertionLocation.AFTER_QUOTE);
                if (insertSeparator) {
                    text = "<br clear=\"all\">" + text;
                }
            } else {
                quotedHtmlContent.setInsertionLocation(
                        InsertableHtmlContent.InsertionLocation.BEFORE_QUOTE);
                if (insertSeparator) {
                    text += "<br><br>";
                }
            }

            if (appendSignature) {
                // Place signature immediately after the quoted text
                if (!(replyAfterQuote || signatureBeforeQuotedText)) {
                    quotedHtmlContent.insertIntoQuotedFooter(getSignatureHtml());
                }
            }

            quotedHtmlContent.setUserContent(text);

            // Save length of the body and its offset.  This is used when thawing drafts.
            composedMessageLength = text.length();
            composedMessageOffset = quotedHtmlContent.getInsertionPoint();
            text = quotedHtmlContent.toString();

        } else {
            // There is no text to quote so simply append the signature if available
            if (appendSignature) {
                text += getSignature();
            }

            // Convert the text to HTML
            text = textToHtmlFragment(text);

            //TODO: Wrap this in proper HTML tags

            composedMessageLength = text.length();
            composedMessageOffset = 0;
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
        String text = messageContent;

        // Capture composed message length before we start attaching quoted parts and signatures.
        composedMessageLength = text.length();
        composedMessageOffset = 0;

        // Do we have to modify an existing message to include our reply?
        if (includeQuotedText) {
            String quotedText = getQuotedText();

            if (appendSignature) {
                // Append signature to the text/reply
                if (replyAfterQuote || signatureBeforeQuotedText) {
                    text += getSignature();
                }
            }

            if (replyAfterQuote) {
                composedMessageOffset = quotedText.length() + "\r\n".length();
                text = quotedText + "\r\n" + text;
            } else {
                text += "\r\n\r\n" + quotedText;
            }

            if (appendSignature) {
                // Place signature immediately after the quoted text
                if (!(replyAfterQuote || signatureBeforeQuotedText)) {
                    text += getSignature();
                }
            }
        } else {
            // There is no text to quote so simply append the signature if available
            if (appendSignature) {
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
        if (!TextUtils.isEmpty(this.signature)) {
            signature = "\r\n" + this.signature;
        }

        return signature;
    }

    private String getSignatureHtml() {
        String signature = "";
        if (!TextUtils.isEmpty(this.signature)) {
            signature = textToHtmlFragment("\r\n" + this.signature);
        }
        return signature;
    }

    private String getQuotedText() {
        String quotedText = "";
        if (!TextUtils.isEmpty(this.quotedText)) {
            quotedText = this.quotedText;
        }
        return quotedText;
    }

    private InsertableHtmlContent getQuotedTextHtml() {
        return quotedTextHtml;
    }

    /**
     * protected for unit-test purposes
     */
    protected String textToHtmlFragment(String text) {
        return HtmlConverter.textToHtmlFragment(text);
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public void setIncludeQuotedText(boolean includeQuotedText) {
        this.includeQuotedText = includeQuotedText;
    }

    public void setQuotedText(String quotedText) {
        this.quotedText = quotedText;
    }

    public void setQuotedTextHtml(InsertableHtmlContent quotedTextHtml) {
        this.quotedTextHtml = quotedTextHtml;
    }

    public void setInsertSeparator(boolean insertSeparator) {
        this.insertSeparator = insertSeparator;
    }

    public void setSignatureBeforeQuotedText(boolean signatureBeforeQuotedText) {
        this.signatureBeforeQuotedText = signatureBeforeQuotedText;
    }

    public void setReplyAfterQuote(boolean replyAfterQuote) {
        this.replyAfterQuote = replyAfterQuote;
    }

    public void setAppendSignature(boolean appendSignature) {
        this.appendSignature = appendSignature;
    }
}
