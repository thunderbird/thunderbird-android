package com.fsck.k9.mailstore;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;

import com.fsck.k9.Globals;
import com.fsck.k9.R;
import com.fsck.k9.helper.HtmlConverter;
import com.fsck.k9.helper.HtmlSanitizer;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MessageExtractor;
import com.fsck.k9.mail.internet.Viewable;
import com.fsck.k9.message.extractors.AttachmentInfoExtractor;
import com.fsck.k9.ui.crypto.MessageCryptoAnnotations;
import com.fsck.k9.ui.crypto.MessageCryptoSplitter;
import com.fsck.k9.ui.crypto.MessageCryptoSplitter.CryptoMessageParts;

import static com.fsck.k9.mail.internet.MimeUtility.getHeaderParameter;
import static com.fsck.k9.mail.internet.Viewable.Alternative;
import static com.fsck.k9.mail.internet.Viewable.Html;
import static com.fsck.k9.mail.internet.Viewable.MessageHeader;
import static com.fsck.k9.mail.internet.Viewable.Text;
import static com.fsck.k9.mail.internet.Viewable.Textual;

public class MessageViewInfoExtractor {
    private static final String TEXT_DIVIDER =
            "------------------------------------------------------------------------";
    private static final int TEXT_DIVIDER_LENGTH = TEXT_DIVIDER.length();
    private static final String FILENAME_PREFIX = "----- ";
    private static final int FILENAME_PREFIX_LENGTH = FILENAME_PREFIX.length();
    private static final String FILENAME_SUFFIX = " ";
    private static final int FILENAME_SUFFIX_LENGTH = FILENAME_SUFFIX.length();


    private final Context context;
    private final AttachmentInfoExtractor attachmentInfoExtractor;
    private final HtmlSanitizer htmlSanitizer;


    public static MessageViewInfoExtractor getInstance() {
        Context context = Globals.getContext();
        AttachmentInfoExtractor attachmentInfoExtractor = AttachmentInfoExtractor.getInstance();
        HtmlSanitizer htmlSanitizer = HtmlSanitizer.getInstance();
        return new MessageViewInfoExtractor(context, attachmentInfoExtractor, htmlSanitizer);
    }

    @VisibleForTesting
    MessageViewInfoExtractor(Context context, AttachmentInfoExtractor attachmentInfoExtractor,
            HtmlSanitizer htmlSanitizer) {
        this.context = context;
        this.attachmentInfoExtractor = attachmentInfoExtractor;
        this.htmlSanitizer = htmlSanitizer;
    }

    @WorkerThread
    public MessageViewInfo extractMessageForView(Message message, MessageCryptoAnnotations annotations)
            throws MessagingException {
        Part rootPart;
        CryptoResultAnnotation cryptoResultAnnotation;
        List<Part> extraParts;

        CryptoMessageParts cryptoMessageParts = MessageCryptoSplitter.split(message, annotations);
        if (cryptoMessageParts != null) {
            rootPart = cryptoMessageParts.contentPart;
            cryptoResultAnnotation = cryptoMessageParts.contentCryptoAnnotation;
            extraParts = cryptoMessageParts.extraParts;
        } else {
            rootPart = message;
            cryptoResultAnnotation = null;
            extraParts = null;
        }

        List<AttachmentViewInfo> attachmentInfos = new ArrayList<>();
        ViewableExtractedText viewable = extractViewableAndAttachments(
                Collections.singletonList(rootPart), attachmentInfos);

        List<AttachmentViewInfo> extraAttachmentInfos = new ArrayList<>();
        String extraViewableText = null;
        if (extraParts != null) {
            ViewableExtractedText extraViewable =
                    extractViewableAndAttachments(extraParts, extraAttachmentInfos);
            extraViewableText = extraViewable.text;
        }

        AttachmentResolver attachmentResolver = AttachmentResolver.createFromPart(rootPart);

        boolean isMessageIncomplete = !message.isSet(Flag.X_DOWNLOADED_FULL) ||
                MessageExtractor.hasMissingParts(message);

        return MessageViewInfo.createWithExtractedContent(message, isMessageIncomplete, rootPart, viewable.html,
                attachmentInfos, cryptoResultAnnotation, attachmentResolver, extraViewableText, extraAttachmentInfos);
    }

