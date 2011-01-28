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

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.MimeIOException;
import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.field.Field;
import org.apache.james.mime4j.field.DefaultFieldParser;
import org.apache.james.mime4j.parser.AbstractContentHandler;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.stream.RawField;

/**
 * The header of an entity (see RFC 2045).
 */
public class HeaderImpl extends Header {

    /**
     * Creates a new empty <code>Header</code>.
     */
    public HeaderImpl() {
    }

    /**
     * Creates a new <code>Header</code> from the specified
     * <code>Header</code>. The <code>Header</code> instance is initialized
     * with a copy of the list of {@link Field}s of the specified
     * <code>Header</code>. The <code>Field</code> objects are not copied
     * because they are immutable and can safely be shared between headers.
     *
     * @param other
     *            header to copy.
     */
    public HeaderImpl(Header other) {
        for (Field otherField : other.getFields()) {
            addField(otherField);
        }
    }

    /**
     * Creates a new <code>Header</code> from the specified stream.
     *
     * @param is the stream to read the header from.
     *
     * @throws IOException on I/O errors.
     * @throws MimeIOException on MIME protocol violations.
     */
    public HeaderImpl(
            final InputStream is,
            final DecodeMonitor monitor) throws IOException, MimeIOException {
        final MimeStreamParser parser = new MimeStreamParser();
        parser.setContentHandler(new AbstractContentHandler() {
            @Override
            public void endHeader() {
                parser.stop();
            }
            @Override
            public void field(RawField field) throws MimeException {
                Field parsedField = DefaultFieldParser.parse(field.getRaw(), monitor);
                addField(parsedField);
            }
        });
        try {
            parser.parse(is);
        } catch (MimeException ex) {
            throw new MimeIOException(ex);
        }
    }

}
