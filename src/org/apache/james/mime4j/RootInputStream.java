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
 * <code>InputStream</code> used by the parser to wrap the original user
 * supplied stream. This stream keeps track of the current line number and
 * can also be truncated. When truncated the stream will appear to have
 * reached end of file. This is used by the parser's 
 * {@link org.apache.james.mime4j.MimeStreamParser#stop()} method.
 *
 * 
 * @version $Id: RootInputStream.java,v 1.2 2004/10/02 12:41:10 ntherning Exp $
 */
class RootInputStream extends InputStream {
    private InputStream is = null;
    private int lineNumber = 1;
    private int prev = -1;
    private boolean truncated = false;

    /**
     * Creates a new <code>RootInputStream</code>.
     * 
     * @param in the stream to read from.
     */
    public RootInputStream(InputStream is) {
        this.is = is;
    }

    /**
     * Gets the current line number starting at 1 
     * (the number of <code>\r\n</code> read so far plus 1).
     * 
     * @return the current line number.
     */
    public int getLineNumber() {
        return lineNumber;
    }
    
    /**
     * Truncates this <code>InputStream</code>. After this call any 
     * call to {@link #read()}, {@link #read(byte[]) or 
     * {@link #read(byte[], int, int)} will return
     * -1 as if end-of-file had been reached.
     */
    public void truncate() {
        this.truncated = true;
    }
    
    /**
     * @see java.io.InputStream#read()
     */
    public int read() throws IOException {
        if (truncated) {
            return -1;
        }
        
        int b = is.read();
        if (prev == '\r' && b == '\n') {
            lineNumber++;
        }
        prev = b;
        return b;
    }
    
    /**
     * 
     * @see java.io.InputStream#read(byte[], int, int)
     */
    public int read(byte[] b, int off, int len) throws IOException {
        if (truncated) {
            return -1;
        }
        
        int n = is.read(b, off, len);
        for (int i = off; i < off + n; i++) {
            if (prev == '\r' && b[i] == '\n') {
                lineNumber++;
            }
            prev = b[i];
        }
        return n;
    }
    
    /**
     * @see java.io.InputStream#read(byte[])
     */
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }
}
