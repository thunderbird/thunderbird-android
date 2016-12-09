package com.fsck.k9.mail.internet;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import org.apache.commons.io.input.BoundedInputStream;

import static com.fsck.k9.mail.K9MailLib.LOG_TAG;
import static com.fsck.k9.mail.internet.CharsetSupport.fixupCharset;
import static com.fsck.k9.mail.internet.MimeUtility.getHeaderParameter;
import static com.fsck.k9.mail.internet.MimeUtility.isSameMimeType;
import static com.fsck.k9.mail.internet.Viewable.Alternative;
import static com.fsck.k9.mail.internet.Viewable.Html;
import static com.fsck.k9.mail.internet.Viewable.MessageHeader;
import static com.fsck.k9.mail.internet.Viewable.Text;
import static com.fsck.k9.mail.internet.Viewable.Textual;

public class MessageExtractor {
    public static final long NO_TEXT_SIZE_LIMIT = -1L;


    private MessageExtractor() {}

    public static String getTextFromPart(Part part) {
        return getTextFromPart(part, NO_TEXT_SIZE_LIMIT);
    }

    public static String getTextFromPart(Part part, long textSizeLimit) {
        try {
            if ((part != null) && (part.getBody() != null)) {
                final Body body = part.getBody();
                if (body instanceof TextBody) {
                    return ((TextBody) body).getRawText();
                }
                final String mimeType = part.getMimeType();
                if (mimeType != null && MimeUtility.mimeTypeMatches(mimeType, "text/*") ||
                        part.isMimeType("application/pgp")) {
                    return getTextFromTextPart(part, body, mimeType, textSizeLimit);
                } else {
                    throw new MessagingException("Provided non-text part: " + mimeType);
                }
            } else {
                throw new MessagingException("Provided invalid part");
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Unable to getTextFromPart", e);
        } catch (MessagingException e) {
            Log.e(LOG_TAG, "Unable to getTextFromPart", e);
        }
        return null;
    }

    private static String getTextFromTextPart(Part part, Body body, String mimeType, long textSizeLimit)
            throws IOException, MessagingException {
        /*
         * We've got a text part, so let's see if it needs to be processed further.
         */
        String charset = getHeaderParameter(part.getContentType(), "charset");
        /*
         * determine the charset from HTML message.
         */
        if (isSameMimeType(mimeType, "text/html") && charset == null) {
            InputStream in = MimeUtility.decodeBody(body);
            try {
                byte[] buf = new byte[256];
                in.read(buf, 0, buf.length);
                String str = new String(buf, "US-ASCII");

                if (str.isEmpty()) {
                    return "";
                }
                Pattern p = Pattern.compile("<meta http-equiv=\"?Content-Type\"? content=\"text/html; charset=(.+?)\">", Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(str);
                if (m.find()) {
                    charset = m.group(1);
                }
            } finally {
                try {
                    MimeUtility.closeInputStreamWithoutDeletingTemporaryFiles(in);
                } catch (IOException e) { /* ignore */ }
            }
        }
        charset = fixupCharset(charset, getMessageFromPart(part));
        /*
         * Now we read the part into a buffer for further processing. Because
         * the stream is now wrapped we'll remove any transfer encoding at this point.
         */
        InputStream in = MimeUtility.decodeBody(body);
        InputStream possiblyLimitedIn =
                textSizeLimit != NO_TEXT_SIZE_LIMIT ? new BoundedInputStream(in, textSizeLimit) : in;
        try {
            return CharsetSupport.readToString(possiblyLimitedIn, charset);
        } finally {
            try {
                MimeUtility.closeInputStreamWithoutDeletingTemporaryFiles(in);
            } catch (IOException e) { /* Ignore */ }
        }
    }

    public static boolean hasMissingParts(Part part) {
        Body body = part.getBody();
        if (body == null) {
            return true;
        }
        if (body instanceof Multipart) {
            Multipart multipart = (Multipart) body;
            for (Part subPart : multipart.getBodyParts()) {
                if (hasMissingParts(subPart)) {
                    return true;
                }
            }
        }
        return false;
    }


    /** Traverse the MIME tree of a message an extract viewable parts. */
    public static void findViewablesAndAttachments(Part part,
                @Nullable List<Viewable> outputViewableParts, @Nullable List<Part> outputNonViewableParts)
            throws MessagingException {
        boolean skipSavingNonViewableParts = outputNonViewableParts == null;
        boolean skipSavingViewableParts = outputViewableParts == null;
        if (skipSavingNonViewableParts && skipSavingViewableParts) {
            throw new IllegalArgumentException("method was called but no output is to be collected - this a bug!");
        }

        Body body = part.getBody();
        if (body instanceof Multipart) {
            Multipart multipart = (Multipart) body;
            if (isSameMimeType(part.getMimeType(), "multipart/alternative")) {
                /*
                 * For multipart/alternative parts we try to find a text/plain and a text/html
                 * child. Everything else we find is put into 'attachments'.
                 */
                List<Viewable> text = findTextPart(multipart, true);

                Set<Part> knownTextParts = getParts(text);
                List<Viewable> html = findHtmlPart(multipart, knownTextParts, outputNonViewableParts, true);

                if (skipSavingViewableParts) {
                    return;
                }
                if (!text.isEmpty() || !html.isEmpty()) {
                    Alternative alternative = new Alternative(text, html);
                    outputViewableParts.add(alternative);
                }
            } else {
                // For all other multipart parts we recurse to grab all viewable children.
                for (Part bodyPart : multipart.getBodyParts()) {
                    findViewablesAndAttachments(bodyPart, outputViewableParts, outputNonViewableParts);
                }
            }
        } else if (body instanceof Message &&
                !("attachment".equalsIgnoreCase(getContentDisposition(part)))) {
            if (skipSavingViewableParts) {
                return;
            }
            /*
             * We only care about message/rfc822 parts whose Content-Disposition header has a value
             * other than "attachment".
             */
            Message message = (Message) body;

            // We add the Message object so we can extract the filename later.
            outputViewableParts.add(new MessageHeader(part, message));

            // Recurse to grab all viewable parts and attachments from that message.
            findViewablesAndAttachments(message, outputViewableParts, outputNonViewableParts);
        } else if (isPartTextualBody(part)) {
            if (skipSavingViewableParts) {
                return;
            }
            String mimeType = part.getMimeType();
            if (isSameMimeType(mimeType, "text/plain")) {
                Text text = new Text(part);
                outputViewableParts.add(text);
            } else {
                Html html = new Html(part);
                outputViewableParts.add(html);
            }
        } else if (isSameMimeType(part.getMimeType(), "application/pgp-signature")) {
            // ignore this type explicitly
        } else {
            if (skipSavingNonViewableParts) {
                return;
            }
            // Everything else is treated as attachment.
            outputNonViewableParts.add(part);
        }
    }

    public static Set<Part> getTextParts(Part part) throws MessagingException {
        List<Viewable> viewableParts = new ArrayList<>();
        List<Part> nonViewableParts = new ArrayList<>();
        findViewablesAndAttachments(part, viewableParts, nonViewableParts);
        return getParts(viewableParts);
    }

    /**
     * Collect attachment parts of a message.
     * @return A list of parts regarded as attachments.
     * @throws MessagingException In case of an error.
     */
    public static List<Part> collectAttachments(Message message) throws MessagingException {
        try {
            List<Part> attachments = new ArrayList<>();
            findViewablesAndAttachments(message, new ArrayList<Viewable>(), attachments);
            return attachments;
        } catch (Exception e) {
            throw new MessagingException("Couldn't collect attachment parts", e);
        }
    }

    /**
     * Collect the viewable textual parts of a message.
     * @return A set of viewable parts of the message.
     * @throws MessagingException In case of an error.
     */
    public static Set<Part> collectTextParts(Message message) throws MessagingException {
        try {
            return getTextParts(message);
        } catch (Exception e) {
            throw new MessagingException("Couldn't extract viewable parts", e);
        }
    }

    private static Message getMessageFromPart(Part part) {
        while (part != null) {
            if (part instanceof Message)
                return (Message)part;

            if (!(part instanceof BodyPart))
                return null;

            Multipart multipart = ((BodyPart)part).getParent();
            if (multipart == null)
                return null;

            part = multipart.getParent();
        }
        return null;
    }

    /**
     * Search the children of a {@link Multipart} for {@code text/plain} parts.
     *
     * @param multipart The {@code Multipart} to search through.
     * @param directChild If {@code true}, this method will return after the first {@code text/plain} was
     *         found.
     *
     * @return A list of {@link Text} viewables.
     *
     * @throws MessagingException
     *          In case of an error.
     */
    private static List<Viewable> findTextPart(Multipart multipart, boolean directChild)
            throws MessagingException {
        List<Viewable> viewables = new ArrayList<Viewable>();

        for (Part part : multipart.getBodyParts()) {
            Body body = part.getBody();
            if (body instanceof Multipart) {
                Multipart innerMultipart = (Multipart) body;

                /*
                 * Recurse to find text parts. Since this is a multipart that is a child of a
                 * multipart/alternative we don't want to stop after the first text/plain part
                 * we find. This will allow to get all text parts for constructions like this:
                 *
                 * 1. multipart/alternative
                 * 1.1. multipart/mixed
                 * 1.1.1. text/plain
                 * 1.1.2. text/plain
                 * 1.2. text/html
                 */
                List<Viewable> textViewables = findTextPart(innerMultipart, false);

                if (!textViewables.isEmpty()) {
                    viewables.addAll(textViewables);
                    if (directChild) {
                        break;
                    }
                }
            } else if (isPartTextualBody(part) && isSameMimeType(part.getMimeType(), "text/plain")) {
                Text text = new Text(part);
                viewables.add(text);
                if (directChild) {
                    break;
                }
            }
        }
        return viewables;
    }

    /**
     * Search the children of a {@link Multipart} for {@code text/html} parts.
     * Every part that is not a {@code text/html} we want to display, we add to 'attachments'.
     *
     * @param multipart The {@code Multipart} to search through.
     * @param knownTextParts A set of {@code text/plain} parts that shouldn't be added to 'attachments'.
     * @param outputNonViewableParts A list that will receive the parts that are considered attachments.
     * @param directChild If {@code true}, this method will add all {@code text/html} parts except the first
     *         found to 'attachments'.
     *
     * @return A list of {@link Text} viewables.
     *
     * @throws MessagingException In case of an error.
     */
    private static List<Viewable> findHtmlPart(Multipart multipart, Set<Part> knownTextParts,
            @Nullable List<Part> outputNonViewableParts, boolean directChild) throws MessagingException {
        boolean saveNonViewableParts = outputNonViewableParts != null;
        List<Viewable> viewables = new ArrayList<>();

        boolean partFound = false;
        for (Part part : multipart.getBodyParts()) {
            Body body = part.getBody();
            if (body instanceof Multipart) {
                Multipart innerMultipart = (Multipart) body;

                if (directChild && partFound) {
                    if (saveNonViewableParts) {
                        // We already found our text/html part. Now we're only looking for attachments.
                        findAttachments(innerMultipart, knownTextParts, outputNonViewableParts);
                    }
                } else {
                    /*
                     * Recurse to find HTML parts. Since this is a multipart that is a child of a
                     * multipart/alternative we don't want to stop after the first text/html part
                     * we find. This will allow to get all text parts for constructions like this:
                     *
                     * 1. multipart/alternative
                     * 1.1. text/plain
                     * 1.2. multipart/mixed
                     * 1.2.1. text/html
                     * 1.2.2. text/html
                     * 1.3. image/jpeg
                     */
                    List<Viewable> htmlViewables = findHtmlPart(innerMultipart, knownTextParts,
                            outputNonViewableParts, false);

                    if (!htmlViewables.isEmpty()) {
                        partFound = true;
                        viewables.addAll(htmlViewables);
                    }
                }
            } else if (!(directChild && partFound) && isPartTextualBody(part) &&
                    isSameMimeType(part.getMimeType(), "text/html")) {
                Html html = new Html(part);
                viewables.add(html);
                partFound = true;
            } else if (!knownTextParts.contains(part)) {
                if (saveNonViewableParts) {
                    // Only add this part as attachment if it's not a viewable text/plain part found earlier
                    outputNonViewableParts.add(part);
                }
            }
        }

        return viewables;
    }

    /**
     * Traverse the MIME tree and add everything that's not a known text part to 'attachments'.
     *
     * @param multipart
     *         The {@link Multipart} to start from.
     * @param knownTextParts
     *         A set of known text parts we don't want to end up in 'attachments'.
     * @param attachments
     *         A list that will receive the parts that are considered attachments.
     */
    private static void findAttachments(Multipart multipart, Set<Part> knownTextParts,
            @NonNull List<Part> attachments) {
        for (Part part : multipart.getBodyParts()) {
            Body body = part.getBody();
            if (body instanceof Multipart) {
                Multipart innerMultipart = (Multipart) body;
                findAttachments(innerMultipart, knownTextParts, attachments);
            } else if (!knownTextParts.contains(part)) {
                attachments.add(part);
            }
        }
    }

    /**
     * Build a set of message parts for fast lookups.
     *
     * @param viewables
     *         A list of {@link Viewable}s containing references to the message parts to include in
     *         the set.
     *
     * @return The set of viewable {@code Part}s.
     *
     * @see MessageExtractor#findHtmlPart(Multipart, Set, List, boolean)
     * @see MessageExtractor#findAttachments(Multipart, Set, List)
     */
    private static Set<Part> getParts(List<Viewable> viewables) {
        Set<Part> parts = new HashSet<>();

        for (Viewable viewable : viewables) {
            if (viewable instanceof Textual) {
                parts.add(((Textual) viewable).getPart());
            } else if (viewable instanceof Alternative) {
                Alternative alternative = (Alternative) viewable;
                parts.addAll(getParts(alternative.getText()));
                parts.addAll(getParts(alternative.getHtml()));
            }
        }

        return parts;
    }

    private static Boolean isPartTextualBody(Part part) throws MessagingException {
        String disposition = part.getDisposition();
        String dispositionType = null;
        String dispositionFilename = null;
        if (disposition != null) {
            dispositionType = MimeUtility.getHeaderParameter(disposition, null);
            dispositionFilename = MimeUtility.getHeaderParameter(disposition, "filename");
        }

        boolean isAttachmentDisposition = "attachment".equalsIgnoreCase(dispositionType) || dispositionFilename != null;
        if (isAttachmentDisposition) {
            return false;
        }

        if (part.isMimeType("text/html")) {
            return true;
        }

        if (part.isMimeType("text/plain")) {
            return true;
        }

        if (part.isMimeType("application/pgp")) {
            return true;
        }

        return false;
    }

    private static String getContentDisposition(Part part) {
        String disposition = part.getDisposition();
        if (disposition != null) {
            return MimeUtility.getHeaderParameter(disposition, null);
        }
        return null;
    }
}
