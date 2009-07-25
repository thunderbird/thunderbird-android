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
package org.apache.commons.io.output;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A decorating output stream that counts the number of bytes that have passed
 * through the stream so far.
 * <p>
 * A typical use case would be during debugging, to ensure that data is being
 * written as expected.
 *
 * @version $Id: CountingOutputStream.java 471628 2006-11-06 04:06:45Z bayard $
 */
public class CountingOutputStream extends ProxyOutputStream {

    /** The count of bytes that have passed. */
    private long count;

    /**
     * Constructs a new CountingOutputStream.
     * 
     * @param out  the OutputStream to write to
     */
    public CountingOutputStream( OutputStream out ) {
        super(out);
    }

    //-----------------------------------------------------------------------
    /**
     * Writes the contents of the specified byte array to this output stream
     * keeping count of the number of bytes written.
     *
     * @param b  the bytes to write, not null
     * @throws IOException if an I/O error occurs
     * @see java.io.OutputStream#write(byte[])
     */
    public void write(byte[] b) throws IOException {
        count += b.length;
        super.write(b);
    }

    /**
     * Writes a portion of the specified byte array to this output stream
     * keeping count of the number of bytes written.
     *
     * @param b  the bytes to write, not null
     * @param off  the start offset in the buffer
     * @param len  the maximum number of bytes to write
     * @throws IOException if an I/O error occurs
     * @see java.io.OutputStream#write(byte[], int, int)
     */
    public void write(byte[] b, int off, int len) throws IOException {
        count += len;
        super.write(b, off, len);
    }

    /**
     * Writes a single byte to the output stream adding to the count of the
     * number of bytes written.
     *
     * @param b  the byte to write
     * @throws IOException if an I/O error occurs
     * @see java.io.OutputStream#write(int)
     */
    public void write(int b) throws IOException {
        count++;
        super.write(b);
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
     * NOTE: This method is an alternative for <code>getCount()</code>.
     * It was added because that method returns an integer which will
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
     * NOTE: This method is an alternative for <code>resetCount()</code>.
     * It was added because that method returns an integer which will
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
