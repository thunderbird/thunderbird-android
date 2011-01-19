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
import org.apache.james.mime4j.dom.field.ContentTransferEncodingField;
import org.apache.james.mime4j.util.ByteSequence;
import org.apache.james.mime4j.util.MimeUtil;

/**
 * Represents a <code>Content-Transfer-Encoding</code> field.
 */
public class ContentTransferEncodingFieldImpl extends AbstractField implements ContentTransferEncodingField {
    private String encoding;

    ContentTransferEncodingFieldImpl(String name, String body, ByteSequence raw, DecodeMonitor monitor) {
        super(name, body, raw, monitor);
        encoding = body.trim().toLowerCase();
    }

    /**
     * @see org.apache.james.mime4j.dom.field.ContentTransferEncodingField#getEncoding()
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Gets the encoding of the given field if. Returns the default
     * <code>7bit</code> if not set or if <code>f</code> is
     * <code>null</code>.
     *
     * @return the encoding.
     */
    public static String getEncoding(ContentTransferEncodingField f) {
        if (f != null && f.getEncoding().length() != 0) {
            return f.getEncoding();
        }
        return MimeUtil.ENC_7BIT;
    }

    static final FieldParser<ContentTransferEncodingFieldImpl> PARSER = new FieldParser<ContentTransferEncodingFieldImpl>() {
        public ContentTransferEncodingFieldImpl parse(final String name, final String body,
                final ByteSequence raw, DecodeMonitor monitor) {
            return new ContentTransferEncodingFieldImpl(name, body, raw, monitor);
        }
    };
}
