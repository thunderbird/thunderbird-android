package com.fsck.k9.mailstore;

import android.content.Context;
import android.net.Uri;

import com.fsck.k9.R;
import com.fsck.k9.crypto.DecryptedTempFileBody;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import com.fsck.k9.helper.HtmlConverter;
import com.fsck.k9.mail.internet.MessageExtractor;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.internet.Viewable;
import com.fsck.k9.mailstore.MessageViewInfo.MessageViewContainer;
import com.fsck.k9.provider.AttachmentProvider;
import com.fsck.k9.provider.K9FileProvider;
import com.fsck.k9.ui.crypto.MessageCryptoAnnotations;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.fsck.k9.mail.internet.MimeUtility.getHeaderParameter;
import static com.fsck.k9.mail.internet.Viewable.Alternative;
import static com.fsck.k9.mail.internet.Viewable.Html;
import static com.fsck.k9.mail.internet.Viewable.MessageHeader;
import static com.fsck.k9.mail.internet.Viewable.Text;
import static com.fsck.k9.mail.internet.Viewable.Textual;

public class LocalMessageExtractor {
    private static final String TEXT_DIVIDER =
            "------------------------------------------------------------------------";
    private static final int TEXT_DIVIDER_LENGTH = TEXT_DIVIDER.length();
    private static final String FILENAME_PREFIX = "----- ";
    private static final int FILENAME_PREFIX_LENGTH = FILENAME_PREFIX.length();
    private static final String FILENAME_SUFFIX = " ";
    private static final int FILENAME_SUFFIX_LENGTH = FILENAME_SUFFIX.length();
    private static final OpenPgpResultAnnotation NO_ANNOTATIONS = null;

