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

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;

/**
 * A functional, light weight {@link Reader} that emulates
 * a reader of a specified size.
 * <p>
 * This implementation provides a light weight
 * object for testing with an {@link Reader}
 * where the contents don't matter.
 * <p>
 * One use case would be for testing the handling of
 * large {@link Reader} as it can emulate that
 * scenario without the overhead of actually processing
 * large numbers of characters - significantly speeding up
 * test execution times.
 * <p>
 * This implementation returns a space from the method that
 * reads a character and leaves the array unchanged in the read
 * methods that are passed a character array.
 * If alternative data is required the <code>processChar()</code> and
 * <code>processChars()</code> methods can be implemented to generate
 * data, for example:
 *
 * <pre>
 *  public class TestReader extends NullReader {
 *      public TestReader(int size) {
 *          super(size);
 *      }
 *      protected char processChar() {
 *          return ... // return required value here
 *      }
 *      protected void processChars(char[] chars, int offset, int length) {
 *          for (int i = offset; i < length; i++) {
 *              chars[i] = ... // set array value here
 *          }
 *      }
 *  }
 * </pre>
 *
 * @since Commons IO 1.3
 * @version $Revision: 463529 $
 */
public class NullReader extends Reader {

    private long size;
    private long position;
    private long mark = -1;
    private long readlimit;
    private boolean eof;
    private boolean throwEofException;
    private boolean markSupported;

    /**
     * Create a {@link Reader} that emulates a specified size
     * which supports marking and does not throw EOFException.
     *
     * @param size The size of the reader to emulate.
     */
    public NullReader(long size) {
       this(size, true, false);
    }

    /**
     * Create a {@link Reader} that emulates a specified
     * size with option settings.
     *
     * @param size The size of the reader to emulate.
     * @param markSupported Whether this instance will support
     * the <code>mark()</code> functionality.
     * @param throwEofException Whether this implementation
     * will throw an {@link EOFException} or return -1 when the
     * end of file is reached.
     */
    public NullReader(long size, boolean markSupported, boolean throwEofException) {
       this.size = size;
       this.markSupported = markSupported;
       this.throwEofException = throwEofException;
    }

    /**
     * Return the current position.
     *
     * @return the current position.
     */
    public long getPosition() {
        return position;
    }

    /**
     * Return the size this {@link Reader} emulates.
     *
     * @return The size of the reader to emulate.
     */
    public long getSize() {
        return size;
    }

    /**
     * Close this Reader - resets the internal state to
     * the initial values.
     *
     * @throws IOException If an error occurs.
     */
    public void close() throws IOException {
        eof = false;
        position = 0;
        mark = -1;
    }

    /**
     * Mark the current position.
     *
     * @param readlimit The number of characters before this marked position
     * is invalid.
     * @throws UnsupportedOperationException if mark is not supported.
     */
    public synchronized void mark(int readlimit) {
        if (!markSupported) {
            throw new UnsupportedOperationException("Mark not supported");
        }
        mark = position;
        this.readlimit = readlimit;
    }

    /**
     * Indicates whether <i>mark</i> is supported.
     *
     * @return Whether <i>mark</i> is supported or not.
     */
    public boolean markSupported() {
        return markSupported;
    }

    /**
     * Read a character.
     *
     * @return Either The character value returned by <code>processChar()</code>
     * or <code>-1</code> if the end of file has been reached and
     * <code>throwEofException</code> is set to <code>false</code>.
     * @throws EOFException if the end of file is reached and
     * <code>throwEofException</code> is set to <code>true</code>.
     * @throws IOException if trying to read past the end of file.
     */
    public int read() throws IOException {
        if (eof) {
            throw new IOException("Read after end of file");
        }
        if (position == size) {
            return doEndOfFile();
        }
        position++;
        return processChar();
    }

    /**
     * Read some characters into the specified array.
     *
     * @param chars The character array to read into
     * @return The number of characters read or <code>-1</code>
     * if the end of file has been reached and
     * <code>throwEofException</code> is set to <code>false</code>.
     * @throws EOFException if the end of file is reached and
     * <code>throwEofException</code> is set to <code>true</code>.
     * @throws IOException if trying to read past the end of file.
     */
    public int read(char[] chars) throws IOException {
        return read(chars, 0, chars.length);
    }

    /**
     * Read the specified number characters into an array.
     *
     * @param chars The character array to read into.
     * @param offset The offset to start reading characters into.
     * @param length The number of characters to read.
     * @return The number of characters read or <code>-1</code>
     * if the end of file has been reached and
     * <code>throwEofException</code> is set to <code>false</code>.
     * @throws EOFException if the end of file is reached and
     * <code>throwEofException</code> is set to <code>true</code>.
     * @throws IOException if trying to read past the end of file.
     */
    public int read(char[] chars, int offset, int length) throws IOException {
        if (eof) {
            throw new IOException("Read after end of file");
        }
        if (position == size) {
            return doEndOfFile();
        }
        position += length;
        int returnLength = length;
        if (position > size) {
            returnLength = length - (int)(position - size);
            position = size;
        }
        processChars(chars, offset, returnLength);
        return returnLength;
    }

    /**
     * Reset the stream to the point when mark was last called.
     *
     * @throws UnsupportedOperationException if mark is not supported.
     * @throws IOException If no position has been marked
     * or the read limit has been exceed since the last position was
     * marked.
     */
    public synchronized void reset() throws IOException {
        if (!markSupported) {
            throw new UnsupportedOperationException("Mark not supported");
        }
        if (mark < 0) {
            throw new IOException("No position has been marked");
        }
        if (position > (mark + readlimit)) {
            throw new IOException("Marked position [" + mark +
                    "] is no longer valid - passed the read limit [" +
                    readlimit + "]");
        }
        position = mark;
        eof = false;
    }

    /**
     * Skip a specified number of characters.
     *
     * @param numberOfChars The number of characters to skip.
     * @return The number of characters skipped or <code>-1</code>
     * if the end of file has been reached and
     * <code>throwEofException</code> is set to <code>false</code>.
     * @throws EOFException if the end of file is reached and
     * <code>throwEofException</code> is set to <code>true</code>.
     * @throws IOException if trying to read past the end of file.
     */
    public long skip(long numberOfChars) throws IOException {
        if (eof) {
            throw new IOException("Skip after end of file");
        }
        if (position == size) {
            return doEndOfFile();
        }
        position += numberOfChars;
        long returnLength = numberOfChars;
        if (position > size) {
            returnLength = numberOfChars - (position - size);
            position = size;
        }
        return returnLength;
    }

    /**
     * Return a character value for the  <code>read()</code> method.
     * <p>
     * This implementation returns zero.
     *
     * @return This implementation always returns zero.
     */
    protected int processChar() {
        // do nothing - overridable by subclass
        return 0;
    }

    /**
     * Process the characters for the <code>read(char[], offset, length)</code>
     * method.
     * <p>
     * This implementation leaves the character array unchanged.
     *
     * @param chars The character array
     * @param offset The offset to start at.
     * @param length The number of characters.
     */
    protected void processChars(char[] chars, int offset, int length) {
        // do nothing - overridable by subclass
    }

    /**
     * Handle End of File.
     *
     * @return <code>-1</code> if <code>throwEofException</code> is
     * set to <code>false</code>
     * @throws EOFException if <code>throwEofException</code> is set
     * to <code>true</code>.
     */
    private int doEndOfFile() throws EOFException {
        eof = true;
        if (throwEofException) {
            throw new EOFException();
        }
        return -1;
    }

}
