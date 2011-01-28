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

package org.apache.james.mime4j.dom.field;

import java.util.Map;

public interface ContentTypeField extends ParsedField {

    /** The prefix of all <code>multipart</code> MIME types. */
    public static final String TYPE_MULTIPART_PREFIX = "multipart/";
    /** The <code>multipart/digest</code> MIME type. */
    public static final String TYPE_MULTIPART_DIGEST = "multipart/digest";
    /** The <code>text/plain</code> MIME type. */
    public static final String TYPE_TEXT_PLAIN = "text/plain";
    /** The <code>message/rfc822</code> MIME type. */
    public static final String TYPE_MESSAGE_RFC822 = "message/rfc822";
    /** The name of the <code>boundary</code> parameter. */
    public static final String PARAM_BOUNDARY = "boundary";
    /** The name of the <code>charset</code> parameter. */
    public static final String PARAM_CHARSET = "charset";

    /**
     * Gets the MIME type defined in this Content-Type field.
     *
     * @return the MIME type or an empty string if not set.
     */
    String getMimeType();

    /**
     * Gets the value of a parameter. Parameter names are case-insensitive.
     *
     * @param name
     *            the name of the parameter to get.
     * @return the parameter value or <code>null</code> if not set.
     */
    String getParameter(String name);

    /**
     * Gets all parameters.
     *
     * @return the parameters.
     */
    Map<String, String> getParameters();

    /**
     * Determines if the MIME type of this field matches the given one.
     *
     * @param mimeType
     *            the MIME type to match against.
     * @return <code>true</code> if the MIME type of this field matches,
     *         <code>false</code> otherwise.
     */
    boolean isMimeType(String mimeType);

    /**
     * Determines if the MIME type of this field is <code>multipart/*</code>.
     *
     * @return <code>true</code> if this field is has a
     *         <code>multipart/*</code> MIME type, <code>false</code>
     *         otherwise.
     */
    boolean isMultipart();

    /**
     * Gets the value of the <code>boundary</code> parameter if set.
     *
     * @return the <code>boundary</code> parameter value or <code>null</code>
     *         if not set.
     */
    String getBoundary();

    /**
     * Gets the value of the <code>charset</code> parameter if set.
     *
     * @return the <code>charset</code> parameter value or <code>null</code>
     *         if not set.
     */
    String getCharset();

}