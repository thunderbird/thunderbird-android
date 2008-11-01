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

/**
 * <p>
 * Receives notifications of the content of a plain RFC822 or MIME message.
 * Implement this interface and register an instance of that implementation
 * with a <code>MimeStreamParser</code> instance using its 
 * {@link org.apache.james.mime4j.MimeStreamParser#setContentHandler(ContentHandler)}
 * method. The parser uses the <code>ContentHandler</code> instance to report
 * basic message-related events like the start and end of the body of a
 * part in a multipart MIME entity.
 * </p>
 * <p>
 * Events will be generated in the order the corresponding elements occur in
 * the message stream parsed by the parser. E.g.:
 * <pre>
 *      startMessage()
 *          startHeader()
 *              field(...)
 *              field(...)
 *              ...
 *          endHeader()
 *          startMultipart()
 *              preamble(...)
 *              startBodyPart()
 *                  startHeader()
 *                      field(...)
 *                      field(...)
 *                      ...
 *                  endHeader()
 *                  body()
 *              endBodyPart()
 *              startBodyPart()
 *                  startHeader()
 *                      field(...)
 *                      field(...)
 *                      ...
 *                  endHeader()
 *                  body()
 *              endBodyPart()
 *              epilogue(...)
 *          endMultipart()
 *      endMessage()
 * </pre>
 * The above shows an example of a MIME message consisting of a multipart
 * body containing two body parts.
 * </p>
 * <p>
 * See MIME RFCs 2045-2049 for more information on the structure of MIME 
 * messages and RFC 822 and 2822 for the general structure of Internet mail
 * messages.
 * </p>
 *
 * 
 * @version $Id: ContentHandler.java,v 1.3 2004/10/02 12:41:10 ntherning Exp $
 */
public interface ContentHandler {
    /**
     * Called when a new message starts (a top level message or an embedded 
     * rfc822 message).
     */
    void startMessage();
    
    /**
     * Called when a message ends.
     */
    void endMessage();
    
    /**
     * Called when a new body part starts inside a
     * <code>multipart/*</code> entity.
     */
    void startBodyPart();
    
    /**
     * Called when a body part ends.
     */
    void endBodyPart();
    
    /**
     * Called when a header (of a message or body part) is about to be parsed.
     */
    void startHeader();
    
    /**
     * Called for each field of a header.
     * 
     * @param fieldData the raw contents of the field 
     *        (<code>Field-Name: field value</code>). The value will not be 
     *        unfolded.
     */
    void field(String fieldData);
    
    /**
     * Called when there are no more header fields in a message or body part.
     */
    void endHeader();
    
    /**
     * Called for the preamble (whatever comes before the first body part)
     * of a <code>multipart/*</code> entity.
     * 
     * @param is used to get the contents of the preamble.
     * @throws IOException should be thrown on I/O errors.
     */
    void preamble(InputStream is) throws IOException;
    
    /**
     * Called for the epilogue (whatever comes after the final body part) 
     * of a <code>multipart/*</code> entity.
     * 
     * @param is used to get the contents of the epilogue.
     * @throws IOException should be thrown on I/O errors.
     */
    void epilogue(InputStream is) throws IOException;
    
    /**
     * Called when the body of a multipart entity is about to be parsed.
     * 
     * @param bd encapsulates the values (either read from the 
     *        message stream or, if not present, determined implictly 
     *        as described in the 
     *        MIME rfc:s) of the <code>Content-Type</code> and 
     *        <code>Content-Transfer-Encoding</code> header fields.
     */
    void startMultipart(BodyDescriptor bd);
    
    /**
     * Called when the body of an entity has been parsed.
     */
    void endMultipart();
    
    /**
     * Called when the body of a discrete (non-multipart) entity is about to 
     * be parsed.
     * 
     * @param bd see {@link #startMultipart(BodyDescriptor)}
     * @param is the contents of the body. NOTE: this is the raw body contents 
     *           - it will not be decoded if encoded. The <code>bd</code>
     *           parameter should be used to determine how the stream data
     *           should be decoded.
     * @throws IOException should be thrown on I/O errors.
     */
    void body(BodyDescriptor bd, InputStream is) throws IOException;
    
    /**
     * Called when a new entity (message or body part) starts and the 
     * parser is in <code>raw</code> mode.
     * 
     * @param is the raw contents of the entity.
     * @throws IOException should be thrown on I/O errors.
     * @see MimeStreamParser#setRaw(boolean)
     */
    void raw(InputStream is) throws IOException;
}
