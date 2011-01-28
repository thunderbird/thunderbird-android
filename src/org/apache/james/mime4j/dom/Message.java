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

package org.apache.james.mime4j.dom;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;

import org.apache.james.mime4j.dom.address.Address;
import org.apache.james.mime4j.dom.address.AddressList;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.address.MailboxList;
import org.apache.james.mime4j.dom.field.AddressListField;
import org.apache.james.mime4j.dom.field.DateTimeField;
import org.apache.james.mime4j.dom.field.Field;
import org.apache.james.mime4j.dom.field.FieldName;
import org.apache.james.mime4j.dom.field.MailboxField;
import org.apache.james.mime4j.dom.field.MailboxListField;
import org.apache.james.mime4j.dom.field.UnstructuredField;

public abstract class Message extends Entity implements Body {

    /**
     * Write the content to the given output stream using the
     * {@link org.apache.james.mime4j.message.MessageWriter#DEFAULT default} message writer.
     *
     * @param out
     *            the output stream to write to.
     * @throws IOException
     *             in case of an I/O error
     * @see org.apache.james.mime4j.message.MessageWriter
     */
    public abstract void writeTo(OutputStream out) throws IOException;

    /**
     * Returns the value of the <i>Message-ID</i> header field of this message
     * or <code>null</code> if it is not present.
     *
     * @return the identifier of this message.
     */
    public String getMessageId() {
        Field field = obtainField(FieldName.MESSAGE_ID);
        if (field == null)
            return null;

        return field.getBody();
    }

    /**
     * Creates and sets a new <i>Message-ID</i> header field for this message.
     * A <code>Header</code> is created if this message does not already have
     * one.
     *
     * @param hostname
     *            host name to be included in the identifier or
     *            <code>null</code> if no host name should be included.
     */
    public void createMessageId(String hostname) {
        Header header = obtainHeader();

        header.setField(newMessageId(hostname));
    }

    protected abstract Field newMessageId(String hostname);

    /**
     * Returns the (decoded) value of the <i>Subject</i> header field of this
     * message or <code>null</code> if it is not present.
     *
     * @return the subject of this message.
     */
    public String getSubject() {
        UnstructuredField field = obtainField(FieldName.SUBJECT);
        if (field == null)
            return null;

        return field.getValue();
    }

    /**
     * Sets the <i>Subject</i> header field for this message. The specified
     * string may contain non-ASCII characters, in which case it gets encoded as
     * an 'encoded-word' automatically. A <code>Header</code> is created if
     * this message does not already have one.
     *
     * @param subject
     *            subject to set or <code>null</code> to remove the subject
     *            header field.
     */
    public void setSubject(String subject) {
        Header header = obtainHeader();

        if (subject == null) {
            header.removeFields(FieldName.SUBJECT);
        } else {
            header.setField(newSubject(subject));
        }
    }

    /**
     * Returns the value of the <i>Date</i> header field of this message as
     * <code>Date</code> object or <code>null</code> if it is not present.
     *
     * @return the date of this message.
     */
    public Date getDate() {
        DateTimeField dateField = obtainField(FieldName.DATE);
        if (dateField == null)
            return null;

        return dateField.getDate();
    }

    /**
     * Sets the <i>Date</i> header field for this message. This method uses the
     * default <code>TimeZone</code> of this host to encode the specified
     * <code>Date</code> object into a string.
     *
     * @param date
     *            date to set or <code>null</code> to remove the date header
     *            field.
     */
    public void setDate(Date date) {
        setDate(date, null);
    }

    /**
     * Sets the <i>Date</i> header field for this message. The specified
     * <code>TimeZone</code> is used to encode the specified <code>Date</code>
     * object into a string.
     *
     * @param date
     *            date to set or <code>null</code> to remove the date header
     *            field.
     * @param zone
     *            a time zone.
     */
    public void setDate(Date date, TimeZone zone) {
        Header header = obtainHeader();

        if (date == null) {
            header.removeFields(FieldName.DATE);
        } else {
            header.setField(newDate(date, zone));
        }
    }

    /**
     * Returns the value of the <i>Sender</i> header field of this message as
     * <code>Mailbox</code> object or <code>null</code> if it is not
     * present.
     *
     * @return the sender of this message.
     */
    public Mailbox getSender() {
        return getMailbox(FieldName.SENDER);
    }

