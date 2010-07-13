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

import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.mime4j.decoder.Base64InputStream;
import org.apache.james.mime4j.decoder.QuotedPrintableInputStream;

/**
 * <p>
 * Parses MIME (or RFC822) message streams of bytes or characters and reports 
 * parsing events to a <code>ContentHandler</code> instance.
 * </p>
 * <p>
 * Typical usage:<br/>
 * <pre>
 *      ContentHandler handler = new MyHandler();
 *      MimeStreamParser parser = new MimeStreamParser();
 *      parser.setContentHandler(handler);
 *      parser.parse(new BufferedInputStream(new FileInputStream("mime.msg")));
 * </pre>
 * <strong>NOTE:</strong> All lines must end with CRLF 
 * (<code>\r\n</code>). If you are unsure of the line endings in your stream 
 * you should wrap it in a {@link org.apache.james.mime4j.EOLConvertingInputStream} instance.
 *
 * 
 * @version $Id: MimeStreamParser.java,v 1.8 2005/02/11 10:12:02 ntherning Exp $
 */
public class MimeStreamParser {
    private static final Log log = LogFactory.getLog(MimeStreamParser.class);
    
    private static BitSet fieldChars = null;
    
    private RootInputStream rootStream = null;
    private LinkedList bodyDescriptors = new LinkedList();
    private ContentHandler handler = null;
    private boolean raw = false;
    
    static {
        fieldChars = new BitSet();
        for (int i = 0x21; i <= 0x39; i++) {
            fieldChars.set(i);
        }
        for (int i = 0x3b; i <= 0x7e; i++) {
            fieldChars.set(i);
        }
    }
    
    /**
     * Creates a new <code>MimeStreamParser</code> instance.
     */
    public MimeStreamParser() {
    }

    /**
     * Parses a stream of bytes containing a MIME message.
     * 
     * @param is the stream to parse.
     * @throws IOException on I/O errors.
     */
    public void parse(InputStream is) throws IOException {
        rootStream = new RootInputStream(is);
        parseMessage(rootStream);
    }
    
    /**
     * Determines if this parser is currently in raw mode.
     * 
     * @return <code>true</code> if in raw mode, <code>false</code>
     *         otherwise.
     * @see #setRaw(boolean)
     */
    public boolean isRaw() {
        return raw;
    }
    
    /**
     * Enables or disables raw mode. In raw mode all future entities 
     * (messages or body parts) in the stream will be reported to the
     * {@link ContentHandler#raw(InputStream)} handler method only.
     * The stream will contain the entire unparsed entity contents 
     * including header fields and whatever is in the body.
     * 
     * @param raw <code>true</code> enables raw mode, <code>false</code>
     *        disables it.
     */
    public void setRaw(boolean raw) {
        this.raw = raw;
    }
    
    /**
     * Finishes the parsing and stops reading lines.
     * NOTE: No more lines will be parsed but the parser
     * will still call 
     * {@link ContentHandler#endMultipart()},
     * {@link ContentHandler#endBodyPart()},
     * {@link ContentHandler#endMessage()}, etc to match previous calls
     * to 
     * {@link ContentHandler#startMultipart(BodyDescriptor)},
     * {@link ContentHandler#startBodyPart()},
     * {@link ContentHandler#startMessage()}, etc.
     */
    public void stop() {
        rootStream.truncate();
    }
    
    /**
     * Parses an entity which consists of a header followed by a body containing
     * arbitrary data, body parts or an embedded message.
     * 
     * @param is the stream to parse.
     * @throws IOException on I/O errors.
     */
    private void parseEntity(InputStream is) throws IOException {
        BodyDescriptor bd = parseHeader(is);
        
        if (bd.isMultipart()) {
            bodyDescriptors.addFirst(bd);
            
            handler.startMultipart(bd);
            
            MimeBoundaryInputStream tempIs = 
                new MimeBoundaryInputStream(is, bd.getBoundary());
            handler.preamble(new CloseShieldInputStream(tempIs));
            tempIs.consume();

            while (tempIs.hasMoreParts()) {
                tempIs = new MimeBoundaryInputStream(is, bd.getBoundary());
                parseBodyPart(tempIs);
                tempIs.consume();
                if (tempIs.parentEOF()) {
                    if (log.isWarnEnabled()) {
                        log.warn("Line " + rootStream.getLineNumber() 
                                + ": Body part ended prematurely. "
                                + "Higher level boundary detected or "
                                + "EOF reached.");
                    }
                    break;
                }
            }
            
            handler.epilogue(new CloseShieldInputStream(is));
            
            handler.endMultipart();
            
            bodyDescriptors.removeFirst();
            
        } else if (bd.isMessage()) {
            if (bd.isBase64Encoded()) {
                log.warn("base64 encoded message/rfc822 detected");
                is = new EOLConvertingInputStream(
                        new Base64InputStream(is));
            } else if (bd.isQuotedPrintableEncoded()) {
                log.warn("quoted-printable encoded message/rfc822 detected");
                is = new EOLConvertingInputStream(
                        new QuotedPrintableInputStream(is));
            }
            bodyDescriptors.addFirst(bd);
            parseMessage(is);
            bodyDescriptors.removeFirst();
        } else {
            handler.body(bd, new CloseShieldInputStream(is));
        }
        
        /*
         * Make sure the stream has been consumed.
         */
        while (is.read() != -1) {
        }
    }
    
