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

package org.apache.james.mime4j;

import org.apache.james.mime4j.decoder.Base64InputStream;
import org.apache.james.mime4j.decoder.QuotedPrintableInputStream;
import org.apache.james.mime4j.field.Field;
import org.apache.james.mime4j.message.Header;

import java.io.InputStream;
import java.io.IOException;

/**
 * Abstract implementation of ContentHandler that automates common
 * tasks. Currently performs header parsing and applies content-transfer
 * decoding to body parts.
 *
 * 
 */
public abstract class SimpleContentHandler extends  AbstractContentHandler {

    /**
     * Called after headers are parsed.
     */
    public abstract void headers(Header header);

    /**
     * Called when the body of a discrete (non-multipart) entity is encountered.

     * @param bd encapsulates the values (either read from the
     *        message stream or, if not present, determined implictly
     *        as described in the
     *        MIME rfc:s) of the <code>Content-Type</code> and
     *        <code>Content-Transfer-Encoding</code> header fields.
     * @param is the contents of the body. Base64 or quoted-printable
     *        decoding will be applied transparently.
     * @throws IOException should be thrown on I/O errors.
     */
    public abstract void bodyDecoded(BodyDescriptor bd, InputStream is) throws IOException;


    /* Implement introduced callbacks. */

    private Header currHeader;

    /**
     * @see org.apache.james.mime4j.AbstractContentHandler#startHeader()
     */
    public final void startHeader() {
        currHeader = new Header();
    }

    /**
     * @see org.apache.james.mime4j.AbstractContentHandler#field(java.lang.String)
     */
    public final void field(String fieldData) {
        currHeader.addField(Field.parse(fieldData));
    }

    /**
     * @see org.apache.james.mime4j.AbstractContentHandler#endHeader()
     */
    public final void endHeader() {
        Header tmp = currHeader;
        currHeader = null;
        headers(tmp);
    }

    /**
     * @see org.apache.james.mime4j.AbstractContentHandler#body(org.apache.james.mime4j.BodyDescriptor, java.io.InputStream)
     */
    public final void body(BodyDescriptor bd, InputStream is) throws IOException {
        if (bd.isBase64Encoded()) {
            bodyDecoded(bd, new Base64InputStream(is));
        }
        else if (bd.isQuotedPrintableEncoded()) {
            bodyDecoded(bd, new QuotedPrintableInputStream(is));
        }
        else {
            bodyDecoded(bd, is);
        }
    }
}
