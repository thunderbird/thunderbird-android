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
 * Abstract <code>ContentHandler</code> with default implementations of all
 * the methods of the <code>ContentHandler</code> interface.
 * 
 * The default is to todo nothing.
 *
 * 
 * @version $Id: AbstractContentHandler.java,v 1.3 2004/10/02 12:41:10 ntherning Exp $
 */
public abstract class AbstractContentHandler implements ContentHandler {
    
    /**
     * @see org.apache.james.mime4j.ContentHandler#endMultipart()
     */
    public void endMultipart() {
    }
    
    /**
     * @see org.apache.james.mime4j.ContentHandler#startMultipart(org.apache.james.mime4j.BodyDescriptor)
     */
    public void startMultipart(BodyDescriptor bd) {
    }
    
    /**
     * @see org.apache.james.mime4j.ContentHandler#body(org.apache.james.mime4j.BodyDescriptor, java.io.InputStream)
     */
    public void body(BodyDescriptor bd, InputStream is) throws IOException {
    }
    
    /**
     * @see org.apache.james.mime4j.ContentHandler#endBodyPart()
     */
    public void endBodyPart() {
    }
    
    /**
     * @see org.apache.james.mime4j.ContentHandler#endHeader()
     */
    public void endHeader() {
    }
    
    /**
     * @see org.apache.james.mime4j.ContentHandler#endMessage()
     */
    public void endMessage() {
    }
    
    /**
     * @see org.apache.james.mime4j.ContentHandler#epilogue(java.io.InputStream)
     */
    public void epilogue(InputStream is) throws IOException {
    }
    
    /**
     * @see org.apache.james.mime4j.ContentHandler#field(java.lang.String)
     */
    public void field(String fieldData) {
    }
    
    /**
     * @see org.apache.james.mime4j.ContentHandler#preamble(java.io.InputStream)
     */
    public void preamble(InputStream is) throws IOException {
    }
    
    /**
     * @see org.apache.james.mime4j.ContentHandler#startBodyPart()
     */
    public void startBodyPart() {
    }
    
    /**
     * @see org.apache.james.mime4j.ContentHandler#startHeader()
     */
    public void startHeader() {
    }
    
    /**
     * @see org.apache.james.mime4j.ContentHandler#startMessage()
     */
    public void startMessage() {
    }
    
    /**
     * @see org.apache.james.mime4j.ContentHandler#raw(java.io.InputStream)
     */
    public void raw(InputStream is) throws IOException {
    }
}
