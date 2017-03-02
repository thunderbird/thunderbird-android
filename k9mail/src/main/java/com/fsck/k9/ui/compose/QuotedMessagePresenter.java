package com.fsck.k9.ui.compose;


import java.util.Map;

import android.content.res.Resources;
import android.os.Bundle;
import timber.log.Timber;

import com.fsck.k9.Account;
import com.fsck.k9.Account.MessageFormat;
import com.fsck.k9.Account.QuoteStyle;
import com.fsck.k9.K9;
import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.activity.MessageCompose.Action;
import com.fsck.k9.message.extractors.BodyTextExtractor;
import com.fsck.k9.message.html.HtmlConverter;
import com.fsck.k9.message.quote.HtmlQuoteCreator;
import com.fsck.k9.message.quote.TextQuoteCreator;
import com.fsck.k9.message.signature.HtmlSignatureRemover;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MessageExtractor;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mailstore.AttachmentResolver;
import com.fsck.k9.mailstore.MessageViewInfo;
import com.fsck.k9.message.IdentityField;
import com.fsck.k9.message.quote.InsertableHtmlContent;
import com.fsck.k9.message.MessageBuilder;
import com.fsck.k9.message.QuotedTextMode;
import com.fsck.k9.message.SimpleMessageFormat;
import com.fsck.k9.message.signature.TextSignatureRemover;


public class QuotedMessagePresenter {
    private static final String STATE_KEY_HTML_QUOTE = "state:htmlQuote";
    private static final String STATE_KEY_QUOTED_TEXT_MODE = "state:quotedTextShown";
    private static final String STATE_KEY_QUOTED_TEXT_FORMAT = "state:quotedTextFormat";
    private static final String STATE_KEY_FORCE_PLAIN_TEXT = "state:forcePlainText";

    private static final int UNKNOWN_LENGTH = 0;


    private final QuotedMessageMvpView view;
    private final MessageCompose messageCompose;
    private final Resources resources;

    private QuotedTextMode quotedTextMode;
    private QuoteStyle quoteStyle;
    private boolean forcePlainText;


    private SimpleMessageFormat quotedTextFormat;
    private InsertableHtmlContent quotedHtmlContent;
    private Account account;


    public QuotedMessagePresenter(
            MessageCompose messageCompose, QuotedMessageMvpView quotedMessageMvpView, Account account) {
        this.messageCompose = messageCompose;
        this.resources = messageCompose.getResources();
        this.view = quotedMessageMvpView;
        onSwitchAccount(account);

        quotedTextMode = QuotedTextMode.NONE;
        quoteStyle = account.getQuoteStyle();

        quotedMessageMvpView.setOnClickPresenter(this);
    }

    public void onSwitchAccount(Account account) {
        this.account = account;
    }

    public void showOrHideQuotedText(QuotedTextMode mode) {
        quotedTextMode = mode;
        view.showOrHideQuotedText(mode, quotedTextFormat);
    }

