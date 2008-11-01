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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The base class of all field classes.
 *
 * 
 * @version $Id: Field.java,v 1.6 2004/10/25 07:26:46 ntherning Exp $
 */
public abstract class Field {
    public static final String SENDER = "Sender";
    public static final String FROM = "From";
    public static final String TO = "To";
    public static final String CC = "Cc";
    public static final String BCC = "Bcc";
    public static final String REPLY_TO = "Reply-To";
    public static final String RESENT_SENDER = "Resent-Sender";
    public static final String RESENT_FROM = "Resent-From";
    public static final String RESENT_TO = "Resent-To";
    public static final String RESENT_CC = "Resent-Cc";
    public static final String RESENT_BCC = "Resent-Bcc";

    public static final String DATE = "Date";
    public static final String RESENT_DATE = "Resent-Date";

    public static final String SUBJECT = "Subject";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TRANSFER_ENCODING = 
                                        "Content-Transfer-Encoding";
    
    private static final String FIELD_NAME_PATTERN = 
        "^([\\x21-\\x39\\x3b-\\x7e]+)[ \t]*:";
    private static final Pattern fieldNamePattern = 
        Pattern.compile(FIELD_NAME_PATTERN);
        
    private static final DefaultFieldParser parser = new DefaultFieldParser();
    
    private final String name;
    private final String body;
    private final String raw;
    
    protected Field(final String name, final String body, final String raw) {
        this.name = name;
        this.body = body;
        this.raw = raw;
    }
    
    /**
     * Parses the given string and returns an instance of the 
     * <code>Field</code> class. The type of the class returned depends on
     * the field name:
     * <table>
     *      <tr>
     *          <td><em>Field name</em></td><td><em>Class returned</em></td>
     *          <td>Content-Type</td><td>org.apache.james.mime4j.field.ContentTypeField</td>
     *          <td>other</td><td>org.apache.james.mime4j.field.UnstructuredField</td>
     *      </tr>
     * </table>
     * 
     * @param s the string to parse.
     * @return a <code>Field</code> instance.
     * @throws IllegalArgumentException on parse errors.
     */
    public static Field parse(final String raw) {
        
        /*
         * Unfold the field.
         */
        final String unfolded = raw.replaceAll("\r|\n", "");
        
        /*
         * Split into name and value.
         */
        final Matcher fieldMatcher = fieldNamePattern.matcher(unfolded);
        if (!fieldMatcher.find()) {
            throw new IllegalArgumentException("Invalid field in string");
        }
        final String name = fieldMatcher.group(1);
        
        String body = unfolded.substring(fieldMatcher.end());
        if (body.length() > 0 && body.charAt(0) == ' ') {
            body = body.substring(1);
        }
        
        return parser.parse(name, body, raw);
    }
    
    /**
     * Gets the default parser used to parse fields.
     * @return the default field parser
     */
    public static DefaultFieldParser getParser() {
        return parser;
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
     * Gets the original raw field string.
     * 
     * @return the original raw field string.
     */
    public String getRaw() {
        return raw;
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
     * Determines if this is a <code>Content-Type</code> field.
     * 
     * @return <code>true</code> if this is a <code>Content-Type</code> field,
     *         <code>false</code> otherwise.
     */
    public boolean isContentType() {
        return CONTENT_TYPE.equalsIgnoreCase(name);
    }
    
    /**
     * Determines if this is a <code>Subject</code> field.
     * 
     * @return <code>true</code> if this is a <code>Subject</code> field,
     *         <code>false</code> otherwise.
     */
    public boolean isSubject() {
        return SUBJECT.equalsIgnoreCase(name);
    }
    
    /**
     * Determines if this is a <code>From</code> field.
     * 
     * @return <code>true</code> if this is a <code>From</code> field,
     *         <code>false</code> otherwise.
     */
    public boolean isFrom() {
        return FROM.equalsIgnoreCase(name);
    }
    
    /**
     * Determines if this is a <code>To</code> field.
     * 
     * @return <code>true</code> if this is a <code>To</code> field,
     *         <code>false</code> otherwise.
     */
    public boolean isTo() {
        return TO.equalsIgnoreCase(name);
    }
    
    /**
     * @see #getRaw()
     */
    public String toString() {
        return raw;
    }
}