    private LocalMessageExtractor() {}
    /**
     * Extract the viewable textual parts of a message and return the rest as attachments.
     *
     * @param context A {@link android.content.Context} instance that will be used to get localized strings.
     * @param viewables
     * @param attachments
     * @return A {@link ViewableContainer} instance containing the textual parts of the message as
     *         plain text and HTML, and a list of message parts considered attachments.
     *
     * @throws com.fsck.k9.mail.MessagingException
     *          In case of an error.
     */
    public static ViewableContainer extractTextAndAttachments(Context context, List<Viewable> viewables,
            List<Part> attachments) throws MessagingException {
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
                    addMessageHeaderText(context, text, innerMessage);

                    addHtmlDivider(html, containerPart, !hideDivider);
                    addMessageHeaderHtml(context, html, innerMessage);

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

            return new ViewableContainer(text.toString(), html.toString(), attachments);
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
    private static StringBuilder buildHtml(Viewable viewable, boolean prependDivider)
    {
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

    private static StringBuilder buildText(Viewable viewable, boolean prependDivider)
    {
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
    private static void addHtmlDivider(StringBuilder html, Part part, boolean prependDivider) {
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
        try {
            String disposition = part.getDisposition();
            if (disposition != null) {
                String name = getHeaderParameter(disposition, "filename");
                return (name == null) ? "" : name;
            }
        }
        catch (MessagingException e) { /* ignore */ }

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
    private static void addTextDivider(StringBuilder text, Part part, boolean prependDivider) {
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
     * @param context
     *         A {@link android.content.Context} instance that will be used to get localized strings.
     * @param text
     *         The {@link StringBuilder} that will receive the (plain text) output.
     * @param message
     *         The message to extract the header values from.
     *
     * @throws com.fsck.k9.mail.MessagingException
     *          In case of an error.
     */
    private static void addMessageHeaderText(Context context, StringBuilder text, Message message)
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
     * @param context
     *         A {@link android.content.Context} instance that will be used to get localized strings.
     * @param html
     *         The {@link StringBuilder} that will receive the (HTML) output.
     * @param message
     *         The message to extract the header values from.
     *
     * @throws com.fsck.k9.mail.MessagingException
     *          In case of an error.
     */
    private static void addMessageHeaderHtml(Context context, StringBuilder html, Message message)
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

    public static MessageViewInfo decodeMessageForView(Context context,
            Message message, MessageCryptoAnnotations annotations) throws MessagingException {

        // 1. break mime structure on encryption/signature boundaries
        List<Part> parts = getCryptPieces(message, annotations);

        // 2. extract viewables/attachments of parts
        ArrayList<MessageViewContainer> containers = new ArrayList<MessageViewContainer>();
        for (Part part : parts) {
            OpenPgpResultAnnotation pgpAnnotation = annotations.get(part);

            // TODO properly handle decrypted data part - this just replaces the part
            if (pgpAnnotation != NO_ANNOTATIONS && pgpAnnotation.hasOutputData()) {
                part = pgpAnnotation.getOutputData();
            }

            ArrayList<Part> attachments = new ArrayList<Part>();
            List<Viewable> viewables = MessageExtractor.getViewables(part, attachments);

            // 3. parse viewables into html string
            ViewableContainer viewable = LocalMessageExtractor.extractTextAndAttachments(context, viewables,
                    attachments);
            List<AttachmentViewInfo> attachmentInfos = extractAttachmentInfos(context, attachments);

            MessageViewContainer messageViewContainer =
                    new MessageViewContainer(viewable.html, part, attachmentInfos, pgpAnnotation);

            containers.add(messageViewContainer);
        }

        return new MessageViewInfo(containers, message);
    }

    public static List<Part> getCryptPieces(Message message, MessageCryptoAnnotations annotations) throws MessagingException {

        // TODO make sure this method does what it is supposed to
        /* This method returns a list of mime parts which are to be parsed into
         * individual MessageViewContainers for display, which each have their
         * own crypto header. This means parts should be individual for each
         * multipart/encrypted, multipart/signed, or a multipart/* which does
         * not contain children of the former types.
         */


        ArrayList<Part> parts = new ArrayList<Part>();
        if (!getCryptSubPieces(message, parts, annotations)) {
            parts.add(message);
        }

        return parts;
    }

    public static boolean getCryptSubPieces(Part part, ArrayList<Part> parts,
            MessageCryptoAnnotations annotations) throws MessagingException {

        Body body = part.getBody();
        if (body instanceof Multipart) {
            Multipart multi = (Multipart) body;
            if (MimeUtility.isSameMimeType(part.getMimeType(), "multipart/mixed")) {
                boolean foundSome = false;
                for (BodyPart sub : multi.getBodyParts()) {
                    foundSome |= getCryptSubPieces(sub, parts, annotations);
                }
                if (!foundSome) {
                    parts.add(part);
                    return true;
                }
            } else if (annotations.has(part)) {
                parts.add(part);
                return true;
            }
        }
        return false;
    }

    private static List<AttachmentViewInfo> extractAttachmentInfos(Context context, List<Part> attachmentParts)
            throws MessagingException {

        List<AttachmentViewInfo> attachments = new ArrayList<AttachmentViewInfo>();
        for (Part part : attachmentParts) {
            attachments.add(extractAttachmentInfo(context, part));
        }

        return attachments;
    }

    public static AttachmentViewInfo extractAttachmentInfo(Context context, Part part) throws MessagingException {
        if (part instanceof LocalPart) {
            LocalPart localPart = (LocalPart) part;
            String accountUuid = localPart.getAccountUuid();
            long messagePartId = localPart.getId();
            String mimeType = part.getMimeType();
            String displayName = localPart.getDisplayName();
            long size = localPart.getSize();
            boolean firstClassAttachment = localPart.isFirstClassAttachment();
            Uri uri = AttachmentProvider.getAttachmentUri(accountUuid, messagePartId);

            return new AttachmentViewInfo(mimeType, displayName, size, uri, firstClassAttachment, part);
        } else {
            Body body = part.getBody();
            if (body instanceof DecryptedTempFileBody) {
                DecryptedTempFileBody decryptedTempFileBody = (DecryptedTempFileBody) body;
                File file = decryptedTempFileBody.getFile();
                Uri uri = K9FileProvider.getUriForFile(context, file, part.getMimeType());
                long size = file.length();
                return extractAttachmentInfo(part, uri, size);
            } else {
                throw new RuntimeException("Not supported");
            }
        }
    }

    public static AttachmentViewInfo extractAttachmentInfo(Part part) throws MessagingException {
        return extractAttachmentInfo(part, Uri.EMPTY, AttachmentViewInfo.UNKNOWN_SIZE);
    }

    private static AttachmentViewInfo extractAttachmentInfo(Part part, Uri uri, long size) throws MessagingException {
        boolean firstClassAttachment = true;

        String mimeType = part.getMimeType();
        String contentTypeHeader = MimeUtility.unfoldAndDecode(part.getContentType());
        String contentDisposition = MimeUtility.unfoldAndDecode(part.getDisposition());

        String name = MimeUtility.getHeaderParameter(contentDisposition, "filename");
        if (name == null) {
            name = MimeUtility.getHeaderParameter(contentTypeHeader, "name");
        }

        if (name == null) {
            firstClassAttachment = false;
            String extension = MimeUtility.getExtensionByMimeType(mimeType);
            name = "noname" + ((extension != null) ? "." + extension : "");
        }

        // Inline parts with a content-id are almost certainly components of an HTML message
        // not attachments. Only show them if the user pressed the button to show more
        // attachments.
        if (contentDisposition != null &&
                MimeUtility.getHeaderParameter(contentDisposition, null).matches("^(?i:inline)") &&
                part.getHeader(MimeHeader.HEADER_CONTENT_ID).length > 0) {
            firstClassAttachment = false;
        }

        long attachmentSize = extractAttachmentSize(contentDisposition, size);

        return new AttachmentViewInfo(mimeType, name, attachmentSize, uri, firstClassAttachment, part);
    }

    private static long extractAttachmentSize(String contentDisposition, long size) {
        if (size != AttachmentViewInfo.UNKNOWN_SIZE) {
            return size;
        }

        long result = AttachmentViewInfo.UNKNOWN_SIZE;
        String sizeParam = MimeUtility.getHeaderParameter(contentDisposition, "size");
        if (sizeParam != null) {
            try {
                result = Integer.parseInt(sizeParam);
            } catch (NumberFormatException e) { /* ignore */ }
        }

        return result;
    }
}
