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

package org.apache.james.mime4j.field;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.codec.EncoderUtil;
import org.apache.james.mime4j.dom.address.Address;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.field.AddressListField;
import org.apache.james.mime4j.dom.field.ContentDispositionField;
import org.apache.james.mime4j.dom.field.ContentTransferEncodingField;
import org.apache.james.mime4j.dom.field.ContentTypeField;
import org.apache.james.mime4j.dom.field.DateTimeField;
import org.apache.james.mime4j.dom.field.Field;
import org.apache.james.mime4j.dom.field.FieldName;
import org.apache.james.mime4j.dom.field.MailboxField;
import org.apache.james.mime4j.dom.field.MailboxListField;
import org.apache.james.mime4j.dom.field.ParsedField;
import org.apache.james.mime4j.dom.field.UnstructuredField;
import org.apache.james.mime4j.field.AddressListFieldImpl;
import org.apache.james.mime4j.field.ContentDispositionFieldImpl;
import org.apache.james.mime4j.field.ContentTransferEncodingFieldImpl;
import org.apache.james.mime4j.field.ContentTypeFieldImpl;
import org.apache.james.mime4j.field.DateTimeFieldImpl;
import org.apache.james.mime4j.field.MailboxFieldImpl;
import org.apache.james.mime4j.field.MailboxListFieldImpl;
import org.apache.james.mime4j.field.UnstructuredFieldImpl;
import org.apache.james.mime4j.field.address.formatter.AddressFormatter;
import org.apache.james.mime4j.stream.RawField;
import org.apache.james.mime4j.util.MimeUtil;

/**
 * Factory for concrete {@link Field} instances.
 */
public class Fields {

    private static final Pattern FIELD_NAME_PATTERN = Pattern
            .compile("[\\x21-\\x39\\x3b-\\x7e]+");

    private Fields() {
    }

    /**
     * Creates a <i>Content-Type</i> field from the specified raw field value.
     * The specified string gets folded into a multiple-line representation if
     * necessary but is otherwise taken as is.
     *
     * @param contentType
     *            raw content type containing a MIME type and optional
     *            parameters.
     * @return the newly created <i>Content-Type</i> field.
     */
    public static ContentTypeField contentType(String contentType) {
        return parse(ContentTypeFieldImpl.PARSER, FieldName.CONTENT_TYPE,
                contentType);
    }

