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

import java.util.HashMap;
import java.util.Map;

public class DelegatingFieldParser implements FieldParser {
    
    private Map parsers = new HashMap();
    private FieldParser defaultParser = new UnstructuredField.Parser();
    
    /**
     * Sets the parser used for the field named <code>name</code>.
     * @param name the name of the field
     * @param parser the parser for fields named <code>name</code>
     */
    public void setFieldParser(final String name, final FieldParser parser) {
        parsers.put(name.toLowerCase(), parser);
    }
    
    public FieldParser getParser(final String name) {
        final FieldParser field = (FieldParser) parsers.get(name.toLowerCase());
        if(field==null) {
            return defaultParser;
        }
        return field;
    }
    
    public Field parse(final String name, final String body, final String raw) {
        final FieldParser parser = getParser(name);
        return parser.parse(name, body, raw);
    }
}
