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

package org.apache.james.mime4j.stream;

import java.util.HashMap;
import java.util.Map;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.util.MimeUtil;

/**
 * Encapsulates the values of the MIME-specific header fields
 * (which starts with <code>Content-</code>).
 */
public class DefaultBodyDescriptor implements MutableBodyDescriptor {
    private static final String US_ASCII = "us-ascii";

    private static final String SUB_TYPE_EMAIL = "rfc822";

    private static final String MEDIA_TYPE_TEXT = "text";

    private static final String MEDIA_TYPE_MESSAGE = "message";

    private static final String EMAIL_MESSAGE_MIME_TYPE = MEDIA_TYPE_MESSAGE + "/" + SUB_TYPE_EMAIL;

    private static final String DEFAULT_SUB_TYPE = "plain";

    private static final String DEFAULT_MEDIA_TYPE = MEDIA_TYPE_TEXT;

    private static final String DEFAULT_MIME_TYPE = DEFAULT_MEDIA_TYPE + "/" + DEFAULT_SUB_TYPE;

    private final DecodeMonitor monitor;

    private String mediaType = DEFAULT_MEDIA_TYPE;
    private String subType = DEFAULT_SUB_TYPE;
    private String mimeType = DEFAULT_MIME_TYPE;
    private String boundary = null;
    private String charset = US_ASCII;
    private String transferEncoding = "7bit";
    private Map<String, String> parameters = new HashMap<String, String>();
    private boolean contentTypeSet;
    private boolean contentTransferEncSet;
    private long contentLength = -1;

    /**
     * Creates a new root <code>BodyDescriptor</code> instance.
     */
    public DefaultBodyDescriptor() {
        this(null, null);
    }

    /**
     * Creates a new <code>BodyDescriptor</code> instance.
     *
     * @param parent the descriptor of the parent or <code>null</code> if this
     *        is the root descriptor.
     */
    public DefaultBodyDescriptor(final BodyDescriptor parent, final DecodeMonitor monitor) {
        if (parent != null && MimeUtil.isSameMimeType("multipart/digest", parent.getMimeType())) {
            this.mimeType = EMAIL_MESSAGE_MIME_TYPE;
            this.subType = SUB_TYPE_EMAIL;
            this.mediaType = MEDIA_TYPE_MESSAGE;
        } else {
            this.mimeType = DEFAULT_MIME_TYPE;
            this.subType = DEFAULT_SUB_TYPE;
            this.mediaType = DEFAULT_MEDIA_TYPE;
        }
        this.monitor = monitor != null ? monitor : DecodeMonitor.SILENT;
    }

    protected DecodeMonitor getDecodeMonitor() {
        return monitor;
    }

    public MutableBodyDescriptor newChild() {
		return new DefaultBodyDescriptor(this, getDecodeMonitor());
    }

    /**
     * Should be called for each <code>Content-</code> header field of
     * a MIME message or part.
     *
     * @param field the MIME field.
     */
    public void addField(RawField field) throws MimeException {
        String name = field.getName();
        String value = field.getBody();

        name = name.trim().toLowerCase();

        if (name.equals("content-transfer-encoding") && !contentTransferEncSet) {
            contentTransferEncSet = true;

            value = value.trim().toLowerCase();
            if (value.length() > 0) {
                transferEncoding = value;
            }

        } else if (name.equals("content-length") && contentLength == -1) {
            try {
                contentLength = Long.parseLong(value.trim());
            } catch (NumberFormatException e) {
                if (monitor.warn("Invalid content length: " + value,
                        "ignoring Content-Length header")) {
                    throw new MimeException("Invalid Content-Length header: " + value);
                }
            }
        } else if (name.equals("content-type") && !contentTypeSet) {
            parseContentType(value);
        }
    }

    private void parseContentType(String value) throws MimeException {
        contentTypeSet = true;

        Map<String, String> params = DefaultBodyDescriptor.getHeaderParams(value, getDecodeMonitor());

        String main = params.get("");
        String type = null;
        String subtype = null;
        if (main != null) {
            main = main.toLowerCase().trim();
            int index = main.indexOf('/');
            boolean valid = false;
            if (index != -1) {
                type = main.substring(0, index).trim();
                subtype = main.substring(index + 1).trim();
                if (type.length() > 0 && subtype.length() > 0) {
                    main = type + "/" + subtype;
                    valid = true;
                }
            }

            if (!valid) {
                main = null;
                type = null;
                subtype = null;
            }
        }
        String b = params.get("boundary");

        if (main != null
                && ((main.startsWith("multipart/") && b != null)
                        || !main.startsWith("multipart/"))) {
            mimeType = main;
            this.subType = subtype;
            this.mediaType = type;
        }

        if (MimeUtil.isMultipart(mimeType)) {
            boundary = b;
        }

        String c = params.get("charset");
        charset = null;
        if (c != null) {
            c = c.trim();
            if (c.length() > 0) {
                charset = c.toLowerCase();
            }
        }
        if (charset == null && MEDIA_TYPE_TEXT.equals(mediaType)) {
            charset = US_ASCII;
        }

        /*
         * Add all other parameters to parameters.
         */
        parameters.putAll(params);
        parameters.remove("");
        parameters.remove("boundary");
        parameters.remove("charset");
    }

