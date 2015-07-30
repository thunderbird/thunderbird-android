
package com.fsck.k9.mail.internet;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

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
import org.apache.james.mime4j.util.MimeUtil;

import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.BodyPart;
import com.fsck.k9.mail.CompositeBody;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;

/**
 * An implementation of Message that stores all of it's metadata in RFC 822 and
 * RFC 2045 style headers.
 */
public class MimeMessage extends Message {
    private MimeHeader mHeader = new MimeHeader();
    protected Address[] mFrom;
    protected Address[] mTo;
    protected Address[] mCc;
    protected Address[] mBcc;
    protected Address[] mReplyTo;

    protected String mMessageId;
    private String[] mReferences;
    private String[] mInReplyTo;

    private Date mSentDate;
    private SimpleDateFormat mDateFormat;

    private Body mBody;
    protected int mSize;
    private String serverExtra;

    public MimeMessage() {
    }


    /**
     * Parse the given InputStream using Apache Mime4J to build a MimeMessage.
     *
     * @param in
     * @param recurse A boolean indicating to recurse through all nested MimeMessage subparts.
     * @throws IOException
     * @throws MessagingException
     */
    public MimeMessage(InputStream in, boolean recurse) throws IOException, MessagingException {
        parse(in, recurse);
    }

    /**
     * Parse the given InputStream using Apache Mime4J to build a MimeMessage.
     * Does not recurse through nested bodyparts.
     */
    public final void parse(InputStream in) throws IOException, MessagingException {
        parse(in, false);
    }

    private void parse(InputStream in, boolean recurse) throws IOException, MessagingException {
        mHeader.clear();
        mFrom = null;
        mTo = null;
        mCc = null;
        mBcc = null;
        mReplyTo = null;

        mMessageId = null;
        mReferences = null;
        mInReplyTo = null;

        mSentDate = null;

        mBody = null;

        MimeConfig parserConfig  = new MimeConfig();
        parserConfig.setMaxHeaderLen(-1); // The default is a mere 10k
        parserConfig.setMaxLineLen(-1); // The default is 1000 characters. Some MUAs generate
        // REALLY long References: headers
        parserConfig.setMaxHeaderCount(-1); // Disable the check for header count.
        MimeStreamParser parser = new MimeStreamParser(parserConfig);
        parser.setContentHandler(new MimeMessageBuilder());
        if (recurse) {
            parser.setRecurse();
        }
        try {
            parser.parse(new EOLConvertingInputStream(in));
        } catch (MimeException me) {
            //TODO wouldn't a MessagingException be better?
            throw new Error(me);
        }
    }

    @Override
    public Date getSentDate() {
        if (mSentDate == null) {
            try {
                DateTimeField field = (DateTimeField)DefaultFieldParser.parse("Date: "
                                      + MimeUtility.unfoldAndDecode(getFirstHeader("Date")));
                mSentDate = field.getDate();
            } catch (Exception e) {

            }
        }
        return mSentDate;
    }