    /**
     * Build and populate the UI with the quoted message.
     *
     * @param showQuotedText
     *         {@code true} if the quoted text should be shown, {@code false} otherwise.
     */
    public void populateUIWithQuotedMessage(MessageViewInfo messageViewInfo, boolean showQuotedText, Action action)
            throws MessagingException {
        MessageFormat origMessageFormat = account.getMessageFormat();

        if (forcePlainText || origMessageFormat == MessageFormat.TEXT) {
            // Use plain text for the quoted message
            quotedTextFormat = SimpleMessageFormat.TEXT;
        } else if (origMessageFormat == MessageFormat.AUTO) {
            // Figure out which message format to use for the quoted text by looking if the source
            // message contains a text/html part. If it does, we use that.
            quotedTextFormat =
                    (MimeUtility.findFirstPartByMimeType(messageViewInfo.rootPart, "text/html") == null) ?
                            SimpleMessageFormat.TEXT : SimpleMessageFormat.HTML;
        } else {
            quotedTextFormat = SimpleMessageFormat.HTML;
        }

        // Handle the original message in the reply
        // If we already have sourceMessageBody, use that.  It's pre-populated if we've got crypto going on.
        String content = BodyTextExtractor.getBodyTextFromMessage(messageViewInfo.rootPart, quotedTextFormat);

        if (quotedTextFormat == SimpleMessageFormat.HTML) {
            // Strip signature.
            // closing tags such as </div>, </span>, </table>, </pre> will be cut off.
            if (account.isStripSignature() && (action == Action.REPLY || action == Action.REPLY_ALL)) {
                content = HtmlSignatureRemover.stripSignature(content);
            }

            // Add the HTML reply header to the top of the content.
            quotedHtmlContent = HtmlQuoteCreator.quoteOriginalHtmlMessage(
                    resources, messageViewInfo.message, content, quoteStyle);

            // Load the message with the reply header. TODO replace with MessageViewInfo data
            view.setQuotedHtml(quotedHtmlContent.getQuotedContent(),
                    AttachmentResolver.createFromPart(messageViewInfo.rootPart));

            // TODO: Also strip the signature from the text/plain part
            view.setQuotedText(TextQuoteCreator.quoteOriginalTextMessage(resources, messageViewInfo.message,
                    BodyTextExtractor.getBodyTextFromMessage(messageViewInfo.rootPart, SimpleMessageFormat.TEXT),
                    quoteStyle, account.getQuotePrefix()));

        } else if (quotedTextFormat == SimpleMessageFormat.TEXT) {
            if (account.isStripSignature() && (action == Action.REPLY || action == Action.REPLY_ALL)) {
                content = TextSignatureRemover.stripSignature(content);
            }

            view.setQuotedText(TextQuoteCreator.quoteOriginalTextMessage(
                    resources, messageViewInfo.message, content, quoteStyle, account.getQuotePrefix()));
        }

        if (showQuotedText) {
            showOrHideQuotedText(QuotedTextMode.SHOW);
        } else {
            showOrHideQuotedText(QuotedTextMode.HIDE);
        }
    }

    public void builderSetProperties(MessageBuilder builder) {
        builder.setQuoteStyle(quoteStyle)
                // TODO avoid using a getter from the view!
                .setQuotedText(view.getQuotedText())
                .setQuotedTextMode(quotedTextMode)
                .setQuotedHtmlContent(quotedHtmlContent)
                .setReplyAfterQuote(account.isReplyAfterQuote());
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(STATE_KEY_QUOTED_TEXT_MODE, quotedTextMode);
        outState.putSerializable(STATE_KEY_HTML_QUOTE, quotedHtmlContent);
        outState.putSerializable(STATE_KEY_QUOTED_TEXT_FORMAT, quotedTextFormat);
        outState.putBoolean(STATE_KEY_FORCE_PLAIN_TEXT, forcePlainText);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        quotedHtmlContent = (InsertableHtmlContent) savedInstanceState.getSerializable(STATE_KEY_HTML_QUOTE);
        if (quotedHtmlContent != null && quotedHtmlContent.getQuotedContent() != null) {
            // we don't have the part here, but inline-displayed images are cached by the webview
            view.setQuotedHtml(quotedHtmlContent.getQuotedContent(), null);
        }
        quotedTextFormat = (SimpleMessageFormat) savedInstanceState.getSerializable(
                STATE_KEY_QUOTED_TEXT_FORMAT);
        forcePlainText = savedInstanceState.getBoolean(STATE_KEY_FORCE_PLAIN_TEXT);

        showOrHideQuotedText(
                (QuotedTextMode) savedInstanceState.getSerializable(STATE_KEY_QUOTED_TEXT_MODE));
    }

    public void processMessageToForward(MessageViewInfo messageViewInfo) throws MessagingException {
        quoteStyle = QuoteStyle.HEADER;
        populateUIWithQuotedMessage(messageViewInfo, true, Action.FORWARD);
    }

    public void initFromReplyToMessage(MessageViewInfo messageViewInfo, Action action)
            throws MessagingException {
        populateUIWithQuotedMessage(messageViewInfo, account.isDefaultQuotedTextShown(), action);
    }

