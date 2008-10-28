/*
 *  Copyright 2006 the mime4j project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.james.mime4j.field;

public class DefaultFieldParser extends DelegatingFieldParser {
    
    public DefaultFieldParser() {
        setFieldParser(Field.CONTENT_TRANSFER_ENCODING, new ContentTransferEncodingField.Parser());
        setFieldParser(Field.CONTENT_TYPE, new ContentTypeField.Parser());
        
        final DateTimeField.Parser dateTimeParser = new DateTimeField.Parser();
        setFieldParser(Field.DATE, dateTimeParser);
        setFieldParser(Field.RESENT_DATE, dateTimeParser);
        
        final MailboxListField.Parser mailboxListParser = new MailboxListField.Parser();
        setFieldParser(Field.FROM, mailboxListParser);
        setFieldParser(Field.RESENT_FROM, mailboxListParser);
        
        final MailboxField.Parser mailboxParser = new MailboxField.Parser();
        setFieldParser(Field.SENDER, mailboxParser);
        setFieldParser(Field.RESENT_SENDER, mailboxParser);
        
        final AddressListField.Parser addressListParser = new AddressListField.Parser();
        setFieldParser(Field.TO, addressListParser);
        setFieldParser(Field.RESENT_TO, addressListParser);
        setFieldParser(Field.CC, addressListParser);
        setFieldParser(Field.RESENT_CC, addressListParser);
        setFieldParser(Field.BCC, addressListParser);
        setFieldParser(Field.RESENT_BCC, addressListParser);
        setFieldParser(Field.REPLY_TO, addressListParser);
    }
}
