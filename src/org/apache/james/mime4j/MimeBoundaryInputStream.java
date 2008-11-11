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
import java.io.PushbackInputStream;

/**
 * Stream that constrains itself to a single MIME body part.
 * After the stream ends (i.e. read() returns -1) {@link #hasMoreParts()}
 * can be used to determine if a final boundary has been seen or not.
 * If {@link #parentEOF()} is <code>true</code> an unexpected end of stream
 * has been detected in the parent stream.
 * 
 * 
 * 
 * @version $Id: MimeBoundaryInputStream.java,v 1.2 2004/11/29 13:15:42 ntherning Exp $
 */
public class MimeBoundaryInputStream extends InputStream {
    
    private PushbackInputStream s = null;
    private byte[] boundary = null;
    private boolean first = true;
    private boolean eof = false;
    private boolean parenteof = false;
    private boolean moreParts = true;

    /**
     * Creates a new MimeBoundaryInputStream.
     * @param s The underlying stream.
     * @param boundary Boundary string (not including leading hyphens).
     */
    public MimeBoundaryInputStream(InputStream s, String boundary) 
            throws IOException {
        
        this.s = new PushbackInputStream(s, boundary.length() + 4);

        boundary = "--" + boundary;
        this.boundary = new byte[boundary.length()];
        for (int i = 0; i < this.boundary.length; i++) {
            this.boundary[i] = (byte) boundary.charAt(i);
        }
        
        /*
         * By reading one byte we will update moreParts to be as expected
         * before any bytes have been read.
         */
        int b = read();
        if (b != -1) {
            this.s.unread(b);
        }
    }

    /**
     * Closes the underlying stream.
     * 
     * @throws IOException on I/O errors.
     */
    public void close() throws IOException {
        s.close();
    }

    /**
     * Determines if the underlying stream has more parts (this stream has
     * not seen an end boundary).
     * 
     * @return <code>true</code> if there are more parts in the underlying 
     *         stream, <code>false</code> otherwise.
     */
    public boolean hasMoreParts() {
        return moreParts;
    }

    /**
     * Determines if the parent stream has reached EOF
     * 
     * @return <code>true</code>  if EOF has been reached for the parent stream, 
     *         <code>false</code> otherwise.
     */
    public boolean parentEOF() {
        return parenteof;
    }
    
    /**
     * Consumes all unread bytes of this stream. After a call to this method
     * this stream will have reached EOF.
     * 
     * @throws IOException on I/O errors.
     */
    public void consume() throws IOException {
        while (read() != -1) {
        }
    }
    
    /**
     * @see java.io.InputStream#read()
     */
    public int read() throws IOException {
        if (eof) {
            return -1;
        }
        
        if (first) {
            first = false;
            if (matchBoundary()) {
                return -1;
            }
        }
        
        int b1 = s.read();
        int b2 = s.read();
        
        if (b1 == '\r' && b2 == '\n') {
            if (matchBoundary()) {
                return -1;
            }
        }
        
        if (b2 != -1) {
            s.unread(b2);
        }

        parenteof = b1 == -1;
        eof = parenteof;
        
        return b1;
    }
    
    private boolean matchBoundary() throws IOException {
        
        for (int i = 0; i < boundary.length; i++) {
            int b = s.read();
            if (b != boundary[i]) {
                if (b != -1) {
                    s.unread(b);
                }
                for (int j = i - 1; j >= 0; j--) {
                    s.unread(boundary[j]);
                }
                return false;
            }
        }
        
        /*
         * We have a match. Is it an end boundary?
         */
        int prev = s.read();
        int curr = s.read();
        moreParts = !(prev == '-' && curr == '-');
        do {
            if (curr == '\n' && prev == '\r') {
                break;
            }
            prev = curr;
        } while ((curr = s.read()) != -1);
        
        if (curr == -1) {
            moreParts = false;
            parenteof = true;
        }
        
        eof = true;
        
        return true;
    }
}
