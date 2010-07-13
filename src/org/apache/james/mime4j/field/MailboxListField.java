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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.mime4j.field.address.AddressList;
import org.apache.james.mime4j.field.address.MailboxList;
import org.apache.james.mime4j.field.address.parser.ParseException;

public class MailboxListField extends Field {
    
    private MailboxList mailboxList;
    private ParseException parseException;

    protected MailboxListField(final String name, final String body, final String raw, final MailboxList mailboxList, final ParseException parseException) {
        super(name, body, raw);
        this.mailboxList = mailboxList;
        this.parseException = parseException;
    }

    public MailboxList getMailboxList() {
        return mailboxList;
    }

    public ParseException getParseException() {
        return parseException;
    }
    
    public static class Parser implements FieldParser {
        private static Log log = LogFactory.getLog(Parser.class);

        public Field parse(final String name, final String body, final String raw) {
            MailboxList mailboxList = null;
            ParseException parseException = null;
            try {
                mailboxList = AddressList.parse(body).flatten();
            }
            catch (ParseException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Parsing value '" + body + "': "+ e.getMessage());
                }
                parseException = e;
            }
            return new MailboxListField(name, body, raw, mailboxList, parseException);
        }
    }
}