    /**
     * Sets the <i>Sender</i> header field of this message to the specified
     * mailbox address.
     *
     * @param sender
     *            address to set or <code>null</code> to remove the header
     *            field.
     */
    public void setSender(Mailbox sender) {
        setMailbox(FieldName.SENDER, sender);
    }

    /**
     * Returns the value of the <i>From</i> header field of this message as
     * <code>MailboxList</code> object or <code>null</code> if it is not
     * present.
     *
     * @return value of the from field of this message.
     */
    public MailboxList getFrom() {
        return getMailboxList(FieldName.FROM);
    }

    /**
     * Sets the <i>From</i> header field of this message to the specified
     * mailbox address.
     *
     * @param from
     *            address to set or <code>null</code> to remove the header
     *            field.
     */
    public void setFrom(Mailbox from) {
        setMailboxList(FieldName.FROM, from);
    }

    /**
     * Sets the <i>From</i> header field of this message to the specified
     * mailbox addresses.
     *
     * @param from
     *            addresses to set or <code>null</code> or no arguments to
     *            remove the header field.
     */
    public void setFrom(Mailbox... from) {
        setMailboxList(FieldName.FROM, from);
    }

    /**
     * Sets the <i>From</i> header field of this message to the specified
     * mailbox addresses.
     *
     * @param from
     *            addresses to set or <code>null</code> or an empty collection
     *            to remove the header field.
     */
    public void setFrom(Collection<Mailbox> from) {
        setMailboxList(FieldName.FROM, from);
    }

    /**
     * Returns the value of the <i>To</i> header field of this message as
     * <code>AddressList</code> object or <code>null</code> if it is not
     * present.
     *
     * @return value of the to field of this message.
     */
    public AddressList getTo() {
        return getAddressList(FieldName.TO);
    }

    /**
     * Sets the <i>To</i> header field of this message to the specified
     * address.
     *
     * @param to
     *            address to set or <code>null</code> to remove the header
     *            field.
     */
    public void setTo(Address to) {
        setAddressList(FieldName.TO, to);
    }

    /**
     * Sets the <i>To</i> header field of this message to the specified
     * addresses.
     *
     * @param to
     *            addresses to set or <code>null</code> or no arguments to
     *            remove the header field.
     */
    public void setTo(Address... to) {
        setAddressList(FieldName.TO, to);
    }

    /**
     * Sets the <i>To</i> header field of this message to the specified
     * addresses.
     *
     * @param to
     *            addresses to set or <code>null</code> or an empty collection
     *            to remove the header field.
     */
    public void setTo(Collection<Address> to) {
        setAddressList(FieldName.TO, to);
    }

    /**
     * Returns the value of the <i>Cc</i> header field of this message as
     * <code>AddressList</code> object or <code>null</code> if it is not
     * present.
     *
     * @return value of the cc field of this message.
     */
    public AddressList getCc() {
        return getAddressList(FieldName.CC);
    }

    /**
     * Sets the <i>Cc</i> header field of this message to the specified
     * address.
     *
     * @param cc
     *            address to set or <code>null</code> to remove the header
     *            field.
     */
    public void setCc(Address cc) {
        setAddressList(FieldName.CC, cc);
    }

    /**
     * Sets the <i>Cc</i> header field of this message to the specified
     * addresses.
     *
     * @param cc
     *            addresses to set or <code>null</code> or no arguments to
     *            remove the header field.
     */
    public void setCc(Address... cc) {
        setAddressList(FieldName.CC, cc);
    }

    /**
     * Sets the <i>Cc</i> header field of this message to the specified
     * addresses.
     *
     * @param cc
     *            addresses to set or <code>null</code> or an empty collection
     *            to remove the header field.
     */
    public void setCc(Collection<Address> cc) {
        setAddressList(FieldName.CC, cc);
    }

    /**
     * Returns the value of the <i>Bcc</i> header field of this message as
     * <code>AddressList</code> object or <code>null</code> if it is not
     * present.
     *
     * @return value of the bcc field of this message.
     */
    public AddressList getBcc() {
        return getAddressList(FieldName.BCC);
    }

    /**
     * Sets the <i>Bcc</i> header field of this message to the specified
     * address.
     *
     * @param bcc
     *            address to set or <code>null</code> to remove the header
     *            field.
     */
    public void setBcc(Address bcc) {
        setAddressList(FieldName.BCC, bcc);
    }

