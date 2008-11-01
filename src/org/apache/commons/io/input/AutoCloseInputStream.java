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
 * Proxy stream that closes and discards the underlying stream as soon as the
 * end of input has been reached or when the stream is explicitly closed.
 * Not even a reference to the underlying stream is kept after it has been
 * closed, so any allocated in-memory buffers can be freed even if the
 * client application still keeps a reference to the proxy stream.
 * <p>
 * This class is typically used to release any resources related to an open
 * stream as soon as possible even if the client application (by not explicitly
 * closing the stream when no longer needed) or the underlying stream (by not
 * releasing resources once the last byte has been read) do not do that.
 *
 * @version $Id: AutoCloseInputStream.java 610010 2008-01-08 14:50:59Z niallp $
 * @since Commons IO 1.4
 */
public class AutoCloseInputStream extends ProxyInputStream {

    /**
     * Creates an automatically closing proxy for the given input stream.
     *
     * @param in underlying input stream
     */
    public AutoCloseInputStream(InputStream in) {
        super(in);
    }

    /**
     * Closes the underlying input stream and replaces the reference to it
     * with a {@link ClosedInputStream} instance.
     * <p>
     * This method is automatically called by the read methods when the end
     * of input has been reached.
     * <p>
     * Note that it is safe to call this method any number of times. The original
     * underlying input stream is closed and discarded only once when this
     * method is first called.
     *
     * @throws IOException if the underlying input stream can not be closed
     */
    public void close() throws IOException {
        in.close();
        in = new ClosedInputStream();
    }

    /**
     * Reads and returns a single byte from the underlying input stream.
     * If the underlying stream returns -1, the {@link #close()} method is
     * called to automatically close and discard the stream.
     *
     * @return next byte in the stream, or -1 if no more bytes are available
     * @throws IOException if the stream could not be read or closed
     */
    public int read() throws IOException {
        int n = in.read();
        if (n == -1) {
            close();
        }
        return n;
    }

    /**
     * Reads and returns bytes from the underlying input stream to the given
     * buffer. If the underlying stream returns -1, the {@link #close()} method
     * i called to automatically close and discard the stream.
     *
     * @param b buffer to which bytes from the stream are written
     * @return number of bytes read, or -1 if no more bytes are available
     * @throws IOException if the stream could not be read or closed
     */
    public int read(byte[] b) throws IOException {
        int n = in.read(b);
        if (n == -1) {
            close();
        }
        return n;
    }

    /**
     * Reads and returns bytes from the underlying input stream to the given
     * buffer. If the underlying stream returns -1, the {@link #close()} method
     * i called to automatically close and discard the stream.
     *
     * @param b buffer to which bytes from the stream are written
     * @param off start offset within the buffer
     * @param len maximum number of bytes to read
     * @return number of bytes read, or -1 if no more bytes are available
     * @throws IOException if the stream could not be read or closed
     */
    public int read(byte[] b, int off, int len) throws IOException {
        int n = in.read(b, off, len);
        if (n == -1) {
            close();
        }
        return n;
    }

    /**
     * Ensures that the stream is closed before it gets garbage-collected.
     * As mentioned in {@link #close()}, this is a no-op if the stream has
     * already been closed.
     * @throws Throwable if an error occurs
     */
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

}
