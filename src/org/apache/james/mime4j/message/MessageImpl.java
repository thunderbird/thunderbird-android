/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mime4j.message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.MimeIOException;
import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.SingleBody;
import org.apache.james.mime4j.dom.address.Address;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.field.AddressListField;
import org.apache.james.mime4j.dom.field.ContentDispositionField;
import org.apache.james.mime4j.dom.field.ContentTransferEncodingField;
import org.apache.james.mime4j.dom.field.ContentTypeField;
import org.apache.james.mime4j.dom.field.DateTimeField;
import org.apache.james.mime4j.dom.field.FieldName;
import org.apache.james.mime4j.dom.field.MailboxField;
import org.apache.james.mime4j.dom.field.MailboxListField;
import org.apache.james.mime4j.dom.field.UnstructuredField;
import org.apache.james.mime4j.field.ContentTransferEncodingFieldImpl;
import org.apache.james.mime4j.field.ContentTypeFieldImpl;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.storage.DefaultStorageProvider;
import org.apache.james.mime4j.storage.StorageProvider;
import org.apache.james.mime4j.stream.MimeEntityConfig;
import org.apache.james.mime4j.stream.MutableBodyDescriptorFactory;
import org.apache.james.mime4j.util.MimeUtil;

/**
 * Represents a MIME message. The following code parses a stream into a
 * <code>Message</code> object.
 *
 * <pre>
 * Message msg = new Message(new FileInputStream(&quot;mime.msg&quot;));
 * </pre>
 */
public class MessageImpl extends Message {

    /**
     * Creates a new empty <code>Message</code>.
     */
    public MessageImpl() {
    }

    /**
     * Creates a new <code>Message</code> from the specified
     * <code>Message</code>. The <code>Message</code> instance is
     * initialized with copies of header and body of the specified
     * <code>Message</code>. The parent entity of the new message is
     * <code>null</code>.
     *
     * @param other
     *            message to copy.
     * @throws UnsupportedOperationException
     *             if <code>other</code> contains a {@link SingleBody} that
     *             does not support the {@link SingleBody#copy() copy()}
     *             operation.
     * @throws IllegalArgumentException
     *             if <code>other</code> contains a <code>Body</code> that
     *             is neither a {@link MessageImpl}, {@link Multipart} or
     *             {@link SingleBody}.
     */
    public MessageImpl(Message other) {
        if (other.getHeader() != null) {
            setHeader(new Header(other.getHeader()));
        }

        if (other.getBody() != null) {
            Body bodyCopy = BodyCopier.copy(other.getBody());
            setBody(bodyCopy);
        }
    }

    /**
     * Parses the specified MIME message stream into a <code>Message</code>
     * instance.
     *
     * @param is
     *            the stream to parse.
     * @throws IOException
     *             on I/O errors.
     * @throws MimeIOException
     *             on MIME protocol violations.
     */
    public MessageImpl(InputStream is) throws IOException, MimeIOException {
        this(is, null, DefaultStorageProvider.getInstance());
    }

    /**
     * Parses the specified MIME message stream into a <code>Message</code>
     * instance using given {@link MimeEntityConfig}.
     *
     * @param is
     *            the stream to parse.
     * @throws IOException
     *             on I/O errors.
     * @throws MimeIOException
     *             on MIME protocol violations.
     */
    public MessageImpl(InputStream is, MimeEntityConfig config) throws IOException,
            MimeIOException {
        this(is, config, DefaultStorageProvider.getInstance());
    }

    /**
     * Parses the specified MIME message stream into a <code>Message</code>
     * instance using given {@link MimeEntityConfig} and {@link StorageProvider}.
     *
     * @param is
     *            the stream to parse.
     * @param config
     *            {@link MimeEntityConfig} to use.
     * @param storageProvider
     *            {@link StorageProvider} to use for storing text and binary
     *            message bodies.
     * @param bodyDescFactory
     *            {@link MutableBodyDescriptorFactory} to use for creating body descriptors.
     * @throws IOException
     *             on I/O errors.
     * @throws MimeIOException
     *             on MIME protocol violations.
     */
    public MessageImpl(
            final InputStream is,
            final MimeEntityConfig config,
            final StorageProvider storageProvider,
            final MutableBodyDescriptorFactory bodyDescFactory,
            final DecodeMonitor monitor) throws IOException, MimeIOException {
        this(is, config, storageProvider, bodyDescFactory, monitor, true, false);
    }

