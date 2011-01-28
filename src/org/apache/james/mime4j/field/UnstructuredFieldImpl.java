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
import org.apache.james.mime4j.codec.DecoderUtil;
import org.apache.james.mime4j.util.ByteSequence;

/**
 * Simple unstructured field such as <code>Subject</code>.
 */
public class UnstructuredFieldImpl extends AbstractField implements org.apache.james.mime4j.dom.field.UnstructuredField {
    private boolean parsed = false;

    private String value;

    UnstructuredFieldImpl(String name, String body, ByteSequence raw, DecodeMonitor monitor) {
        super(name, body, raw, monitor);
    }

    /**
     * @see org.apache.james.mime4j.dom.field.UnstructuredField#getValue()
     */
    public String getValue() {
        if (!parsed)
            parse();

        return value;
    }

    private void parse() {
        String body = getBody();

        value = DecoderUtil.decodeEncodedWords(body, monitor);

        parsed = true;
    }

    static final FieldParser<UnstructuredFieldImpl> PARSER = new FieldParser<UnstructuredFieldImpl>() {
        public UnstructuredFieldImpl parse(final String name, final String body,
                final ByteSequence raw, DecodeMonitor monitor) {
            return new UnstructuredFieldImpl(name, body, raw, monitor);
        }
    };
}