    /**
     * Sets the <i>Bcc</i> header field of this message to the specified
     * addresses.
     *
     * @param bcc
     *            addresses to set or <code>null</code> or no arguments to
     *            remove the header field.
     */
    public void setBcc(Address... bcc) {
        setAddressList(FieldName.BCC, bcc);
    }

    /**
     * Sets the <i>Bcc</i> header field of this message to the specified
     * addresses.
     *
     * @param bcc
     *            addresses to set or <code>null</code> or an empty collection
     *            to remove the header field.
     */
    public void setBcc(Collection<Address> bcc) {
        setAddressList(FieldName.BCC, bcc);
    }

    /**
     * Returns the value of the <i>Reply-To</i> header field of this message as
     * <code>AddressList</code> object or <code>null</code> if it is not
     * present.
     *
     * @return value of the reply to field of this message.
     */
    public AddressList getReplyTo() {
        return getAddressList(FieldName.REPLY_TO);
    }

    /**
     * Sets the <i>Reply-To</i> header field of this message to the specified
     * address.
     *
     * @param replyTo
     *            address to set or <code>null</code> to remove the header
     *            field.
     */
    public void setReplyTo(Address replyTo) {
        setAddressList(FieldName.REPLY_TO, replyTo);
    }

    /**
     * Sets the <i>Reply-To</i> header field of this message to the specified
     * addresses.
     *
     * @param replyTo
     *            addresses to set or <code>null</code> or no arguments to
     *            remove the header field.
     */
    public void setReplyTo(Address... replyTo) {
        setAddressList(FieldName.REPLY_TO, replyTo);
    }

    /**
     * Sets the <i>Reply-To</i> header field of this message to the specified
     * addresses.
     *
     * @param replyTo
     *            addresses to set or <code>null</code> or an empty collection
     *            to remove the header field.
     */
    public void setReplyTo(Collection<Address> replyTo) {
        setAddressList(FieldName.REPLY_TO, replyTo);
    }

    private Mailbox getMailbox(String fieldName) {
        MailboxField field = obtainField(fieldName);
        if (field == null)
            return null;

        return field.getMailbox();
    }

    private void setMailbox(String fieldName, Mailbox mailbox) {
        Header header = obtainHeader();

        if (mailbox == null) {
            header.removeFields(fieldName);
        } else {
            header.setField(newMailbox(fieldName, mailbox));
        }
    }

    private MailboxList getMailboxList(String fieldName) {
        MailboxListField field = obtainField(fieldName);
        if (field == null)
            return null;

        return field.getMailboxList();
    }

    private void setMailboxList(String fieldName, Mailbox mailbox) {
        setMailboxList(fieldName, mailbox == null ? null : Collections
                .singleton(mailbox));
    }

    private void setMailboxList(String fieldName, Mailbox... mailboxes) {
        setMailboxList(fieldName, mailboxes == null ? null : Arrays
                .asList(mailboxes));
    }

    private void setMailboxList(String fieldName, Collection<Mailbox> mailboxes) {
        Header header = obtainHeader();

        if (mailboxes == null || mailboxes.isEmpty()) {
            header.removeFields(fieldName);
        } else {
            header.setField(newMailboxList(fieldName, mailboxes));
        }
    }

    private AddressList getAddressList(String fieldName) {
        AddressListField field = obtainField(fieldName);
        if (field == null)
            return null;

        return field.getAddressList();
    }

    private void setAddressList(String fieldName, Address address) {
        setAddressList(fieldName, address == null ? null : Collections
                .singleton(address));
    }

    private void setAddressList(String fieldName, Address... addresses) {
        setAddressList(fieldName, addresses == null ? null : Arrays
                .asList(addresses));
    }

    private void setAddressList(String fieldName, Collection<Address> addresses) {
        Header header = obtainHeader();

        if (addresses == null || addresses.isEmpty()) {
            header.removeFields(fieldName);
        } else {
            header.setField(newAddressList(fieldName, addresses));
        }
    }

    protected abstract AddressListField newAddressList(String fieldName, Collection<Address> addresses);

    protected abstract UnstructuredField newSubject(String subject);

    protected abstract DateTimeField newDate(Date date, TimeZone zone);

    protected abstract MailboxField newMailbox(String fieldName, Mailbox mailbox);

    protected abstract MailboxListField newMailboxList(String fieldName, Collection<Mailbox> mailboxes);


}