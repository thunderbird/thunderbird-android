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

import java.util.HashMap;
import java.util.Map;

import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.dom.field.ParsedField;
import org.apache.james.mime4j.field.UnstructuredFieldImpl;
import org.apache.james.mime4j.util.ByteSequence;

public class DelegatingFieldParser implements FieldParser<ParsedField> {
    private static final FieldParser<UnstructuredFieldImpl> DEFAULT_PARSER = UnstructuredFieldImpl.PARSER;

    private Map<String, FieldParser<? extends ParsedField>> parsers = new HashMap<String, FieldParser<? extends ParsedField>>();

    /**
     * Sets the parser used for the field named <code>name</code>.
     * @param name the name of the field
     * @param parser the parser for fields named <code>name</code>
     */
    public void setFieldParser(final String name, final FieldParser<? extends ParsedField> parser) {
        parsers.put(name.toLowerCase(), parser);
    }
    
    public FieldParser<? extends ParsedField> getParser(final String name) {
        final FieldParser<? extends ParsedField> field = parsers.get(name.toLowerCase());
        if (field == null) {
            return DEFAULT_PARSER;
        }
        return field;
    }
    
    public ParsedField parse(final String name, final String body, final ByteSequence raw, DecodeMonitor monitor) {
        final FieldParser<? extends ParsedField> parser = getParser(name);
        return parser.parse(name, body, raw, monitor);
    }
}