    /**
     * Return the MimeType
     *
     * @return mimeType
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Return the boundary
     *
     * @return boundary
     */
    public String getBoundary() {
        return boundary;
    }

    /**
     * Return the charset
     *
     * @return charset
     */
    public String getCharset() {
        return charset;
    }

    /**
     * Return all parameters for the BodyDescriptor
     *
     * @return parameters
     */
    public Map<String, String> getContentTypeParameters() {
        return parameters;
    }

    /**
     * Return the TransferEncoding
     *
     * @return transferEncoding
     */
    public String getTransferEncoding() {
        return transferEncoding;
    }

    @Override
    public String toString() {
        return mimeType;
    }

    public long getContentLength() {
        return contentLength;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getSubType() {
        return subType;
    }

    /**
     * <p>Parses a complex field value into a map of key/value pairs. You may
     * use this, for example, to parse a definition like
     * <pre>
     *   text/plain; charset=UTF-8; boundary=foobar
     * </pre>
     * The above example would return a map with the keys "", "charset",
     * and "boundary", and the values "text/plain", "UTF-8", and "foobar".
     * </p><p>
     * Header value will be unfolded and excess white space trimmed.
     * </p>
     * @param pValue The field value to parse.
     * @return The result map; use the key "" to retrieve the first value.
     */
    public static Map<String, String> getHeaderParams(
            String pValue, DecodeMonitor monitor) throws MimeException {
        pValue = pValue.trim();

        Map<String, String> result = new HashMap<String, String>();

        // split main value and parameters
        String main;
        String rest;
        if (pValue.indexOf(";") == -1) {
            main = pValue;
            rest = null;
        } else {
            main = pValue.substring(0, pValue.indexOf(";"));
            rest = pValue.substring(main.length() + 1);
        }

        result.put("", main);
        if (rest != null) {
            char[] chars = rest.toCharArray();
            StringBuilder paramName = new StringBuilder(64);
            StringBuilder paramValue = new StringBuilder(64);

            final byte READY_FOR_NAME = 0;
            final byte IN_NAME = 1;
            final byte READY_FOR_VALUE = 2;
            final byte IN_VALUE = 3;
            final byte IN_QUOTED_VALUE = 4;
            final byte VALUE_DONE = 5;
            final byte ERROR = 99;

            byte state = READY_FOR_NAME;
            boolean escaped = false;
            for (char c : chars) {
                switch (state) {
                    case ERROR:
                        if (c == ';')
                            state = READY_FOR_NAME;
                        break;

                    case READY_FOR_NAME:
                        if (c == '=') {
                            if (monitor.warn("Expected header param name, got '='", "ignoring")) {
                                throw new MimeException("Expected header param name, got '='");
                            }
                            state = ERROR;
                            break;
                        }

                        paramName.setLength(0);
                        paramValue.setLength(0);

                        state = IN_NAME;
                        // fall-through

                    case IN_NAME:
                        if (c == '=') {
                            if (paramName.length() == 0)
                                state = ERROR;
                            else
                                state = READY_FOR_VALUE;
                            break;
                        }

                        // not '='... just add to name
                        paramName.append(c);
                        break;

                    case READY_FOR_VALUE:
                        boolean fallThrough = false;
                        switch (c) {
                            case ' ':
                            case '\t':
                                break;  // ignore spaces, especially before '"'

                            case '"':
                                state = IN_QUOTED_VALUE;
                                break;

                            default:
                                state = IN_VALUE;
                                fallThrough = true;
                                break;
                        }
                        if (!fallThrough)
                            break;

                        // fall-through

                    case IN_VALUE:
                        fallThrough = false;
                        switch (c) {
                            case ';':
                            case ' ':
                            case '\t':
                                result.put(
                                   paramName.toString().trim().toLowerCase(),
                                   paramValue.toString().trim());
                                state = VALUE_DONE;
                                fallThrough = true;
                                break;
                            default:
                                paramValue.append(c);
                                break;
                        }
                        if (!fallThrough)
                            break;

                    case VALUE_DONE:
                        switch (c) {
                            case ';':
                                state = READY_FOR_NAME;
                                break;

                            case ' ':
                            case '\t':
                                break;

                            default:
                                state = ERROR;
                                break;
                        }
                        break;

                    case IN_QUOTED_VALUE:
                        switch (c) {
                            case '"':
                                if (!escaped) {
                                    // don't trim quoted strings; the spaces could be intentional.
                                    result.put(
                                            paramName.toString().trim().toLowerCase(),
                                            paramValue.toString());
                                    state = VALUE_DONE;
                                } else {
                                    escaped = false;
                                    paramValue.append(c);
                                }
                                break;

                            case '\\':
                                if (escaped) {
                                    paramValue.append('\\');
                                }
                                escaped = !escaped;
                                break;

                            default:
                                if (escaped) {
                                    paramValue.append('\\');
                                }
                                escaped = false;
                                paramValue.append(c);
                                break;
                        }
                        break;

                }
            }

            // done looping.  check if anything is left over.
            if (state == IN_VALUE) {
                result.put(
                        paramName.toString().trim().toLowerCase(),
                        paramValue.toString().trim());
            }
        }

        return result;
    }
}