    private ViewableExtractedText extractViewableAndAttachments(List<Part> parts,
            List<AttachmentViewInfo> attachmentInfos) throws MessagingException {
        ArrayList<Viewable> viewableParts = new ArrayList<>();
        ArrayList<Part> attachments = new ArrayList<>();

        for (Part part : parts) {
            MessageExtractor.findViewablesAndAttachments(part, viewableParts, attachments);
        }

        attachmentInfos.addAll(attachmentInfoExtractor.extractAttachmentInfoForView(attachments));
        return extractTextFromViewables(viewableParts);
    }

    /**
     * Extract the viewable textual parts of a message and return the rest as attachments.
     *
     * @return A {@link ViewableExtractedText} instance containing the textual parts of the message as
     *         plain text and HTML, and a list of message parts considered attachments.
     *
     * @throws com.fsck.k9.mail.MessagingException
     *          In case of an error.
     */
    @VisibleForTesting
    ViewableExtractedText extractTextFromViewables(List<Viewable> viewables)
            throws MessagingException {
        try {
            // Collect all viewable parts

            /*
             * Convert the tree of viewable parts into text and HTML
             */

            // Used to suppress the divider for the first viewable part
            boolean hideDivider = true;

            StringBuilder text = new StringBuilder();
            StringBuilder html = new StringBuilder();

            for (Viewable viewable : viewables) {
                if (viewable instanceof Textual) {
                    // This is either a text/plain or text/html part. Fill the variables 'text' and
                    // 'html', converting between plain text and HTML as necessary.
                    text.append(buildText(viewable, !hideDivider));
                    html.append(buildHtml(viewable, !hideDivider));
                    hideDivider = false;
                } else if (viewable instanceof MessageHeader) {
                    MessageHeader header = (MessageHeader) viewable;
                    Part containerPart = header.getContainerPart();
                    Message innerMessage =  header.getMessage();

                    addTextDivider(text, containerPart, !hideDivider);
                    addMessageHeaderText(text, innerMessage);

                    addHtmlDivider(html, containerPart, !hideDivider);
                    addMessageHeaderHtml(html, innerMessage);

                    hideDivider = true;
                } else if (viewable instanceof Alternative) {
                    // Handle multipart/alternative contents
                    Alternative alternative = (Alternative) viewable;

                    /*
                     * We made sure at least one of text/plain or text/html is present when
                     * creating the Alternative object. If one part is not present we convert the
                     * other one to make sure 'text' and 'html' always contain the same text.
                     */
                    List<Viewable> textAlternative = alternative.getText().isEmpty() ?
                            alternative.getHtml() : alternative.getText();
                    List<Viewable> htmlAlternative = alternative.getHtml().isEmpty() ?
                            alternative.getText() : alternative.getHtml();

                    // Fill the 'text' variable
                    boolean divider = !hideDivider;
                    for (Viewable textViewable : textAlternative) {
                        text.append(buildText(textViewable, divider));
                        divider = true;
                    }

                    // Fill the 'html' variable
                    divider = !hideDivider;
                    for (Viewable htmlViewable : htmlAlternative) {
                        html.append(buildHtml(htmlViewable, divider));
                        divider = true;
                    }
                    hideDivider = false;
                }
            }

            String content = HtmlConverter.wrapMessageContent(html);
            String sanitizedHtml = htmlSanitizer.sanitize(content);

            return new ViewableExtractedText(text.toString(), sanitizedHtml);
        } catch (Exception e) {
            throw new MessagingException("Couldn't extract viewable parts", e);
        }
    }

