/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.io.input;

import java.io.IOException;
import java.io.InputStream;

/**
 * A decorating input stream that counts the number of bytes that have passed
 * through the stream so far.
 * <p>
 * A typical use case would be during debugging, to ensure that data is being
 * read as expected.
 *
 * @author Marcelo Liberato
 * @version $Id: CountingInputStream.java 471628 2006-11-06 04:06:45Z bayard $
 */
public class CountingInputStream extends ProxyInputStream {

    /** The count of bytes that have passed. */
    private long count;

    /**
     * Constructs a new CountingInputStream.
     *
     * @param in  the InputStream to delegate to
     */
    public CountingInputStream(InputStream in) {
        super(in);
    }

    //-----------------------------------------------------------------------
    /**
     * Reads a number of bytes into the byte array, keeping count of the
     * number read.
     *
     * @param b  the buffer into which the data is read, not null
     * @return the total number of bytes read into the buffer, -1 if end of stream
     * @throws IOException if an I/O error occurs
     * @see java.io.InputStream#read(byte[]) 
     */
    public int read(byte[] b) throws IOException {
        int found = super.read(b);
        this.count += (found >= 0) ? found : 0;
        return found;
    }

    /**
     * Reads a number of bytes into the byte array at a specific offset,
     * keeping count of the number read.
     *
     * @param b  the buffer into which the data is read, not null
     * @param off  the start offset in the buffer
     * @param len  the maximum number of bytes to read
     * @return the total number of bytes read into the buffer, -1 if end of stream
     * @throws IOException if an I/O error occurs
     * @see java.io.InputStream#read(byte[], int, int)
     */
    public int read(byte[] b, int off, int len) throws IOException {
        int found = super.read(b, off, len);
        this.count += (found >= 0) ? found : 0;
        return found;
    }

    /**
     * Reads the next byte of data adding to the count of bytes received
     * if a byte is successfully read. 
     *
     * @return the byte read, -1 if end of stream
     * @throws IOException if an I/O error occurs
     * @see java.io.InputStream#read()
     */
    public int read() throws IOException {
        int found = super.read();
        this.count += (found >= 0) ? 1 : 0;
        return found;
    }

    /**
     * Skips the stream over the specified number of bytes, adding the skipped
     * amount to the count.
     *
     * @param length  the number of bytes to skip
     * @return the actual number of bytes skipped
     * @throws IOException if an I/O error occurs
     * @see java.io.InputStream#skip(long)
     */
    public long skip(final long length) throws IOException {
        final long skip = super.skip(length);
        this.count += skip;
        return skip;
    }

    //-----------------------------------------------------------------------
    /**
     * The number of bytes that have passed through this stream.
     * <p>
     * NOTE: From v1.3 this method throws an ArithmeticException if the
     * count is greater than can be expressed by an <code>int</code>.
     * See {@link #getByteCount()} for a method using a <code>long</code>.
     *
     * @return the number of bytes accumulated
     * @throws ArithmeticException if the byte count is too large
     */
    public synchronized int getCount() {
        long result = getByteCount();
        if (result > Integer.MAX_VALUE) {
            throw new ArithmeticException("The byte count " + result + " is too large to be converted to an int");
        }
        return (int) result;
    }

    /** 
     * Set the byte count back to 0. 
     * <p>
     * NOTE: From v1.3 this method throws an ArithmeticException if the
     * count is greater than can be expressed by an <code>int</code>.
     * See {@link #resetByteCount()} for a method using a <code>long</code>.
     *
     * @return the count previous to resetting
     * @throws ArithmeticException if the byte count is too large
     */
    public synchronized int resetCount() {
        long result = resetByteCount();
        if (result > Integer.MAX_VALUE) {
            throw new ArithmeticException("The byte count " + result + " is too large to be converted to an int");
        }
        return (int) result;
    }

    /**
     * The number of bytes that have passed through this stream.
     * <p>
     * NOTE: This method is an alternative for <code>getCount()</code>
     * and was added because that method returns an integer which will
     * result in incorrect count for files over 2GB.
     *
     * @return the number of bytes accumulated
     * @since Commons IO 1.3
     */
    public synchronized long getByteCount() {
        return this.count;
    }

    /** 
     * Set the byte count back to 0. 
     * <p>
     * NOTE: This method is an alternative for <code>resetCount()</code>
     * and was added because that method returns an integer which will
     * result in incorrect count for files over 2GB.
     *
     * @return the count previous to resetting
     * @since Commons IO 1.3
     */
    public synchronized long resetByteCount() {
        long tmp = this.count;
        this.count = 0;
        return tmp;
    }

}
