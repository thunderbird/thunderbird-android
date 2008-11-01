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

import java.io.InputStream;
import java.io.IOException;

/**
 * InputStream that shields its underlying input stream from
 * being closed.
 * 
 * 
 * @version $Id: CloseShieldInputStream.java,v 1.2 2004/10/02 12:41:10 ntherning Exp $
 */
public class CloseShieldInputStream extends InputStream {

    /**
     * Underlying InputStream
     */
    private InputStream is;

    public CloseShieldInputStream(InputStream is) {
        this.is = is;
    }

    public InputStream getUnderlyingStream() {
        return is;
    }

    /**
     * @see java.io.InputStream#read()
     */
    public int read() throws IOException {
        checkIfClosed();
        return is.read();
    }

    /**
     * @see java.io.InputStream#available()
     */
    public int available() throws IOException {
        checkIfClosed();
        return is.available();
    }

    
    /**
     * Set the underlying InputStream to null
     */
    public void close() throws IOException {
        is = null;
    }

    /**
     * @see java.io.FilterInputStream#reset()
     */
    public synchronized void reset() throws IOException {
        checkIfClosed();
        is.reset();
    }

    /**
     * @see java.io.FilterInputStream#markSupported()
     */
    public boolean markSupported() {
        if (is == null)
            return false;
        return is.markSupported();
    }

    /**
     * @see java.io.FilterInputStream#mark(int)
     */
    public synchronized void mark(int readlimit) {
        if (is != null)
            is.mark(readlimit);
    }

    /**
     * @see java.io.FilterInputStream#skip(long)
     */
    public long skip(long n) throws IOException {
        checkIfClosed();
        return is.skip(n);
    }

    /**
     * @see java.io.FilterInputStream#read(byte[])
     */
    public int read(byte b[]) throws IOException {
        checkIfClosed();
        return is.read(b);
    }

    /**
     * @see java.io.FilterInputStream#read(byte[], int, int)
     */
    public int read(byte b[], int off, int len) throws IOException {
        checkIfClosed();
        return is.read(b, off, len);
    }

    /**
     * Check if the underlying InputStream is null. If so throw an Exception
     * 
     * @throws IOException if the underlying InputStream is null
     */
    private void checkIfClosed() throws IOException {
        if (is == null)
            throw new IOException("Stream is closed");
    }
}