    public void processDraftMessage(MessageViewInfo messageViewInfo, Map<IdentityField, String> k9identity) {
        quoteStyle = k9identity.get(IdentityField.QUOTE_STYLE) != null
                ? QuoteStyle.valueOf(k9identity.get(IdentityField.QUOTE_STYLE))
                : account.getQuoteStyle();

        int cursorPosition = 0;
        if (k9identity.containsKey(IdentityField.CURSOR_POSITION)) {
            try {
                cursorPosition = Integer.parseInt(k9identity.get(IdentityField.CURSOR_POSITION));
            } catch (Exception e) {
                Timber.e(e, "Could not parse cursor position for MessageCompose; continuing.");
            }
        }

        String showQuotedTextMode;
        if (k9identity.containsKey(IdentityField.QUOTED_TEXT_MODE)) {
            showQuotedTextMode = k9identity.get(IdentityField.QUOTED_TEXT_MODE);
        } else {
            showQuotedTextMode = "NONE";
        }

        int bodyLength = k9identity.get(IdentityField.LENGTH) != null ?
                Integer.valueOf(k9identity.get(IdentityField.LENGTH)) : UNKNOWN_LENGTH;
        int bodyOffset = k9identity.get(IdentityField.OFFSET) != null ?
                Integer.valueOf(k9identity.get(IdentityField.OFFSET)) : UNKNOWN_LENGTH;
        Integer bodyFooterOffset = k9identity.get(IdentityField.FOOTER_OFFSET) != null ?
                Integer.valueOf(k9identity.get(IdentityField.FOOTER_OFFSET)) : null;
        Integer bodyPlainLength = k9identity.get(IdentityField.PLAIN_LENGTH) != null ?
                Integer.valueOf(k9identity.get(IdentityField.PLAIN_LENGTH)) : null;
        Integer bodyPlainOffset = k9identity.get(IdentityField.PLAIN_OFFSET) != null ?
                Integer.valueOf(k9identity.get(IdentityField.PLAIN_OFFSET)) : null;

        QuotedTextMode quotedMode;
        try {
            quotedMode = QuotedTextMode.valueOf(showQuotedTextMode);
        } catch (Exception e) {
            quotedMode = QuotedTextMode.NONE;
        }

        // Always respect the user's current composition format preference, even if the
        // draft was saved in a different format.
        // TODO - The current implementation doesn't allow a user in HTML mode to edit a draft that wasn't saved with K9mail.
        String messageFormatString = k9identity.get(IdentityField.MESSAGE_FORMAT);

        MessageFormat messageFormat = null;
        if (messageFormatString != null) {
            try {
                messageFormat = MessageFormat.valueOf(messageFormatString);
            } catch (Exception e) { /* do nothing */ }
        }

        if (messageFormat == null) {
            // This message probably wasn't created by us. The exception is legacy
            // drafts created before the advent of HTML composition. In those cases,
            // we'll display the whole message (including the quoted part) in the
            // composition window. If that's the case, try and convert it to text to
            // match the behavior in text mode.
            view.setMessageContentCharacters(
                    BodyTextExtractor.getBodyTextFromMessage(messageViewInfo.message, SimpleMessageFormat.TEXT));
            forcePlainText = true;

            showOrHideQuotedText(quotedMode);
            return;
        }

        if (messageFormat == MessageFormat.HTML) {
            Part part = MimeUtility.findFirstPartByMimeType(messageViewInfo.message, "text/html");
            if (part != null) { // Shouldn't happen if we were the one who saved it.
                quotedTextFormat = SimpleMessageFormat.HTML;
                String text = MessageExtractor.getTextFromPart(part);

                Timber.d("Loading message with offset %d, length %d. Text length is %d.",
                        bodyOffset, bodyLength, text.length());

                if (bodyOffset + bodyLength > text.length()) {
                    // The draft was edited outside of K-9 Mail?
                    Timber.d("The identity field from the draft contains an invalid LENGTH/OFFSET");
                    bodyOffset = 0;
                    bodyLength = 0;
                }
                // Grab our reply text.
                String bodyText = text.substring(bodyOffset, bodyOffset + bodyLength);
                view.setMessageContentCharacters(HtmlConverter.htmlToText(bodyText));

                // Regenerate the quoted html without our user content in it.
                StringBuilder quotedHTML = new StringBuilder();
                quotedHTML.append(text.substring(0, bodyOffset));   // stuff before the reply
                quotedHTML.append(text.substring(bodyOffset + bodyLength));
                if (quotedHTML.length() > 0) {
                    quotedHtmlContent = new InsertableHtmlContent();
                    quotedHtmlContent.setQuotedContent(quotedHTML);
                    // We don't know if bodyOffset refers to the header or to the footer
                    quotedHtmlContent.setHeaderInsertionPoint(bodyOffset);
                    if (bodyFooterOffset != null) {
                        quotedHtmlContent.setFooterInsertionPoint(bodyFooterOffset);
                    } else {
                        quotedHtmlContent.setFooterInsertionPoint(bodyOffset);
                    }
                    // TODO replace with MessageViewInfo data
                    view.setQuotedHtml(quotedHtmlContent.getQuotedContent(),
                            AttachmentResolver.createFromPart(messageViewInfo.rootPart));
                }
            }
            if (bodyPlainOffset != null && bodyPlainLength != null) {
                processSourceMessageText(messageViewInfo.rootPart, bodyPlainOffset, bodyPlainLength, false);
            }
        } else if (messageFormat == MessageFormat.TEXT) {
            quotedTextFormat = SimpleMessageFormat.TEXT;
            processSourceMessageText(messageViewInfo.rootPart, bodyOffset, bodyLength, true);
        } else {
            Timber.e("Unhandled message format.");
        }

        // Set the cursor position if we have it.
        try {
            view.setMessageContentCursorPosition(cursorPosition);
        } catch (Exception e) {
            Timber.e(e, "Could not set cursor position in MessageCompose; ignoring.");
        }

        showOrHideQuotedText(quotedMode);
    }