    /**
     * Use the contents of a {@link com.fsck.k9.mail.internet.Viewable} to create the HTML to be displayed.
     *
     * <p>
     * This will use {@link com.fsck.k9.helper.HtmlConverter#textToHtml(String)} to convert plain text parts
     * to HTML if necessary.
     * </p>
     *
     * @param viewable
     *         The viewable part to build the HTML from.
     * @param prependDivider
     *         {@code true}, if the HTML divider should be inserted as first element.
     *         {@code false}, otherwise.
     *
     * @return The contents of the supplied viewable instance as HTML.
     */
    private StringBuilder buildHtml(Viewable viewable, boolean prependDivider) {
        StringBuilder html = new StringBuilder();
        if (viewable instanceof Textual) {
            Part part = ((Textual)viewable).getPart();
            addHtmlDivider(html, part, prependDivider);

            String t = MessageExtractor.getTextFromPart(part);
            if (t == null) {
                t = "";
            } else if (viewable instanceof Text) {
                t = HtmlConverter.textToHtml(t);
            }
            html.append(t);
        } else if (viewable instanceof Alternative) {
            // That's odd - an Alternative as child of an Alternative; go ahead and try to use the
            // text/html child; fall-back to the text/plain part.
            Alternative alternative = (Alternative) viewable;

            List<Viewable> htmlAlternative = alternative.getHtml().isEmpty() ?
                    alternative.getText() : alternative.getHtml();

            boolean divider = prependDivider;
            for (Viewable htmlViewable : htmlAlternative) {
                html.append(buildHtml(htmlViewable, divider));
                divider = true;
            }
        }

        return html;
    }

    private StringBuilder buildText(Viewable viewable, boolean prependDivider) {
        StringBuilder text = new StringBuilder();
        if (viewable instanceof Textual) {
            Part part = ((Textual)viewable).getPart();
            addTextDivider(text, part, prependDivider);

            String t = MessageExtractor.getTextFromPart(part);
            if (t == null) {
                t = "";
            } else if (viewable instanceof Html) {
                t = HtmlConverter.htmlToText(t);
            }
            text.append(t);
        } else if (viewable instanceof Alternative) {
            // That's odd - an Alternative as child of an Alternative; go ahead and try to use the
            // text/plain child; fall-back to the text/html part.
            Alternative alternative = (Alternative) viewable;

            List<Viewable> textAlternative = alternative.getText().isEmpty() ?
                    alternative.getHtml() : alternative.getText();

            boolean divider = prependDivider;
            for (Viewable textViewable : textAlternative) {
                text.append(buildText(textViewable, divider));
                divider = true;
            }
        }

        return text;
    }

    /**
     * Add an HTML divider between two HTML message parts.
     *
     * @param html
     *         The {@link StringBuilder} to append the divider to.
     * @param part
     *         The message part that will follow after the divider. This is used to extract the
     *         part's name.
     * @param prependDivider
     *         {@code true}, if the divider should be appended. {@code false}, otherwise.
     */
    private void addHtmlDivider(StringBuilder html, Part part, boolean prependDivider) {
        if (prependDivider) {
            String filename = getPartName(part);

            html.append("<p style=\"margin-top: 2.5em; margin-bottom: 1em; border-bottom: 1px solid #000\">");
            html.append(filename);
            html.append("</p>");
        }
    }

    /**
     * Get the name of the message part.
     *
     * @param part
     *         The part to get the name for.
     *
     * @return The (file)name of the part if available. An empty string, otherwise.
     */
    private static String getPartName(Part part) {
        String disposition = part.getDisposition();
        if (disposition != null) {
            String name = getHeaderParameter(disposition, "filename");
            return (name == null) ? "" : name;
        }

        return "";
    }

    /**
     * Add a plain text divider between two plain text message parts.
     *
     * @param text
     *         The {@link StringBuilder} to append the divider to.
     * @param part
     *         The message part that will follow after the divider. This is used to extract the
     *         part's name.
     * @param prependDivider
     *         {@code true}, if the divider should be appended. {@code false}, otherwise.
     */
    private void addTextDivider(StringBuilder text, Part part, boolean prependDivider) {
        if (prependDivider) {
            String filename = getPartName(part);

            text.append("\r\n\r\n");
            int len = filename.length();
            if (len > 0) {
                if (len > TEXT_DIVIDER_LENGTH - FILENAME_PREFIX_LENGTH - FILENAME_SUFFIX_LENGTH) {
                    filename = filename.substring(0, TEXT_DIVIDER_LENGTH - FILENAME_PREFIX_LENGTH -
                            FILENAME_SUFFIX_LENGTH - 3) + "...";
                }
                text.append(FILENAME_PREFIX);
                text.append(filename);
                text.append(FILENAME_SUFFIX);
                text.append(TEXT_DIVIDER.substring(0, TEXT_DIVIDER_LENGTH -
                        FILENAME_PREFIX_LENGTH - filename.length() - FILENAME_SUFFIX_LENGTH));
            } else {
                text.append(TEXT_DIVIDER);
            }
            text.append("\r\n\r\n");
        }
    }

