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

import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.dom.address.MailboxList;
import org.apache.james.mime4j.field.address.parser.AddressBuilder;
import org.apache.james.mime4j.field.address.parser.ParseException;
import org.apache.james.mime4j.util.ByteSequence;

/**
 * Mailbox-list field such as <code>From</code> or <code>Resent-From</code>.
 */
public class MailboxListFieldImpl extends AbstractField implements org.apache.james.mime4j.dom.field.MailboxListField {
    private boolean parsed = false;

    private MailboxList mailboxList;
    private ParseException parseException;

    MailboxListFieldImpl(final String name, final String body, final ByteSequence raw, DecodeMonitor monitor) {
        super(name, body, raw, monitor);
    }

    /**
     * @see org.apache.james.mime4j.dom.field.MailboxListField#getMailboxList()
     */
    public MailboxList getMailboxList() {
        if (!parsed)
            parse();

        return mailboxList;
    }

    /**
     * @see org.apache.james.mime4j.dom.field.MailboxListField#getParseException()
     */
    @Override
    public ParseException getParseException() {
        if (!parsed)
            parse();

        return parseException;
    }

    private void parse() {
        String body = getBody();

        try {
            mailboxList = AddressBuilder.parseAddressList(body, monitor).flatten();
        } catch (ParseException e) {
            parseException = e;
        }

        parsed = true;
    }

    static final FieldParser<MailboxListFieldImpl> PARSER = new FieldParser<MailboxListFieldImpl>() {
        public MailboxListFieldImpl parse(final String name, final String body,
                final ByteSequence raw, DecodeMonitor monitor) {
            return new MailboxListFieldImpl(name, body, raw, monitor);
        }
    };
}