    /**
     * Parses the specified MIME message stream into a <code>Message</code>
     * instance using given {@link MimeEntityConfig} and {@link StorageProvider}.
     *
     * @param is
     *            the stream to parse.
     * @param config
     *            {@link MimeEntityConfig} to use.
     * @param storageProvider
     *            {@link StorageProvider} to use for storing text and binary
     *            message bodies.
     * @param bodyDescFactory
     *            {@link MutableBodyDescriptorFactory} to use for creating body descriptors.
     * @throws IOException
     *             on I/O errors.
     * @throws MimeIOException
     *             on MIME protocol violations.
     */
    public MessageImpl(
            final InputStream is,
            final MimeEntityConfig config,
            final StorageProvider storageProvider,
            final MutableBodyDescriptorFactory bodyDescFactory,
            final DecodeMonitor monitor,
            boolean contentDecoding,
            boolean flatMode) throws IOException, MimeIOException {
        try {
            DecodeMonitor mon = monitor != null ? monitor : DecodeMonitor.SILENT;
            MimeStreamParser parser = new MimeStreamParser(config, bodyDescFactory, mon);
            parser.setContentHandler(new EntityBuilder(this, storageProvider, mon));
            parser.setContentDecoding(contentDecoding);
            if (flatMode) parser.setFlat(true);
            parser.parse(is);
        } catch (MimeException e) {
            throw new MimeIOException(e);
        }
    }

    public MessageImpl(
            final InputStream is,
            final MimeEntityConfig config,
            final StorageProvider storageProvider,
            final MutableBodyDescriptorFactory bodyDescFactory) throws IOException, MimeIOException {
        this(is, config, storageProvider, bodyDescFactory, null);
    }

    public MessageImpl(
            final InputStream is,
            final MimeEntityConfig config,
            final StorageProvider storageProvider) throws IOException, MimeIOException {
        this(is, config, storageProvider, null, null);
    }

    /**
     * @see org.apache.james.mime4j.dom.Message#writeTo(java.io.OutputStream)
     */
    public void writeTo(OutputStream out) throws IOException {
        MessageWriter.DEFAULT.writeEntity(this, out);
    }

    @Override
    protected String newUniqueBoundary() {
        return MimeUtil.createUniqueBoundary();
    }

    protected UnstructuredField newMessageId(String hostname) {
        return Fields.messageId(hostname);
    }

    protected DateTimeField newDate(Date date, TimeZone zone) {
        return Fields.date(FieldName.DATE, date, zone);
    }

    protected MailboxField newMailbox(String fieldName, Mailbox mailbox) {
        return Fields.mailbox(fieldName, mailbox);
    }

    protected MailboxListField newMailboxList(String fieldName,
            Collection<Mailbox> mailboxes) {
        return Fields.mailboxList(fieldName, mailboxes);
    }

    protected AddressListField newAddressList(String fieldName,
            Collection<Address> addresses) {
        return Fields.addressList(fieldName, addresses);
    }

    protected UnstructuredField newSubject(String subject) {
        return Fields.subject(subject);
    }

    protected ContentDispositionField newContentDisposition(
            String dispositionType, String filename, long size,
            Date creationDate, Date modificationDate, Date readDate) {
        return Fields.contentDisposition(dispositionType, filename, size,
                creationDate, modificationDate, readDate);
    }

    protected ContentDispositionField newContentDisposition(
            String dispositionType, Map<String, String> parameters) {
        return Fields.contentDisposition(dispositionType, parameters);
    }

    protected ContentTypeField newContentType(String mimeType,
            Map<String, String> parameters) {
        return Fields.contentType(mimeType, parameters);
    }

    protected ContentTransferEncodingField newContentTransferEncoding(
            String contentTransferEncoding) {
        return Fields.contentTransferEncoding(contentTransferEncoding);
    }

    protected String calcTransferEncoding(ContentTransferEncodingField f) {
        return ContentTransferEncodingFieldImpl.getEncoding(f);
    }

    protected String calcMimeType(ContentTypeField child, ContentTypeField parent) {
        return ContentTypeFieldImpl.getMimeType(child, parent);
    }

    protected String calcCharset(ContentTypeField contentType) {
        return ContentTypeFieldImpl.getCharset(contentType);
    }

}
