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

import java.io.IOException;
import java.io.OutputStream;

import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.dom.field.ParseException;
import org.apache.james.mime4j.dom.field.ParsedField;
import org.apache.james.mime4j.util.ByteSequence;

/**
 * The base class of all field classes.
 */
public abstract class AbstractField implements ParsedField {

    private final String name;
    private final String body;
    private final ByteSequence raw;
    protected DecodeMonitor monitor;

    protected AbstractField(
            final String name,
            final String body,
            final ByteSequence raw,
            final DecodeMonitor monitor) {
        this.name = name;
        this.body = body;
        this.raw = raw;
        this.monitor = monitor != null ? monitor : DecodeMonitor.SILENT;
    }

    /**
     * Gets the name of the field (<code>Subject</code>,
     * <code>From</code>, etc).
     *
     * @return the field name.
     */
    public String getName() {
        return name;
    }

    /**
     * @see org.apache.james.mime4j.dom.field.Field#writeTo(java.io.OutputStream)
     */
    public void writeTo(OutputStream out) throws IOException {
        out.write(raw.toByteArray());
    }

    /**
     * Gets the unfolded, unparsed and possibly encoded (see RFC 2047) field
     * body string.
     *
     * @return the unfolded unparsed field body string.
     */
    public String getBody() {
        return body;
    }

    /**
     * @see ParsedField#isValidField()
     */
    public boolean isValidField() {
        return getParseException() == null;
    }

    /**
     * @see ParsedField#getParseException()
     */
    public ParseException getParseException() {
        return null;
    }

    @Override
    public String toString() {
        return name + ": " + body;
    }

}
