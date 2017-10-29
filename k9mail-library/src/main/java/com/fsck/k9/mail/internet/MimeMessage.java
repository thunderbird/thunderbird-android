
package com.fsck.k9.mail.internet;


import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import android.support.annotation.NonNull;

import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyFactory;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.DefaultBodyFactory;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.field.DateTimeField;
import org.apache.james.mime4j.field.DefaultFieldParser;
import org.apache.james.mime4j.io.EOLConvertingInputStream;
import org.apache.james.mime4j.parser.ContentHandler;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.stream.BodyDescriptor;
import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.stream.MimeConfig;
import timber.log.Timber;


/**
 * An implementation of Message that stores all of it's metadata in RFC 822 and
 * RFC 2045 style headers.
 */
public class MimeMessage extends Message {
    private MimeHeader header = new MimeHeader();
    protected Address[] from;
    protected Address[] to;
    protected Address[] cc;
    protected Address[] bcc;
    protected Address[] replyTo;
    private Address[] originalTo;
    private Address[] deliveredTo;
    private Address[] envelopeTo;

    protected String messageId;
    private String[] references;
    private String[] inReplyTo;

    private Date sentDate;
    private SimpleDateFormat dateFormat;

    private Body body;
    protected int size;
    private String serverExtra;


    public static MimeMessage parseMimeMessage(InputStream in, boolean recurse) throws IOException, MessagingException {
        MimeMessage mimeMessage = new MimeMessage();
        mimeMessage.parse(in, recurse);
        return mimeMessage;
    }

    public MimeMessage() {
    }

    /**
     * Parse the given InputStream using Apache Mime4J to build a MimeMessage.
     * Does not recurse through nested bodyparts.
     */
    public final void parse(InputStream in) throws IOException, MessagingException {
        parse(in, false);
    }

    private void parse(InputStream in, boolean recurse) throws IOException, MessagingException {
        header.clear();
        from = null;
        to = null;
        cc = null;
        bcc = null;
        replyTo = null;
        originalTo = null;
        deliveredTo = null;
        envelopeTo = null;

        messageId = null;
        references = null;
        inReplyTo = null;

        sentDate = null;

        body = null;

        MimeConfig parserConfig = new MimeConfig.Builder()
                // The default is a mere 10k
                .setMaxHeaderLen(-1)
                // The default is 1000 characters. Some MUAs generate REALLY long References: headers
                .setMaxLineLen(-1)
                // Disable the check for header count.
                .setMaxHeaderCount(-1)
                .build();

        MimeStreamParser parser = new MimeStreamParser(parserConfig);
        parser.setContentHandler(new MimeMessageBuilder(new DefaultBodyFactory()));
        if (recurse) {
            parser.setRecurse();
        }
        try {
            parser.parse(new EOLConvertingInputStream(in));
        } catch (MimeException me) {
            throw new MessagingException(me.getMessage(), me);
        }
    }

    @Override
    public Date getSentDate() {
        if (sentDate == null) {
            try {
                DateTimeField field = (DateTimeField)DefaultFieldParser.parse("Date: "
                                      + MimeUtility.unfoldAndDecode(getFirstHeader("Date")));
                sentDate = field.getDate();
            } catch (Exception e) {
                Timber.e(e);
            }
        }
        return sentDate;
    }