    private void parseMessage(InputStream is) throws IOException {
        if (raw) {
            handler.raw(new CloseShieldInputStream(is));
        } else {
            handler.startMessage();
            parseEntity(is);
            handler.endMessage();
        }
    }
    
    private void parseBodyPart(InputStream is) throws IOException {
        if (raw) {
            handler.raw(new CloseShieldInputStream(is));
        } else {
            handler.startBodyPart();
            parseEntity(is);
            handler.endBodyPart();
        }
    }
    
    /**
     * Parses a header.
     * 
     * @param is the stream to parse.
     * @return a <code>BodyDescriptor</code> describing the body following 
     *         the header.
     */
    private BodyDescriptor parseHeader(InputStream is) throws IOException {
        BodyDescriptor bd = new BodyDescriptor(bodyDescriptors.isEmpty() 
                        ? null : (BodyDescriptor) bodyDescriptors.getFirst());
        
        handler.startHeader();
        
        int lineNumber = rootStream.getLineNumber();
        
        StringBuffer sb = new StringBuffer();
        int curr = 0;
        int prev = 0;
        while ((curr = is.read()) != -1) {
            if (curr == '\n' && (prev == '\n' || prev == 0)) {
                /*
                 * [\r]\n[\r]\n or an immediate \r\n have been seen.
                 */
                sb.deleteCharAt(sb.length() - 1);
                break;
            }
            sb.append((char) curr);
            prev = curr == '\r' ? prev : curr;
        }
        
        if (curr == -1 && log.isWarnEnabled()) {
            log.warn("Line " + rootStream.getLineNumber()  
                    + ": Unexpected end of headers detected. "
                    + "Boundary detected in header or EOF reached.");
        }

        int start = 0;
        int pos = 0;
        int startLineNumber = lineNumber;
        while (pos < sb.length()) {
            while (pos < sb.length() && sb.charAt(pos) != '\r') {
                pos++;
            }
            if (pos < sb.length() - 1 && sb.charAt(pos + 1) != '\n') {
                pos++;
                continue;
            }
            
            if (pos >= sb.length() - 2 || fieldChars.get(sb.charAt(pos + 2))) {
                
                /*
                 * field should be the complete field data excluding the 
                 * trailing \r\n.
                 */
                String field = sb.substring(start, pos);
                start = pos + 2;
                
                /*
                 * Check for a valid field.
                 */
                int index = field.indexOf(':');
                boolean valid = false;
                if (index != -1 && fieldChars.get(field.charAt(0))) {
                    valid = true;
                    String fieldName = field.substring(0, index).trim();
                    for (int i = 0; i < fieldName.length(); i++) {
                        if (!fieldChars.get(fieldName.charAt(i))) {
                            valid = false;
                            break;
                        }
                    }
                    
                    if (valid) {
                        handler.field(field);
                        bd.addField(fieldName, field.substring(index + 1));
                    }                        
                }
                
                if (!valid && log.isWarnEnabled()) {
                    log.warn("Line " + startLineNumber 
                            + ": Ignoring invalid field: '" + field.trim() + "'");
                }          
                
                startLineNumber = lineNumber;
            }
            
            pos += 2;
            lineNumber++;
        }
        
        handler.endHeader();
        
        return bd;
    }
    
    /**
     * Sets the <code>ContentHandler</code> to use when reporting 
     * parsing events.
     * 
     * @param h the <code>ContentHandler</code>.
     */
    public void setContentHandler(ContentHandler h) {
        this.handler = h;
    }

}