    /**
     * Creates a <i>Content-Type</i> field from the specified MIME type and
     * parameters.
     *
     * @param mimeType
     *            a MIME type (such as <code>&quot;text/plain&quot;</code> or
     *            <code>&quot;application/octet-stream&quot;</code>).
     * @param parameters
     *            map containing content-type parameters such as
     *            <code>&quot;boundary&quot;</code>.
     * @return the newly created <i>Content-Type</i> field.
     */
    public static ContentTypeField contentType(String mimeType,
            Map<String, String> parameters) {
        if (!isValidMimeType(mimeType))
            throw new IllegalArgumentException();

        if (parameters == null || parameters.isEmpty()) {
            return parse(ContentTypeFieldImpl.PARSER, FieldName.CONTENT_TYPE,
                    mimeType);
        } else {
            StringBuilder sb = new StringBuilder(mimeType);
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                sb.append("; ");
                sb.append(EncoderUtil.encodeHeaderParameter(entry.getKey(),
                        entry.getValue()));
            }
            String contentType = sb.toString();
            return contentType(contentType);
        }
    }

    /**
     * Creates a <i>Content-Transfer-Encoding</i> field from the specified raw
     * field value.
     *
     * @param contentTransferEncoding
     *            an encoding mechanism such as <code>&quot;7-bit&quot;</code>
     *            or <code>&quot;quoted-printable&quot;</code>.
     * @return the newly created <i>Content-Transfer-Encoding</i> field.
     */
    public static ContentTransferEncodingField contentTransferEncoding(
            String contentTransferEncoding) {
        return parse(ContentTransferEncodingFieldImpl.PARSER,
                FieldName.CONTENT_TRANSFER_ENCODING, contentTransferEncoding);
    }

    /**
     * Creates a <i>Content-Disposition</i> field from the specified raw field
     * value. The specified string gets folded into a multiple-line
     * representation if necessary but is otherwise taken as is.
     *
     * @param contentDisposition
     *            raw content disposition containing a disposition type and
     *            optional parameters.
     * @return the newly created <i>Content-Disposition</i> field.
     */
    public static ContentDispositionField contentDisposition(
            String contentDisposition) {
        return parse(ContentDispositionFieldImpl.PARSER,
                FieldName.CONTENT_DISPOSITION, contentDisposition);
    }

    /**
     * Creates a <i>Content-Disposition</i> field from the specified
     * disposition type and parameters.
     *
     * @param dispositionType
     *            a disposition type (usually <code>&quot;inline&quot;</code>
     *            or <code>&quot;attachment&quot;</code>).
     * @param parameters
     *            map containing disposition parameters such as
     *            <code>&quot;filename&quot;</code>.
     * @return the newly created <i>Content-Disposition</i> field.
     */
    public static ContentDispositionField contentDisposition(
            String dispositionType, Map<String, String> parameters) {
        if (!isValidDispositionType(dispositionType))
            throw new IllegalArgumentException();

        if (parameters == null || parameters.isEmpty()) {
            return parse(ContentDispositionFieldImpl.PARSER,
                    FieldName.CONTENT_DISPOSITION, dispositionType);
        } else {
            StringBuilder sb = new StringBuilder(dispositionType);
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                sb.append("; ");
                sb.append(EncoderUtil.encodeHeaderParameter(entry.getKey(),
                        entry.getValue()));
            }
            String contentDisposition = sb.toString();
            return contentDisposition(contentDisposition);
        }
    }

    /**
     * Creates a <i>Content-Disposition</i> field from the specified
     * disposition type and filename.
     *
     * @param dispositionType
     *            a disposition type (usually <code>&quot;inline&quot;</code>
     *            or <code>&quot;attachment&quot;</code>).
     * @param filename
     *            filename parameter value or <code>null</code> if the
     *            parameter should not be included.
     * @return the newly created <i>Content-Disposition</i> field.
     */
    public static ContentDispositionField contentDisposition(
            String dispositionType, String filename) {
        return contentDisposition(dispositionType, filename, -1, null, null,
                null);
    }

    /**
     * Creates a <i>Content-Disposition</i> field from the specified values.
     *
     * @param dispositionType
     *            a disposition type (usually <code>&quot;inline&quot;</code>
     *            or <code>&quot;attachment&quot;</code>).
     * @param filename
     *            filename parameter value or <code>null</code> if the
     *            parameter should not be included.
     * @param size
     *            size parameter value or <code>-1</code> if the parameter
     *            should not be included.
     * @return the newly created <i>Content-Disposition</i> field.
     */
    public static ContentDispositionField contentDisposition(
            String dispositionType, String filename, long size) {
        return contentDisposition(dispositionType, filename, size, null, null,
                null);
    }

    /**
     * Creates a <i>Content-Disposition</i> field from the specified values.
     *
     * @param dispositionType
     *            a disposition type (usually <code>&quot;inline&quot;</code>
     *            or <code>&quot;attachment&quot;</code>).
     * @param filename
     *            filename parameter value or <code>null</code> if the
     *            parameter should not be included.
     * @param size
     *            size parameter value or <code>-1</code> if the parameter
     *            should not be included.
     * @param creationDate
     *            creation-date parameter value or <code>null</code> if the
     *            parameter should not be included.
     * @param modificationDate
     *            modification-date parameter value or <code>null</code> if
     *            the parameter should not be included.
     * @param readDate
     *            read-date parameter value or <code>null</code> if the
     *            parameter should not be included.
     * @return the newly created <i>Content-Disposition</i> field.
     */
    public static ContentDispositionField contentDisposition(
            String dispositionType, String filename, long size,
            Date creationDate, Date modificationDate, Date readDate) {
        Map<String, String> parameters = new HashMap<String, String>();
        if (filename != null) {
            parameters.put(ContentDispositionFieldImpl.PARAM_FILENAME, filename);
        }
        if (size >= 0) {
            parameters.put(ContentDispositionFieldImpl.PARAM_SIZE, Long
                    .toString(size));
        }
        if (creationDate != null) {
            parameters.put(ContentDispositionFieldImpl.PARAM_CREATION_DATE,
                    MimeUtil.formatDate(creationDate, null));
        }
        if (modificationDate != null) {
            parameters.put(ContentDispositionFieldImpl.PARAM_MODIFICATION_DATE,
                    MimeUtil.formatDate(modificationDate, null));
        }
        if (readDate != null) {
            parameters.put(ContentDispositionFieldImpl.PARAM_READ_DATE, MimeUtil
                    .formatDate(readDate, null));
        }
        return contentDisposition(dispositionType, parameters);
    }

    /**
     * Creates a <i>Date</i> field from the specified <code>Date</code>
     * value. The default time zone of the host is used to format the date.
     *
     * @param date
     *            date value for the header field.
     * @return the newly created <i>Date</i> field.
     */
    public static DateTimeField date(Date date) {
        return date0(FieldName.DATE, date, null);
    }

    /**
     * Creates a date field from the specified field name and <code>Date</code>
     * value. The default time zone of the host is used to format the date.
     *
     * @param fieldName
     *            a field name such as <code>Date</code> or
     *            <code>Resent-Date</code>.
     * @param date
     *            date value for the header field.
     * @return the newly created date field.
     */
    public static DateTimeField date(String fieldName, Date date) {
        checkValidFieldName(fieldName);
        return date0(fieldName, date, null);
    }

    /**
     * Creates a date field from the specified field name, <code>Date</code>
     * and <code>TimeZone</code> values.
     *
     * @param fieldName
     *            a field name such as <code>Date</code> or
     *            <code>Resent-Date</code>.
     * @param date
     *            date value for the header field.
     * @param zone
     *            the time zone to be used for formatting the date.
     * @return the newly created date field.
     */
    public static DateTimeField date(String fieldName, Date date, TimeZone zone) {
        checkValidFieldName(fieldName);
        return date0(fieldName, date, zone);
    }

    /**
     * Creates a <i>Message-ID</i> field for the specified host name.
     *
     * @param hostname
     *            host name to be included in the message ID or
     *            <code>null</code> if no host name should be included.
     * @return the newly created <i>Message-ID</i> field.
     */
    public static UnstructuredField messageId(String hostname) {
        String fieldValue = MimeUtil.createUniqueMessageId(hostname);
        return parse(UnstructuredFieldImpl.PARSER, FieldName.MESSAGE_ID, fieldValue);
    }

    /**
     * Creates a <i>Subject</i> field from the specified string value. The
     * specified string may contain non-ASCII characters.
     *
     * @param subject
     *            the subject string.
     * @return the newly created <i>Subject</i> field.
     */
    public static UnstructuredField subject(String subject) {
        int usedCharacters = FieldName.SUBJECT.length() + 2;
        String fieldValue = EncoderUtil.encodeIfNecessary(subject,
                EncoderUtil.Usage.TEXT_TOKEN, usedCharacters);

        return parse(UnstructuredFieldImpl.PARSER, FieldName.SUBJECT, fieldValue);
    }

    /**
     * Creates a <i>Sender</i> field for the specified mailbox address.
     *
     * @param mailbox
     *            address to be included in the field.
     * @return the newly created <i>Sender</i> field.
     */
    public static MailboxField sender(Mailbox mailbox) {
        return mailbox0(FieldName.SENDER, mailbox);
    }

    /**
     * Creates a <i>From</i> field for the specified mailbox address.
     *
     * @param mailbox
     *            address to be included in the field.
     * @return the newly created <i>From</i> field.
     */
    public static MailboxListField from(Mailbox mailbox) {
        return mailboxList0(FieldName.FROM, Collections.singleton(mailbox));
    }

    /**
     * Creates a <i>From</i> field for the specified mailbox addresses.
     *
     * @param mailboxes
     *            addresses to be included in the field.
     * @return the newly created <i>From</i> field.
     */
    public static MailboxListField from(Mailbox... mailboxes) {
        return mailboxList0(FieldName.FROM, Arrays.asList(mailboxes));
    }

    /**
     * Creates a <i>From</i> field for the specified mailbox addresses.
     *
     * @param mailboxes
     *            addresses to be included in the field.
     * @return the newly created <i>From</i> field.
     */
    public static MailboxListField from(Iterable<Mailbox> mailboxes) {
        return mailboxList0(FieldName.FROM, mailboxes);
    }

    /**
     * Creates a <i>To</i> field for the specified mailbox or group address.
     *
     * @param address
     *            mailbox or group address to be included in the field.
     * @return the newly created <i>To</i> field.
     */
    public static AddressListField to(Address address) {
        return addressList0(FieldName.TO, Collections.singleton(address));
    }

    /**
     * Creates a <i>To</i> field for the specified mailbox or group addresses.
     *
     * @param addresses
     *            mailbox or group addresses to be included in the field.
     * @return the newly created <i>To</i> field.
     */
    public static AddressListField to(Address... addresses) {
        return addressList0(FieldName.TO, Arrays.asList(addresses));
    }

    /**
     * Creates a <i>To</i> field for the specified mailbox or group addresses.
     *
     * @param addresses
     *            mailbox or group addresses to be included in the field.
     * @return the newly created <i>To</i> field.
     */
    public static AddressListField to(Iterable<Address> addresses) {
        return addressList0(FieldName.TO, addresses);
    }

    /**
     * Creates a <i>Cc</i> field for the specified mailbox or group address.
     *
     * @param address
     *            mailbox or group address to be included in the field.
     * @return the newly created <i>Cc</i> field.
     */
    public static AddressListField cc(Address address) {
        return addressList0(FieldName.CC, Collections.singleton(address));
    }

    /**
     * Creates a <i>Cc</i> field for the specified mailbox or group addresses.
     *
     * @param addresses
     *            mailbox or group addresses to be included in the field.
     * @return the newly created <i>Cc</i> field.
     */
    public static AddressListField cc(Address... addresses) {
        return addressList0(FieldName.CC, Arrays.asList(addresses));
    }

    /**
     * Creates a <i>Cc</i> field for the specified mailbox or group addresses.
     *
     * @param addresses
     *            mailbox or group addresses to be included in the field.
     * @return the newly created <i>Cc</i> field.
     */
    public static AddressListField cc(Iterable<Address> addresses) {
        return addressList0(FieldName.CC, addresses);
    }

    /**
     * Creates a <i>Bcc</i> field for the specified mailbox or group address.
     *
     * @param address
     *            mailbox or group address to be included in the field.
     * @return the newly created <i>Bcc</i> field.
     */
    public static AddressListField bcc(Address address) {
        return addressList0(FieldName.BCC, Collections.singleton(address));
    }

    /**
     * Creates a <i>Bcc</i> field for the specified mailbox or group addresses.
     *
     * @param addresses
     *            mailbox or group addresses to be included in the field.
     * @return the newly created <i>Bcc</i> field.
     */
    public static AddressListField bcc(Address... addresses) {
        return addressList0(FieldName.BCC, Arrays.asList(addresses));
    }

    /**
     * Creates a <i>Bcc</i> field for the specified mailbox or group addresses.
     *
     * @param addresses
     *            mailbox or group addresses to be included in the field.
     * @return the newly created <i>Bcc</i> field.
     */
    public static AddressListField bcc(Iterable<Address> addresses) {
        return addressList0(FieldName.BCC, addresses);
    }

    /**
     * Creates a <i>Reply-To</i> field for the specified mailbox or group
     * address.
     *
     * @param address
     *            mailbox or group address to be included in the field.
     * @return the newly created <i>Reply-To</i> field.
     */
    public static AddressListField replyTo(Address address) {
        return addressList0(FieldName.REPLY_TO, Collections.singleton(address));
    }

    /**
     * Creates a <i>Reply-To</i> field for the specified mailbox or group
     * addresses.
     *
     * @param addresses
     *            mailbox or group addresses to be included in the field.
     * @return the newly created <i>Reply-To</i> field.
     */
    public static AddressListField replyTo(Address... addresses) {
        return addressList0(FieldName.REPLY_TO, Arrays.asList(addresses));
    }

    /**
     * Creates a <i>Reply-To</i> field for the specified mailbox or group
     * addresses.
     *
     * @param addresses
     *            mailbox or group addresses to be included in the field.
     * @return the newly created <i>Reply-To</i> field.
     */
    public static AddressListField replyTo(Iterable<Address> addresses) {
        return addressList0(FieldName.REPLY_TO, addresses);
    }

    /**
     * Creates a mailbox field from the specified field name and mailbox
     * address. Valid field names are <code>Sender</code> and
     * <code>Resent-Sender</code>.
     *
     * @param fieldName
     *            the name of the mailbox field (<code>Sender</code> or
     *            <code>Resent-Sender</code>).
     * @param mailbox
     *            mailbox address for the field value.
     * @return the newly created mailbox field.
     */
    public static MailboxField mailbox(String fieldName, Mailbox mailbox) {
        checkValidFieldName(fieldName);
        return mailbox0(fieldName, mailbox);
    }

    /**
     * Creates a mailbox-list field from the specified field name and mailbox
     * addresses. Valid field names are <code>From</code> and
     * <code>Resent-From</code>.
     *
     * @param fieldName
     *            the name of the mailbox field (<code>From</code> or
     *            <code>Resent-From</code>).
     * @param mailboxes
     *            mailbox addresses for the field value.
     * @return the newly created mailbox-list field.
     */
    public static MailboxListField mailboxList(String fieldName,
            Iterable<Mailbox> mailboxes) {
        checkValidFieldName(fieldName);
        return mailboxList0(fieldName, mailboxes);
    }

    /**
     * Creates an address-list field from the specified field name and mailbox
     * or group addresses. Valid field names are <code>To</code>,
     * <code>Cc</code>, <code>Bcc</code>, <code>Reply-To</code>,
     * <code>Resent-To</code>, <code>Resent-Cc</code> and
     * <code>Resent-Bcc</code>.
     *
     * @param fieldName
     *            the name of the mailbox field (<code>To</code>,
     *            <code>Cc</code>, <code>Bcc</code>, <code>Reply-To</code>,
     *            <code>Resent-To</code>, <code>Resent-Cc</code> or
     *            <code>Resent-Bcc</code>).
     * @param addresses
     *            mailbox or group addresses for the field value.
     * @return the newly created address-list field.
     */
    public static AddressListField addressList(String fieldName,
            Iterable<Address> addresses) {
        checkValidFieldName(fieldName);
        return addressList0(fieldName, addresses);
    }

    private static DateTimeField date0(String fieldName, Date date,
            TimeZone zone) {
        final String formattedDate = MimeUtil.formatDate(date, zone);
        return parse(DateTimeFieldImpl.PARSER, fieldName, formattedDate);
    }

    private static MailboxField mailbox0(String fieldName, Mailbox mailbox) {
        String fieldValue = encodeAddresses(Collections.singleton(mailbox));
        return parse(MailboxFieldImpl.PARSER, fieldName, fieldValue);
    }

    private static MailboxListField mailboxList0(String fieldName,
            Iterable<Mailbox> mailboxes) {
        String fieldValue = encodeAddresses(mailboxes);
        return parse(MailboxListFieldImpl.PARSER, fieldName, fieldValue);
    }

    private static AddressListField addressList0(String fieldName,
            Iterable<Address> addresses) {
        String fieldValue = encodeAddresses(addresses);
        return parse(AddressListFieldImpl.PARSER, fieldName, fieldValue);
    }

    private static void checkValidFieldName(String fieldName) {
        if (!FIELD_NAME_PATTERN.matcher(fieldName).matches())
            throw new IllegalArgumentException("Invalid field name");
    }

    private static boolean isValidMimeType(String mimeType) {
        if (mimeType == null)
            return false;

        int idx = mimeType.indexOf('/');
        if (idx == -1)
            return false;

        String type = mimeType.substring(0, idx);
        String subType = mimeType.substring(idx + 1);
        return EncoderUtil.isToken(type) && EncoderUtil.isToken(subType);
    }

    private static boolean isValidDispositionType(String dispositionType) {
        if (dispositionType == null)
            return false;

        return EncoderUtil.isToken(dispositionType);
    }

    private static <F extends ParsedField> F parse(FieldParser<F> parser,
            String fieldName, String fieldBody) {
        RawField rawField = new RawField(fieldName, fieldBody);
        return parser.parse(rawField.getName(), rawField.getBody(), rawField.getRaw(),
                DecodeMonitor.SILENT);
    }

    private static String encodeAddresses(Iterable<? extends Address> addresses) {
        StringBuilder sb = new StringBuilder();

        for (Address address : addresses) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            AddressFormatter.encode(sb, address);
        }
        return sb.toString();
    }

}
