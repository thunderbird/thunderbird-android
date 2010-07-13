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

package org.apache.james.mime4j;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Encapsulates the values of the MIME-specific header fields 
 * (which starts with <code>Content-</code>). 
 *
 * 
 * @version $Id: BodyDescriptor.java,v 1.4 2005/02/11 10:08:37 ntherning Exp $
 */
public class BodyDescriptor {
    private static Log log = LogFactory.getLog(BodyDescriptor.class);
    
    private String mimeType = "text/plain";
    private String boundary = null;
    private String charset = "us-ascii";
    private String transferEncoding = "7bit";
    private Map parameters = new HashMap();
    private boolean contentTypeSet = false;
    private boolean contentTransferEncSet = false;
    
    /**
     * Creates a new root <code>BodyDescriptor</code> instance.
     */
    public BodyDescriptor() {
        this(null);
    }

    /**
     * Creates a new <code>BodyDescriptor</code> instance.
     * 
     * @param parent the descriptor of the parent or <code>null</code> if this
     *        is the root descriptor.
     */
    public BodyDescriptor(BodyDescriptor parent) {
        if (parent != null && parent.isMimeType("multipart/digest")) {
            mimeType = "message/rfc822";
        } else {
            mimeType = "text/plain";
        }
    }
    
    /**
     * Should be called for each <code>Content-</code> header field of 
     * a MIME message or part.
     * 
     * @param name the field name.
     * @param value the field value.
     */
    public void addField(String name, String value) {
        
        name = name.trim().toLowerCase();
        
        if (name.equals("content-transfer-encoding") && !contentTransferEncSet) {
            contentTransferEncSet = true;
            
            value = value.trim().toLowerCase();
            if (value.length() > 0) {
                transferEncoding = value;
            }
            
        } else if (name.equals("content-type") && !contentTypeSet) {
            contentTypeSet = true;
            
            value = value.trim();
            
            /*
             * Unfold Content-Type value
             */
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c == '\r' || c == '\n') {
                    continue;
                }
                sb.append(c);
            }
            
            Map params = getHeaderParams(sb.toString());
            
            String main = (String) params.get("");
            if (main != null) {
                main = main.toLowerCase().trim();
                int index = main.indexOf('/');
                boolean valid = false;
                if (index != -1) {
                    String type = main.substring(0, index).trim();
                    String subtype = main.substring(index + 1).trim();
                    if (type.length() > 0 && subtype.length() > 0) {
                        main = type + "/" + subtype;
                        valid = true;
                    }
                }
                
                if (!valid) {
                    main = null;
                }
            }
            String b = (String) params.get("boundary");
            
            if (main != null 
                    && ((main.startsWith("multipart/") && b != null) 
                            || !main.startsWith("multipart/"))) {
                
                mimeType = main;
            }
            
            if (isMultipart()) {
                boundary = b;
            }
            
            String c = (String) params.get("charset");
            if (c != null) {
                c = c.trim();
                if (c.length() > 0) {
                    charset = c.toLowerCase();
                }
            }
            
            /*
             * Add all other parameters to parameters.
             */
            parameters.putAll(params);
            parameters.remove("");
            parameters.remove("boundary");
            parameters.remove("charset");
        }
    }
    
    private Map getHeaderParams(String headerValue) {
        Map result = new HashMap();

        // split main value and parameters
        String main;
        String rest;
        if (headerValue.indexOf(";") == -1) {
            main = headerValue;
            rest = null;
        } else {
            main = headerValue.substring(0, headerValue.indexOf(";"));
            rest = headerValue.substring(main.length() + 1);
        }

        result.put("", main);
        if (rest != null) {
            char[] chars = rest.toCharArray();
            StringBuffer paramName = new StringBuffer();
            StringBuffer paramValue = new StringBuffer();

            final byte READY_FOR_NAME = 0;
            final byte IN_NAME = 1;
            final byte READY_FOR_VALUE = 2;
            final byte IN_VALUE = 3;
            final byte IN_QUOTED_VALUE = 4;
            final byte VALUE_DONE = 5;
            final byte ERROR = 99;

            byte state = READY_FOR_NAME;
            boolean escaped = false;
            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];

                switch (state) {
                    case ERROR:
                        if (c == ';')
                            state = READY_FOR_NAME;
                        break;

                    case READY_FOR_NAME:
                        if (c == '=') {
                            log.error("Expected header param name, got '='");
                            state = ERROR;
                            break;
                        }

                        paramName = new StringBuffer();
                        paramValue = new StringBuffer();

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
    

    public boolean isMimeType(String mimeType) {
        return this.mimeType.equals(mimeType.toLowerCase());
    }
    
    /**
     * Return true if the BodyDescriptor belongs to a message 
     * 
     * @return
     */
    public boolean isMessage() {
        return mimeType.equals("message/rfc822");
    }
    
    /**
     * Retrun true if the BodyDescripotro belogns to a multipart
     * 
     * @return
     */
    public boolean isMultipart() {
        return mimeType.startsWith("multipart/");
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
    public Map getParameters() {
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
    
    /**
     * Return true if it's base64 encoded
     * 
     * @return
     * 
     */
    public boolean isBase64Encoded() {
        return "base64".equals(transferEncoding);
    }
    
    /**
     * Return true if it's quoted-printable
     * @return
     */
    public boolean isQuotedPrintableEncoded() {
        return "quoted-printable".equals(transferEncoding);
    }
    
    public String toString() {
        return mimeType;
    }
}
