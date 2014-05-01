package com.fsck.k9.mail.internet;

import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.activity.InsertableHtmlContent;
import com.fsck.k9.helper.HtmlConverter;
import com.fsck.k9.helper.StringUtils;
import com.fsck.k9.mail.Body;

public class TextBodyBuilder {
    // option
    private boolean mIncludeQuotedText = true;
    private boolean mReplyAfterQuote = false;
    private boolean mSignatureBeforeQuotedText = false;

    private boolean mInsertSeparator = false;
    private boolean mAppendSignature = true;

    // message parts
    private String mMessageContent;
    private String mSignature;

    public TextBodyBuilder(
            String messageContent,
            String signature
            ) {
        mMessageContent = messageContent;
        mSignature = signature;
    }

    /**
     * Build the {@link Body} that will contain the text of the message.
     * 
     * <p>
     * Draft messages are treated somewhat differently in that signatures are
     * not appended and HTML separators between composed text and quoted text
     * are not added.
     * </p>
     * 
     * @return {@link TextBody} instance that contains the entered text and
     *         possibly the quoted original message.
     */
    public TextBody buildTextHtml(InsertableHtmlContent quotedHtmlContent) {
        // The length of the formatted version of the user-supplied text/reply
        int composedMessageLength;

        // The offset of the user-supplied text/reply in the final text body
        int composedMessageOffset;

        // Get the user-supplied text
        String text = mMessageContent;

        // Do we have to modify an existing message to include our reply?
        if (mIncludeQuotedText && quotedHtmlContent != null) {
            if (K9.DEBUG) {
                Log.d(K9.LOG_TAG, "insertable: " + quotedHtmlContent.toDebugString());
            }

            if (mAppendSignature) {
                // Append signature to the reply
                if (mReplyAfterQuote || mSignatureBeforeQuotedText) {
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
            if (mReplyAfterQuote) {
                quotedHtmlContent.setInsertionLocation(
                        InsertableHtmlContent.InsertionLocation.AFTER_QUOTE);
                if (mInsertSeparator) {
                    text = "<br clear=\"all\">" + text;
                }
            }
            else {
                quotedHtmlContent.setInsertionLocation(
                        InsertableHtmlContent.InsertionLocation.BEFORE_QUOTE);
                if (mInsertSeparator) {
                    text += "<br><br>";
                }
            }

            if (mAppendSignature) {
                // Place signature immediately after the quoted text
                if (!(mReplyAfterQuote || mSignatureBeforeQuotedText)) {
                    quotedHtmlContent.insertIntoQuotedFooter(getSignatureHtml());
                }
            }

            quotedHtmlContent.setUserContent(text);

            // Save length of the body and its offset.  This is used when thawing drafts.
            composedMessageLength = text.length();
            composedMessageOffset = quotedHtmlContent.getInsertionPoint();
            text = quotedHtmlContent.toString();

        }
        else {
            // There is no text to quote so simply append the signature if available
            if (mAppendSignature) {
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
     * <p>
     * Draft messages are treated somewhat differently in that signatures are
     * not appended and HTML separators between composed text and quoted text
     * are not added.
     * </p>
     * 
     * @return {@link TextBody} instance that contains the entered text and
     *         possibly the quoted original message.
     */
    public TextBody buildTextPlain(String quotedText) {
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
        if (mIncludeQuotedText && !StringUtils.isNullOrEmpty(quotedText)) {

            if (mAppendSignature) {
                // Append signature to the text/reply
                if (mReplyAfterQuote || mSignatureBeforeQuotedText) {
                    text += getSignature();
                }
            }

            if (mReplyAfterQuote) {
                composedMessageOffset = quotedText.length() + "\r\n".length();
                text = quotedText + "\r\n" + text;
            } else {
                text += "\r\n\r\n" + quotedText;
            }

            if (mAppendSignature) {
                // Place signature immediately after the quoted text
                if (!(mReplyAfterQuote || mSignatureBeforeQuotedText)) {
                    text += getSignature();
                }
            }
        }
        else {
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
        if (!StringUtils.isNullOrEmpty(mSignature)) {
            signature = "\r\n" + mSignature;
        }

        return signature;
    }

    /**
     * Get an HTML version of the signature in the #mSignatureView, if any.
     * 
     * @return HTML version of signature.
     */
    private String getSignatureHtml() {
        String signature = "";
        if (!StringUtils.isNullOrEmpty(mSignature)) {
            signature = textToHtmlFragment("\r\n" + mSignature);
        }
        return signature;
    }

    public String textToHtmlFragment(String text) {
        return HtmlConverter.textToHtmlFragment(text);
    }

    // getter and setter

    public boolean isIncludeQuotedText() {
        return mIncludeQuotedText;
    }

    public void setIncludeQuotedText(boolean includeQuotedText) {
        mIncludeQuotedText = includeQuotedText;
    }

    public boolean isInsertSeparator() {
        return mInsertSeparator;
    }

    public void setInsertSeparator(boolean insertSeparator) {
        mInsertSeparator = insertSeparator;
    }

    public boolean isSignatureBeforeQuotedText() {
        return mSignatureBeforeQuotedText;
    }

    public void setSignatureBeforeQuotedText(boolean signatureBeforeQuotedText) {
        mSignatureBeforeQuotedText = signatureBeforeQuotedText;
    }

    public boolean isReplyAfterQuote() {
        return mReplyAfterQuote;
    }

    public void setReplyAfterQuote(boolean replyAfterQuote) {
        mReplyAfterQuote = replyAfterQuote;
    }

    public boolean isAppendSignature() {
        return mAppendSignature;
    }

    public void setAppendSignature(boolean appendSignature) {
        mAppendSignature = appendSignature;
    }
}