    /**
     * Pull out the parts of the now loaded source message and apply them to the new message
     * depending on the type of message being composed.
     * @param bodyOffset Insertion point for reply.
     * @param bodyLength Length of reply.
     * @param viewMessageContent Update mMessageContentView or not.
     */
    private void processSourceMessageText(
            Part rootMessagePart, int bodyOffset, int bodyLength, boolean viewMessageContent) {
        Part textPart = MimeUtility.findFirstPartByMimeType(rootMessagePart, "text/plain");
        if (textPart == null) {
            return;
        }

        String messageText = MessageExtractor.getTextFromPart(textPart);

        Timber.d("Loading message with offset %d, length %d. Text length is %d.",
                bodyOffset, bodyLength, messageText.length());

        // If we had a body length (and it was valid), separate the composition from the quoted text
        // and put them in their respective places in the UI.
        if (bodyLength != UNKNOWN_LENGTH) {
            try {
                // Regenerate the quoted text without our user content in it nor added newlines.
                StringBuilder quotedText = new StringBuilder();
                if (bodyOffset == UNKNOWN_LENGTH &&
                        messageText.substring(bodyLength, bodyLength + 4).equals("\r\n\r\n")) {
                    // top-posting: ignore two newlines at start of quote
                    quotedText.append(messageText.substring(bodyLength + 4));
                } else if (bodyOffset + bodyLength == messageText.length() &&
                        messageText.substring(bodyOffset - 2, bodyOffset).equals("\r\n")) {
                    // bottom-posting: ignore newline at end of quote
                    quotedText.append(messageText.substring(0, bodyOffset - 2));
                } else {
                    quotedText.append(messageText.substring(0, bodyOffset));   // stuff before the reply
                    quotedText.append(messageText.substring(bodyOffset + bodyLength));
                }

                view.setQuotedText(quotedText.toString());

                messageText = messageText.substring(bodyOffset, bodyOffset + bodyLength);
            } catch (IndexOutOfBoundsException e) {
                // Invalid bodyOffset or bodyLength.  The draft was edited outside of K-9 Mail?
                Timber.d("The identity field from the draft contains an invalid bodyOffset/bodyLength");
            }
        }

        if (viewMessageContent) {
            view.setMessageContentCharacters(messageText);
        }
    }

    void onClickShowQuotedText() {
        showOrHideQuotedText(QuotedTextMode.SHOW);
        messageCompose.updateMessageFormat();
        messageCompose.saveDraftEventually();
    }

    void onClickDeleteQuotedText() {
        showOrHideQuotedText(QuotedTextMode.HIDE);
        messageCompose.updateMessageFormat();
        messageCompose.saveDraftEventually();
    }

    void onClickEditQuotedText() {
        forcePlainText = true;
        messageCompose.loadQuotedTextForEdit();
    }

    public boolean includeQuotedText() {
        return quotedTextMode == QuotedTextMode.SHOW;
    }

    public boolean isForcePlainText() {
        return forcePlainText;
    }

    public boolean isQuotedTextText() {
        return quotedTextFormat == SimpleMessageFormat.TEXT;
    }
}
