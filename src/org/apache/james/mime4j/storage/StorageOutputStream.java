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

package org.apache.james.mime4j.storage;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This class implements an output stream that can be used to create a
 * {@link Storage} object. An instance of this class is obtained by calling
 * {@link StorageProvider#createStorageOutputStream()}. The user can then write
 * data to this instance and invoke {@link #toStorage()} to retrieve a
 * {@link Storage} object that contains the data that has been written.
 * <p>
 * Note that the <code>StorageOutputStream</code> does not have to be closed
 * explicitly because {@link #toStorage()} invokes {@link #close()} if
 * necessary. Also note that {@link #toStorage()} may be invoked only once. One
 * <code>StorageOutputStream</code> can create only one <code>Storage</code>
 * instance.
 */
public abstract class StorageOutputStream extends OutputStream {

    private byte[] singleByte;
    private boolean closed;
    private boolean usedUp;

    /**
     * Sole constructor.
     */
    protected StorageOutputStream() {
    }

    /**
     * Closes this output stream if it has not already been closed and returns a
     * {@link Storage} object which contains the bytes that have been written to
     * this output stream.
     * <p>
     * Note that this method may not be invoked a second time. This is because
     * for some implementations it is not possible to create another
     * <code>Storage</code> object that can be read from and deleted
     * independently (e.g. if the implementation writes to a file).
     *
     * @return a <code>Storage</code> object as described above.
     * @throws IOException
     *             if an I/O error occurs.
     * @throws IllegalStateException
     *             if this method has already been called.
     */
    public final Storage toStorage() throws IOException {
        if (usedUp)
            throw new IllegalStateException(
                    "toStorage may be invoked only once");

        if (!closed)
            close();

        usedUp = true;
        return toStorage0();
    }

    @Override
    public final void write(int b) throws IOException {
        if (closed)
            throw new IOException("StorageOutputStream has been closed");

        if (singleByte == null)
            singleByte = new byte[1];

        singleByte[0] = (byte) b;
        write0(singleByte, 0, 1);
    }

    @Override
    public final void write(byte[] buffer) throws IOException {
        if (closed)
            throw new IOException("StorageOutputStream has been closed");

        if (buffer == null)
            throw new NullPointerException();

        if (buffer.length == 0)
            return;

        write0(buffer, 0, buffer.length);
    }

    @Override
    public final void write(byte[] buffer, int offset, int length)
            throws IOException {
        if (closed)
            throw new IOException("StorageOutputStream has been closed");

        if (buffer == null)
            throw new NullPointerException();

        if (offset < 0 || length < 0 || offset + length > buffer.length)
            throw new IndexOutOfBoundsException();

        if (length == 0)
            return;

        write0(buffer, offset, length);
    }

    /**
     * Closes this output stream. Subclasses that override this method have to
     * invoke <code>super.close()</code>.
     * <p>
     * This implementation never throws an {@link IOException} but a subclass
     * might.
     *
     * @throws IOException
     *             if an I/O error occurs.
     */
    @Override
    public void close() throws IOException {
        closed = true;
    }

    /**
     * Has to implemented by a concrete subclass to write bytes from the given
     * byte array to this <code>StorageOutputStream</code>. This method gets
     * called by {@link #write(int)}, {@link #write(byte[])} and
     * {@link #write(byte[], int, int)}. All the required preconditions have
     * already been checked by these methods, including the check if the output
     * stream has already been closed.
     *
     * @param buffer
     *            buffer containing bytes to write.
     * @param offset
     *            start offset in the buffer.
     * @param length
     *            number of bytes to write.
     * @throws IOException
     *             if an I/O error occurs.
     */
    protected abstract void write0(byte[] buffer, int offset, int length)
            throws IOException;

    /**
     * Has to be implemented by a concrete subclass to create a {@link Storage}
     * object from the bytes that have been written to this
     * <code>StorageOutputStream</code>. This method gets called by
     * {@link #toStorage()} after the preconditions have been checked. The
     * implementation can also be sure that this methods gets invoked only once.
     *
     * @return a <code>Storage</code> object as described above.
     * @throws IOException
     *             if an I/O error occurs.
     */
    protected abstract Storage toStorage0() throws IOException;

}