    /**
     * Extract important header values from a message to display inline (plain text version).
     *
     * @param text
     *         The {@link StringBuilder} that will receive the (plain text) output.
     * @param message
     *         The message to extract the header values from.
     *
     * @throws com.fsck.k9.mail.MessagingException
     *          In case of an error.
     */
    private void addMessageHeaderText(StringBuilder text, Message message)
            throws MessagingException {
        // From: <sender>
        Address[] from = message.getFrom();
        if (from != null && from.length > 0) {
            text.append(context.getString(R.string.message_compose_quote_header_from));
            text.append(' ');
            text.append(Address.toString(from));
            text.append("\r\n");
        }

        // To: <recipients>
        Address[] to = message.getRecipients(Message.RecipientType.TO);
        if (to != null && to.length > 0) {
            text.append(context.getString(R.string.message_compose_quote_header_to));
            text.append(' ');
            text.append(Address.toString(to));
            text.append("\r\n");
        }

        // Cc: <recipients>
        Address[] cc = message.getRecipients(Message.RecipientType.CC);
        if (cc != null && cc.length > 0) {
            text.append(context.getString(R.string.message_compose_quote_header_cc));
            text.append(' ');
            text.append(Address.toString(cc));
            text.append("\r\n");
        }

        // Date: <date>
        Date date = message.getSentDate();
        if (date != null) {
            text.append(context.getString(R.string.message_compose_quote_header_send_date));
            text.append(' ');
            text.append(date.toString());
            text.append("\r\n");
        }

        // Subject: <subject>
        String subject = message.getSubject();
        text.append(context.getString(R.string.message_compose_quote_header_subject));
        text.append(' ');
        if (subject == null) {
            text.append(context.getString(R.string.general_no_subject));
        } else {
            text.append(subject);
        }
        text.append("\r\n\r\n");
    }

    /**
     * Extract important header values from a message to display inline (HTML version).
     *
     * @param html
     *         The {@link StringBuilder} that will receive the (HTML) output.
     * @param message
     *         The message to extract the header values from.
     *
     * @throws com.fsck.k9.mail.MessagingException
     *          In case of an error.
     */
    private void addMessageHeaderHtml(StringBuilder html, Message message)
            throws MessagingException {

        html.append("<table style=\"border: 0\">");

        // From: <sender>
        Address[] from = message.getFrom();
        if (from != null && from.length > 0) {
            addTableRow(html, context.getString(R.string.message_compose_quote_header_from),
                    Address.toString(from));
        }

        // To: <recipients>
        Address[] to = message.getRecipients(Message.RecipientType.TO);
        if (to != null && to.length > 0) {
            addTableRow(html, context.getString(R.string.message_compose_quote_header_to),
                    Address.toString(to));
        }

        // Cc: <recipients>
        Address[] cc = message.getRecipients(Message.RecipientType.CC);
        if (cc != null && cc.length > 0) {
            addTableRow(html, context.getString(R.string.message_compose_quote_header_cc),
                    Address.toString(cc));
        }

        // Date: <date>
        Date date = message.getSentDate();
        if (date != null) {
            addTableRow(html, context.getString(R.string.message_compose_quote_header_send_date),
                    date.toString());
        }

        // Subject: <subject>
        String subject = message.getSubject();
        addTableRow(html, context.getString(R.string.message_compose_quote_header_subject),
                (subject == null) ? context.getString(R.string.general_no_subject) : subject);

        html.append("</table>");
    }

    /**
     * Output an HTML table two column row with some hardcoded style.
     *
     * @param html
     *         The {@link StringBuilder} that will receive the output.
     * @param header
     *         The string to be put in the {@code TH} element.
     * @param value
     *         The string to be put in the {@code TD} element.
     */
    private static void addTableRow(StringBuilder html, String header, String value) {
        html.append("<tr><th style=\"text-align: left; vertical-align: top;\">");
        html.append(header);
        html.append("</th>");
        html.append("<td>");
        html.append(value);
        html.append("</td></tr>");
    }

    @VisibleForTesting
    static class ViewableExtractedText {
        public final String text;
        public final String html;

        public ViewableExtractedText(String text, String html) {
            this.text = text;
            this.html = html;
        }
    }
}