    /**
     * Sets the sent date object member as well as *adds* the 'Date' header
     * instead of setting it (for performance reasons).
     *
     * @see #mSentDate
     * @param sentDate
     * @throws com.fsck.k9.mail.MessagingException
     */
    public void addSentDate(Date sentDate, boolean hideTimeZone) throws MessagingException {
        if (mDateFormat == null) {
            mDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
        }

        if (hideTimeZone) {
            mDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        addHeader("Date", mDateFormat.format(sentDate));
        setInternalSentDate(sentDate);
    }

    @Override
    public void setSentDate(Date sentDate, boolean hideTimeZone) throws MessagingException {
        removeHeader("Date");
        addSentDate(sentDate, hideTimeZone);
    }

    public void setInternalSentDate(Date sentDate) {
        this.mSentDate = sentDate;
    }

    @Override
    public String getContentType() {
        String contentType = getFirstHeader(MimeHeader.HEADER_CONTENT_TYPE);
        return (contentType == null) ? "text/plain" : contentType;
    }

    @Override
    public String getDisposition() throws MessagingException {
        return getFirstHeader(MimeHeader.HEADER_CONTENT_DISPOSITION);
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
    public boolean isMimeType(String mimeType) throws MessagingException {
        return getMimeType().equalsIgnoreCase(mimeType);
    }

    @Override
    public int getSize() {
        return mSize;
    }

    /**
     * Returns a list of the given recipient type from this message. If no addresses are
     * found the method returns an empty array.
     */
    @Override
    public Address[] getRecipients(RecipientType type) throws MessagingException {
        if (type == RecipientType.TO) {
            if (mTo == null) {
                mTo = Address.parse(MimeUtility.unfold(getFirstHeader("To")));
            }
            return mTo;
        } else if (type == RecipientType.CC) {
            if (mCc == null) {
                mCc = Address.parse(MimeUtility.unfold(getFirstHeader("CC")));
            }
            return mCc;
        } else if (type == RecipientType.BCC) {
            if (mBcc == null) {
                mBcc = Address.parse(MimeUtility.unfold(getFirstHeader("BCC")));
            }
            return mBcc;
        } else {
            throw new MessagingException("Unrecognized recipient type.");
        }
    }

    @Override
    public void setRecipients(RecipientType type, Address[] addresses) throws MessagingException {
        if (type == RecipientType.TO) {
            if (addresses == null || addresses.length == 0) {
                removeHeader("To");
                this.mTo = null;
            } else {
                setHeader("To", Address.toEncodedString(addresses));
                this.mTo = addresses;
            }
        } else if (type == RecipientType.CC) {
            if (addresses == null || addresses.length == 0) {
                removeHeader("CC");
                this.mCc = null;
            } else {
                setHeader("CC", Address.toEncodedString(addresses));
                this.mCc = addresses;
            }
        } else if (type == RecipientType.BCC) {
            if (addresses == null || addresses.length == 0) {
                removeHeader("BCC");
                this.mBcc = null;
            } else {
                setHeader("BCC", Address.toEncodedString(addresses));
                this.mBcc = addresses;
            }
        } else {
            throw new MessagingException("Unrecognized recipient type.");
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
    public void setSubject(String subject) throws MessagingException {
        setHeader("Subject", subject);
    }

    @Override
    public Address[] getFrom() {
        if (mFrom == null) {
            String list = MimeUtility.unfold(getFirstHeader("From"));
            if (list == null || list.length() == 0) {
                list = MimeUtility.unfold(getFirstHeader("Sender"));
            }
            mFrom = Address.parse(list);
        }
        return mFrom;
    }

    @Override
    public void setFrom(Address from) throws MessagingException {
        if (from != null) {
            setHeader("From", from.toEncodedString());
            this.mFrom = new Address[] {
                from
            };
        } else {
            this.mFrom = null;
        }
    }

    @Override
    public Address[] getReplyTo() {
        if (mReplyTo == null) {
            mReplyTo = Address.parse(MimeUtility.unfold(getFirstHeader("Reply-to")));
        }
        return mReplyTo;
    }

    @Override
    public void setReplyTo(Address[] replyTo) throws MessagingException {
        if (replyTo == null || replyTo.length == 0) {
            removeHeader("Reply-to");
            mReplyTo = null;
        } else {
            setHeader("Reply-to", Address.toEncodedString(replyTo));
            mReplyTo = replyTo;
        }
    }

    @Override
    public String getMessageId() throws MessagingException {
        if (mMessageId == null) {
            mMessageId = getFirstHeader("Message-ID");
        }
        return mMessageId;
    }

    public void generateMessageId() throws MessagingException {
        String hostname = null;

        if (mFrom != null && mFrom.length >= 1) {
            hostname = mFrom[0].getHostname();
        }

        if (hostname == null && mReplyTo != null && mReplyTo.length >= 1) {
            hostname = mReplyTo[0].getHostname();
        }

        if (hostname == null) {
            hostname = "email.android.com";
        }

        /* We use upper case here to match Apple Mail Message-ID format (for privacy) */
        String messageId = "<" + UUID.randomUUID().toString().toUpperCase(Locale.US) + "@" + hostname + ">";

        setMessageId(messageId);
    }

    public void setMessageId(String messageId) throws MessagingException {
        setHeader("Message-ID", messageId);
        mMessageId = messageId;
    }

    @Override
    public void setInReplyTo(String inReplyTo) throws MessagingException {
        setHeader("In-Reply-To", inReplyTo);
    }

    @Override
    public String[] getReferences() throws MessagingException {
        if (mReferences == null) {
            mReferences = getHeader("References");
        }
        return mReferences;
    }

    @Override
    public void setReferences(String references) throws MessagingException {
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
        return mBody;
    }

    @Override
    public void setBody(Body body) {
        this.mBody = body;
    }

    private String getFirstHeader(String name) {
        return mHeader.getFirstHeader(name);
    }

    @Override
    public void addHeader(String name, String value) throws MessagingException {
        mHeader.addHeader(name, value);
    }

    @Override
    public void addRawHeader(String name, String raw) {
        mHeader.addRawHeader(name, raw);
    }

    @Override
    public void setHeader(String name, String value) throws MessagingException {
        mHeader.setHeader(name, value);
    }

    @Override
    public String[] getHeader(String name) throws MessagingException {
        return mHeader.getHeader(name);
    }

    @Override
    public void removeHeader(String name) throws MessagingException {
        mHeader.removeHeader(name);
    }

    @Override
    public Set<String> getHeaderNames() throws MessagingException {
        return mHeader.getHeaderNames();
    }

    @Override
    public void writeTo(OutputStream out) throws IOException, MessagingException {

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out), 1024);
        mHeader.writeTo(out);
        writer.write("\r\n");
        writer.flush();
        if (mBody != null) {
            mBody.writeTo(out);
        }
    }

    @Override
    public void writeHeaderTo(OutputStream out) throws IOException, MessagingException {
        mHeader.writeTo(out);
    }

    @Override
    public InputStream getInputStream() throws MessagingException {
        return null;
    }

    @Override
    public void setEncoding(String encoding) throws MessagingException {
        if (mBody != null) {
            mBody.setEncoding(encoding);
        }
        setHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING, encoding);
    }

    @Override
    public void setCharset(String charset) throws MessagingException {
        mHeader.setCharset(charset);
        if (mBody instanceof Multipart) {
            ((Multipart)mBody).setCharset(charset);
        } else if (mBody instanceof TextBody) {
            CharsetSupport.setCharset(charset, this);
            ((TextBody)mBody).setCharset(charset);
        }
    }

    private class MimeMessageBuilder implements ContentHandler {
        private final LinkedList<Object> stack = new LinkedList<Object>();

        public MimeMessageBuilder() {
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
        public void startMultipart(BodyDescriptor bd) {
            expect(Part.class);

            Part e = (Part)stack.peek();
            try {
                String contentType = e.getContentType();
                String mimeType = MimeUtility.getHeaderParameter(contentType, null);
                String boundary = MimeUtility.getHeaderParameter(contentType, "boundary");
                MimeMultipart multiPart = new MimeMultipart(mimeType, boundary);
                e.setBody(multiPart);
                stack.addFirst(multiPart);
            } catch (MessagingException me) {
                throw new Error(me);
            }
        }

        @Override
        public void body(BodyDescriptor bd, InputStream in) throws IOException {
            expect(Part.class);
            try {
                Body body = MimeUtility.createBody(in, bd.getTransferEncoding(), bd.getMimeType());
                ((Part)stack.peek()).setBody(body);
            } catch (MessagingException me) {
                throw new Error(me);
            }
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
        public void startBodyPart() {
            expect(MimeMultipart.class);

            try {
                MimeBodyPart bodyPart = new MimeBodyPart();
                ((MimeMultipart)stack.peek()).addBodyPart(bodyPart);
                stack.addFirst(bodyPart);
            } catch (MessagingException me) {
                throw new Error(me);
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
            try {
                String name = parsedField.getName();
                String raw = parsedField.getRaw().toString();
                ((Part) stack.peek()).addRawHeader(name, raw);
            } catch (MessagingException me) {
                throw new Error(me);
            }
        }
    }

    /**
     * Copy the contents of this object into another {@code MimeMessage} object.
     *
     * @param destination The {@code MimeMessage} object to receive the contents of this instance.
     */
    protected void copy(MimeMessage destination) {
        super.copy(destination);

        destination.mHeader = mHeader.clone();

        destination.mBody = mBody;
        destination.mMessageId = mMessageId;
        destination.mSentDate = mSentDate;
        destination.mDateFormat = mDateFormat;
        destination.mSize = mSize;

        // These arrays are not supposed to be modified, so it's okay to reuse the references
        destination.mFrom = mFrom;
        destination.mTo = mTo;
        destination.mCc = mCc;
        destination.mBcc = mBcc;
        destination.mReplyTo = mReplyTo;
        destination.mReferences = mReferences;
        destination.mInReplyTo = mInReplyTo;
    }

    @Override
    public MimeMessage clone() {
        MimeMessage message = new MimeMessage();
        copy(message);
        return message;
    }

    @Override
    public long getId() {
        return Long.parseLong(mUid); //or maybe .mMessageId?
    }

    @Override
    public String getPreview() {
        return "";
    }

    @Override
    public boolean hasAttachments() {
        return false;
    }


    @Override
    public void setUsing7bitTransport() throws MessagingException {
        String type = getFirstHeader(MimeHeader.HEADER_CONTENT_TYPE);
        /*
         * We don't trust that a multipart/* will properly have an 8bit encoding
         * header if any of its subparts are 8bit, so we automatically recurse
         * (as long as its not multipart/signed).
         */
        if (mBody instanceof CompositeBody
                && !"multipart/signed".equalsIgnoreCase(type)) {
            setEncoding(MimeUtil.ENC_7BIT);
            // recurse
            ((CompositeBody) mBody).setUsing7bitTransport();
        } else if (!MimeUtil.ENC_8BIT
                .equalsIgnoreCase(getFirstHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING))) {
            return;
        } else if (type != null
                && (type.equalsIgnoreCase("multipart/signed") || type
                        .toLowerCase(Locale.US).startsWith("message/"))) {
            /*
             * This shouldn't happen. In any case, it would be wrong to convert
             * them to some other encoding for 7bit transport.
             *
             * RFC 1847 says multipart/signed must be 7bit. It also says their
             * bodies must be treated as opaque, so we must not change the
             * encoding.
             *
             * We've dealt with (CompositeBody) type message/rfc822 above. Here
             * we must deal with all other message/* types. RFC 2045 says
             * message/* can only be 7bit or 8bit. RFC 2046 says unknown
             * message/* types must be treated as application/octet-stream,
             * which means we can't recurse into them. It also says that
             * existing subtypes message/partial and message/external must only
             * be 7bit, and that future subtypes "should be" 7bit.
             */
            throw new MessagingException(
                    "Unable to convert 8bit body part to 7bit");
        } else {
            setEncoding(MimeUtil.ENC_QUOTED_PRINTABLE);
        }
    }

    @Override
    public String getServerExtra() {
        return serverExtra;
    }

    @Override
    public void setServerExtra(String serverExtra) {
        this.serverExtra = serverExtra;
    }


}
