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

import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.dom.field.ContentTypeField;
import org.apache.james.mime4j.field.contenttype.parser.ContentTypeParser;
import org.apache.james.mime4j.field.contenttype.parser.ParseException;
import org.apache.james.mime4j.field.contenttype.parser.TokenMgrError;
import org.apache.james.mime4j.util.ByteSequence;

/**
 * Represents a <code>Content-Type</code> field.
 */
public class ContentTypeFieldImpl extends AbstractField implements ContentTypeField {
    private boolean parsed = false;

    private String mimeType = "";
    private Map<String, String> parameters = new HashMap<String, String>();
    private ParseException parseException;

    ContentTypeFieldImpl(String name, String body, ByteSequence raw, DecodeMonitor monitor) {
        super(name, body, raw, monitor);
    }

    /**
     * @see org.apache.james.mime4j.dom.field.ContentTypeField#getParseException()
     */
    @Override
    public ParseException getParseException() {
        if (!parsed)
            parse();

        return parseException;
    }

    /**
     * @see org.apache.james.mime4j.dom.field.ContentTypeField#getMimeType()
     */
    public String getMimeType() {
        if (!parsed)
            parse();

        return mimeType;
    }

    /**
     * @see org.apache.james.mime4j.dom.field.ContentTypeField#getParameter(java.lang.String)
     */
    public String getParameter(String name) {
        if (!parsed)
            parse();

        return parameters.get(name.toLowerCase());
    }

    /**
     * @see org.apache.james.mime4j.dom.field.ContentTypeField#getParameters()
     */
    public Map<String, String> getParameters() {
        if (!parsed)
            parse();

        return Collections.unmodifiableMap(parameters);
    }

    /**
     * @see org.apache.james.mime4j.dom.field.ContentTypeField#isMimeType(java.lang.String)
     */
    public boolean isMimeType(String mimeType) {
        if (!parsed)
            parse();

        return this.mimeType.equalsIgnoreCase(mimeType);
    }

    /**
     * @see org.apache.james.mime4j.dom.field.ContentTypeField#isMultipart()
     */
    public boolean isMultipart() {
        if (!parsed)
            parse();

        return mimeType.startsWith(TYPE_MULTIPART_PREFIX);
    }

    /**
     * @see org.apache.james.mime4j.dom.field.ContentTypeField#getBoundary()
     */
    public String getBoundary() {
        return getParameter(PARAM_BOUNDARY);
    }

    /**
     * @see org.apache.james.mime4j.dom.field.ContentTypeField#getCharset()
     */
    public String getCharset() {
        return getParameter(PARAM_CHARSET);
    }

    /**
     * Gets the MIME type defined in the child's Content-Type field or derives a
     * MIME type from the parent if child is <code>null</code> or hasn't got a
     * MIME type value set. If child's MIME type is multipart but no boundary
     * has been set the MIME type of child will be derived from the parent.
     *
     * @param child
     *            the child.
     * @param parent
     *            the parent.
     * @return the MIME type.
     */
    public static String getMimeType(ContentTypeField child,
            ContentTypeField parent) {
        if (child == null || child.getMimeType().length() == 0
                || child.isMultipart() && child.getBoundary() == null) {

            if (parent != null && parent.isMimeType(TYPE_MULTIPART_DIGEST)) {
                return TYPE_MESSAGE_RFC822;
            } else {
                return TYPE_TEXT_PLAIN;
            }
        }

        return child.getMimeType();
    }

    /**
     * Gets the value of the <code>charset</code> parameter if set for the
     * given field. Returns the default <code>us-ascii</code> if not set or if
     * <code>f</code> is <code>null</code>.
     *
     * @return the <code>charset</code> parameter value.
     */
    public static String getCharset(ContentTypeField f) {
        if (f != null) {
            String charset = f.getCharset();
            if (charset != null && charset.length() > 0) {
                return charset;
            }
        }
        return "us-ascii";
    }

    private void parse() {
        String body = getBody();

        ContentTypeParser parser = new ContentTypeParser(new StringReader(body));
        try {
            parser.parseAll();
        } catch (ParseException e) {
            parseException = e;
        } catch (TokenMgrError e) {
            parseException = new ParseException(e.getMessage());
        }

        final String type = parser.getType();
        final String subType = parser.getSubType();

        if (type != null && subType != null) {
            mimeType = (type + "/" + subType).toLowerCase();

            List<String> paramNames = parser.getParamNames();
            List<String> paramValues = parser.getParamValues();

            if (paramNames != null && paramValues != null) {
                final int len = Math.min(paramNames.size(), paramValues.size());
                for (int i = 0; i < len; i++) {
                    String paramName = paramNames.get(i).toLowerCase();
                    String paramValue = paramValues.get(i);
                    parameters.put(paramName, paramValue);
                }
            }
        }

        parsed = true;
    }

    static final FieldParser<ContentTypeFieldImpl> PARSER = new FieldParser<ContentTypeFieldImpl>() {
        public ContentTypeFieldImpl parse(final String name, final String body,
                final ByteSequence raw, DecodeMonitor monitor) {
            return new ContentTypeFieldImpl(name, body, raw, monitor);
        }
    };
}