    /**
     * Sets the sent date object member as well as *adds* the 'Date' header
     * instead of setting it (for performance reasons).
     *
     * @see #sentDate
     * @param sentDate time message was sent, with precision of seconds
     */
    public void addSentDate(Date sentDate, boolean hideTimeZone) {
        if (dateFormat == null) {
            dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
        }

        if (hideTimeZone) {
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        addHeader("Date", dateFormat.format(sentDate));
        setInternalSentDate(sentDate);
    }

    @Override
    public void setSentDate(Date sentDate, boolean hideTimeZone) {
        removeHeader("Date");
        addSentDate(sentDate, hideTimeZone);
    }

    protected void setInternalSentDate(Date sentDate) {
        this.sentDate = sentDate;
    }

    @Override
    public String getContentType() {
        String contentType = getFirstHeader(MimeHeader.HEADER_CONTENT_TYPE);
        return (contentType == null) ? "text/plain" : MimeUtility.unfoldAndDecode(contentType);
    }

    @Override
    public String getDisposition() {
        return MimeUtility.unfoldAndDecode(getFirstHeader(MimeHeader.HEADER_CONTENT_DISPOSITION));
    }

    @Override
    public String getContentId() {
        return null;
    }

    @Override
    public String getMimeType() {
        return MimeUtility.getHeaderParameter(getContentType(), null);
    }

    @Override
    public boolean isMimeType(String mimeType) {
        return getMimeType().equalsIgnoreCase(mimeType);
    }

    @Override
    public long getSize() {
        return size;
    }

    /**
     * Returns a list of the given recipient type from this message. If no addresses are
     * found the method returns an empty array.
     */
    @Override
    public Address[] getRecipients(RecipientType type) {
        switch (type) {
            case TO: {
                if (to == null) {
                    to = Address.parse(MimeUtility.unfold(getFirstHeader("To")));
                }
                return to;
            }
            case CC: {
                if (cc == null) {
                    cc = Address.parse(MimeUtility.unfold(getFirstHeader("CC")));
                }
                return cc;
            }
            case BCC: {
                if (bcc == null) {
                    bcc = Address.parse(MimeUtility.unfold(getFirstHeader("BCC")));
                }
                return bcc;
            }
            case X_ORIGINAL_TO: {
                if (originalTo == null) {
                    originalTo = Address.parse(MimeUtility.unfold(getFirstHeader("X-Original-To")));
                }
                return originalTo;
            }
            case DELIVERED_TO: {
                if (deliveredTo == null) {
                    deliveredTo = Address.parse(MimeUtility.unfold(getFirstHeader("Delivered-To")));
                }
                return deliveredTo;
            }
            case X_ENVELOPE_TO: {
                if (envelopeTo == null) {
                    envelopeTo = Address.parse(MimeUtility.unfold(getFirstHeader("X-Envelope-To")));
                }
                return envelopeTo;
            }
        }

        throw new IllegalArgumentException("Unrecognized recipient type.");
    }

    @Override
    public void setRecipients(RecipientType type, Address[] addresses) {
        if (type == RecipientType.TO) {
            if (addresses == null || addresses.length == 0) {
                removeHeader("To");
                this.to = null;
            } else {
                setHeader("To", Address.toEncodedString(addresses));
                this.to = addresses;
            }
        } else if (type == RecipientType.CC) {
            if (addresses == null || addresses.length == 0) {
                removeHeader("CC");
                this.cc = null;
            } else {
                setHeader("CC", Address.toEncodedString(addresses));
                this.cc = addresses;
            }
        } else if (type == RecipientType.BCC) {
            if (addresses == null || addresses.length == 0) {
                removeHeader("BCC");
                this.bcc = null;
            } else {
                setHeader("BCC", Address.toEncodedString(addresses));
                this.bcc = addresses;
            }
        } else if (type == RecipientType.X_ORIGINAL_TO) {
            if (addresses == null || addresses.length == 0) {
                removeHeader("X-Original-To");
                this.originalTo = null;
            } else {
                setHeader("X-Original-To", Address.toEncodedString(addresses));
                this.originalTo = addresses;
            }
        } else if (type == RecipientType.DELIVERED_TO) {
            if (addresses == null || addresses.length == 0) {
                removeHeader("Delivered-To");
                this.deliveredTo = null;
            } else {
                setHeader("Delivered-To", Address.toEncodedString(addresses));
                this.deliveredTo = addresses;
            }
        } else if (type == RecipientType.X_ENVELOPE_TO) {
            if (addresses == null || addresses.length == 0) {
                removeHeader("X-Envelope-To");
                this.envelopeTo = null;
            } else {
                setHeader("X-Envelope-To", Address.toEncodedString(addresses));
                this.envelopeTo = addresses;
            }
        } else {
            throw new IllegalStateException("Unrecognized recipient type.");
        }
    }

    /**
     * Returns the unfolded, decoded value of the Subject header.
     */
    @Override
    public String getSubject() {
        return MimeUtility.unfoldAndDecode(getFirstHeader("Subject"), this);
    }

    @Override
    public void setSubject(String subject) {
        setHeader("Subject", subject);
    }

    @Override
    public Address[] getFrom() {
        if (from == null) {
            String list = MimeUtility.unfold(getFirstHeader("From"));
            if (list == null || list.length() == 0) {
                list = MimeUtility.unfold(getFirstHeader("Sender"));
            }
            from = Address.parse(list);
        }
        return from;
    }

    @Override
    public void setFrom(Address from) {
        if (from != null) {
            setHeader("From", from.toEncodedString());
            this.from = new Address[] {
                from
            };
        } else {
            this.from = null;
        }
    }

    @Override
    public Address[] getSender() {
        return Address.parse(MimeUtility.unfold(getFirstHeader("Sender")));
    }

    @Override
    public void setSender(Address sender) {
        if (sender != null) {
            setHeader("Sender", sender.toEncodedString());
        }
    }

    @Override
    public Address[] getReplyTo() {
        if (replyTo == null) {
            replyTo = Address.parse(MimeUtility.unfold(getFirstHeader("Reply-to")));
        }
        return replyTo;
    }

    @Override
    public void setReplyTo(Address[] replyTo) {
        if (replyTo == null || replyTo.length == 0) {
            removeHeader("Reply-to");
            this.replyTo = null;
        } else {
            setHeader("Reply-to", Address.toEncodedString(replyTo));
            this.replyTo = replyTo;
        }
    }

    @Override
    public String getMessageId() {
        if (messageId == null) {
            messageId = getFirstHeader("Message-ID");
        }
        return messageId;
    }

    public void setMessageId(String messageId) {
        setHeader("Message-ID", messageId);
        this.messageId = messageId;
    }

    @Override
    public void setInReplyTo(String inReplyTo) {
        setHeader("In-Reply-To", inReplyTo);
    }

    @Override
    public String[] getReferences() {
        if (references == null) {
            references = getHeader("References");
        }
        return references;
    }

    @Override
    public void setReferences(String references) {
        /*
         * Make sure the References header doesn't exceed the maximum header
         * line length and won't get (Q-)encoded later. Otherwise some clients
         * will break threads apart.
         *
         * For more information see issue 1559.
         */

        // Make sure separator is SPACE to prevent Q-encoding when TAB is encountered
        references = references.replaceAll("\\s+", " ");

        /*
         * NOTE: Usually the maximum header line is 998 + CRLF = 1000 characters.
         * But at least one implementations seems to have problems with 998
         * characters, so we adjust for that fact.
         */
        final int limit = 1000 - 2 /* CRLF */ - 12 /* "References: " */ - 1 /* Off-by-one bugs */;
        final int originalLength = references.length();
        if (originalLength >= limit) {
            // Find start of first reference
            final int start = references.indexOf('<');

            // First reference + SPACE
            final String firstReference = references.substring(start,
                                          references.indexOf('<', start + 1));

            // Find longest tail
            final String tail = references.substring(references.indexOf('<',
                                firstReference.length() + originalLength - limit));

            references = firstReference + tail;
        }
        setHeader("References", references);
    }

    @Override
    public Body getBody() {
        return body;
    }

    @Override
    public void setBody(Body body) {
        this.body = body;
    }

    private String getFirstHeader(String name) {
        return header.getFirstHeader(name);
    }

    @Override
    public void addHeader(String name, String value) {
        header.addHeader(name, value);
    }

    @Override
    public void addRawHeader(String name, String raw) {
        header.addRawHeader(name, raw);
    }

    @Override
    public void setHeader(String name, String value) {
        header.setHeader(name, value);
    }

    @NonNull
    @Override
    public String[] getHeader(String name) {
        return header.getHeader(name);
    }

    @Override
    public void removeHeader(String name) {
        header.removeHeader(name);
    }

    @Override
    public Set<String> getHeaderNames() {
        return header.getHeaderNames();
    }

    @Override
    public void writeTo(OutputStream out) throws IOException, MessagingException {

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out), 1024);
        header.writeTo(out);
        writer.write("\r\n");
        writer.flush();
        if (body != null) {
            body.writeTo(out);
        }
    }

    @Override
    public void writeHeaderTo(OutputStream out) throws IOException, MessagingException {
        header.writeTo(out);
    }

    @Override
    @NonNull
    public InputStream getInputStream() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setEncoding(String encoding) throws MessagingException {
        if (body != null) {
            body.setEncoding(encoding);
        }
        setHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING, encoding);
    }

    @Override
    public void setCharset(String charset) throws MessagingException {
        header.setCharset(charset);
        if (body instanceof Multipart) {
            ((Multipart) body).setCharset(charset);
        } else if (body instanceof TextBody) {
            CharsetSupport.setCharset(charset, this);
            ((TextBody) body).setCharset(charset);
        }
    }

    private class MimeMessageBuilder implements ContentHandler {
        private final LinkedList<Object> stack = new LinkedList<>();
        private final BodyFactory bodyFactory;

        MimeMessageBuilder(BodyFactory bodyFactory) {
            this.bodyFactory = bodyFactory;
        }

        private void expect(Class<?> c) {
            if (!c.isInstance(stack.peek())) {
                throw new IllegalStateException("Internal stack error: " + "Expected '"
                                                + c.getName() + "' found '" + stack.peek().getClass().getName() + "'");
            }
        }

        @Override
        public void startMessage() {
            if (stack.isEmpty()) {
                stack.addFirst(MimeMessage.this);
            } else {
                expect(Part.class);
                Part part = (Part) stack.peek();

                MimeMessage m = new MimeMessage();
                part.setBody(m);
                stack.addFirst(m);
            }
        }

        @Override
        public void endMessage() {
            expect(MimeMessage.class);
            stack.removeFirst();
        }

        @Override
        public void startHeader() {
            expect(Part.class);
        }

        @Override
        public void endHeader() {
            expect(Part.class);
        }

        @Override
        public void startMultipart(BodyDescriptor bd) throws MimeException {
            expect(Part.class);

            Part e = (Part)stack.peek();
            String mimeType = bd.getMimeType();
            String boundary = bd.getBoundary();
            MimeMultipart multiPart = new MimeMultipart(mimeType, boundary);
            e.setBody(multiPart);
            stack.addFirst(multiPart);
        }

        @Override
        public void body(BodyDescriptor bd, InputStream in) throws IOException, MimeException {
            expect(Part.class);
            Body body = bodyFactory.createBody(bd.getTransferEncoding(), bd.getMimeType(), in);
            ((Part)stack.peek()).setBody(body);
        }

        @Override
        public void endMultipart() {
            expect(Multipart.class);
            Multipart multipart = (Multipart) stack.removeFirst();

            boolean hasNoBodyParts = multipart.getCount() == 0;
            boolean hasNoEpilogue = multipart.getEpilogue() == null;
            if (hasNoBodyParts && hasNoEpilogue) {
                /*
                 * The parser is calling startMultipart(), preamble(), and endMultipart() when all we have is
                 * headers of a "multipart/*" part. But there's really no point in keeping a Multipart body if all
                 * of the content is missing.
                 */
                expect(Part.class);
                Part part = (Part) stack.peek();
                part.setBody(null);
            }
        }

        @Override
        public void startBodyPart() throws MimeException {
            expect(MimeMultipart.class);

            try {
                MimeBodyPart bodyPart = new MimeBodyPart();
                ((MimeMultipart)stack.peek()).addBodyPart(bodyPart);
                stack.addFirst(bodyPart);
            } catch (MessagingException me) {
                throw new MimeException(me);
            }
        }

        @Override
        public void endBodyPart() {
            expect(BodyPart.class);
            stack.removeFirst();
        }

        @Override
        public void preamble(InputStream is) throws IOException {
            expect(MimeMultipart.class);
            ByteArrayOutputStream preamble = new ByteArrayOutputStream();
            IOUtils.copy(is, preamble);
            ((MimeMultipart)stack.peek()).setPreamble(preamble.toByteArray());
        }

        @Override
        public void epilogue(InputStream is) throws IOException {
            expect(MimeMultipart.class);
            ByteArrayOutputStream epilogue = new ByteArrayOutputStream();
            IOUtils.copy(is, epilogue);
            ((MimeMultipart) stack.peek()).setEpilogue(epilogue.toByteArray());
        }

        @Override
        public void raw(InputStream is) throws IOException {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void field(Field parsedField) throws MimeException {
            expect(Part.class);
            String name = parsedField.getName();
            String raw = parsedField.getRaw().toString();
            ((Part) stack.peek()).addRawHeader(name, raw);
        }
    }

    /**
     * Copy the contents of this object into another {@code MimeMessage} object.
     *
     * @param destination The {@code MimeMessage} object to receive the contents of this instance.
     */
    protected void copy(MimeMessage destination) {
        super.copy(destination);

        destination.header = header.clone();

        destination.body = body;
        destination.messageId = messageId;
        destination.sentDate = sentDate;
        destination.dateFormat = dateFormat;
        destination.size = size;

        // These arrays are not supposed to be modified, so it's okay to reuse the references
        destination.from = from;
        destination.to = to;
        destination.cc = cc;
        destination.bcc = bcc;
        destination.replyTo = replyTo;
        destination.references = references;
        destination.inReplyTo = inReplyTo;
        destination.originalTo = originalTo;
        destination.deliveredTo = deliveredTo;
        destination.envelopeTo = envelopeTo;
    }

    @Override
    public MimeMessage clone() {
        MimeMessage message = new MimeMessage();
        copy(message);
        return message;
    }

    @Override
    public boolean hasAttachments() {
        return false;
    }

    @Override
    public String getServerExtra() {
        return serverExtra;
    }

    @Override
    public void setServerExtra(String serverExtra) {
        this.serverExtra = serverExtra;
    }

    /**
     * Convert a top level message into a bodypart.
     * Returned body part shouldn't contain inappropriate headers such as smtp
     * headers or MIME-VERSION.
     * Both Message and MimeBodyPart might share structures.
     * @return the body part
     * @throws MessagingException if message is improperly encoded
     */
    @NonNull
    public MimeBodyPart toBodyPart() throws MessagingException {
        MimeHeader contentHeaders = new MimeHeader();
        for (String header : header.getHeaderNames()) {
            if (header.toLowerCase().startsWith("content-")) {
                for (String value : this.header.getHeader(header)) {
                    contentHeaders.addHeader(header, value);
                }
            }
        }
        return new MimeBodyPart(contentHeaders, getBody());
    }
}
