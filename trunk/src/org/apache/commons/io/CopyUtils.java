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
package org.apache.commons.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

/**
 * This class provides static utility methods for buffered
 * copying between sources (<code>InputStream</code>, <code>Reader</code>,
 * <code>String</code> and <code>byte[]</code>) and destinations
 * (<code>OutputStream</code>, <code>Writer</code>, <code>String</code> and
 * <code>byte[]</code>).
 * <p>
 * Unless otherwise noted, these <code>copy</code> methods do <em>not</em>
 * flush or close the streams. Often doing so would require making non-portable
 * assumptions about the streams' origin and further use. This means that both
 * streams' <code>close()</code> methods must be called after copying. if one
 * omits this step, then the stream resources (sockets, file descriptors) are
 * released when the associated Stream is garbage-collected. It is not a good
 * idea to rely on this mechanism. For a good overview of the distinction
 * between "memory management" and "resource management", see
 * <a href="http://www.unixreview.com/articles/1998/9804/9804ja/ja.htm">this
 * UnixReview article</a>.
 * <p>
 * For byte-to-char methods, a <code>copy</code> variant allows the encoding
 * to be selected (otherwise the platform default is used). We would like to
 * encourage you to always specify the encoding because relying on the platform
 * default can lead to unexpected results.
 * <p
 * We don't provide special variants for the <code>copy</code> methods that
 * let you specify the buffer size because in modern VMs the impact on speed
 * seems to be minimal. We're using a default buffer size of 4 KB.
 * <p>
 * The <code>copy</code> methods use an internal buffer when copying. It is
 * therefore advisable <em>not</em> to deliberately wrap the stream arguments
 * to the <code>copy</code> methods in <code>Buffered*</code> streams. For
 * example, don't do the following:
 * <pre>
 *  copy( new BufferedInputStream( in ), new BufferedOutputStream( out ) );
 *  </pre>
 * The rationale is as follows:
 * <p>
 * Imagine that an InputStream's read() is a very expensive operation, which
 * would usually suggest wrapping in a BufferedInputStream. The
 * BufferedInputStream works by issuing infrequent
 * {@link java.io.InputStream#read(byte[] b, int off, int len)} requests on the
 * underlying InputStream, to fill an internal buffer, from which further
 * <code>read</code> requests can inexpensively get their data (until the buffer
 * runs out).
 * <p>
 * However, the <code>copy</code> methods do the same thing, keeping an
 * internal buffer, populated by
 * {@link InputStream#read(byte[] b, int off, int len)} requests. Having two
 * buffers (or three if the destination stream is also buffered) is pointless,
 * and the unnecessary buffer management hurts performance slightly (about 3%,
 * according to some simple experiments).
 * <p>
 * Behold, intrepid explorers; a map of this class:
 * <pre>
 *       Method      Input               Output          Dependency
 *       ------      -----               ------          -------
 * 1     copy        InputStream         OutputStream    (primitive)
 * 2     copy        Reader              Writer          (primitive)
 *
 * 3     copy        InputStream         Writer          2
 *
 * 4     copy        Reader              OutputStream    2
 *
 * 5     copy        String              OutputStream    2
 * 6     copy        String              Writer          (trivial)
 *
 * 7     copy        byte[]              Writer          3
 * 8     copy        byte[]              OutputStream    (trivial)
 * </pre>
 * <p>
 * Note that only the first two methods shuffle bytes; the rest use these
 * two, or (if possible) copy using native Java copy methods. As there are
 * method variants to specify the encoding, each row may
 * correspond to up to 2 methods.
 * <p>
 * Origin of code: Excalibur.
 *
 * @author Peter Donald
 * @author Jeff Turner
 * @author Matthew Hawthorne
 * @version $Id: CopyUtils.java 437680 2006-08-28 11:57:00Z scolebourne $
 * @deprecated Use IOUtils. Will be removed in 2.0.
 *  Methods renamed to IOUtils.write() or IOUtils.copy().
 *  Null handling behaviour changed in IOUtils (null data does not
 *  throw NullPointerException).
 */
public class CopyUtils {

    /**
     * The default size of the buffer.
     */
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    /**
     * Instances should NOT be constructed in standard programming.
     */
    public CopyUtils() { }

    // ----------------------------------------------------------------
    // byte[] -> OutputStream
    // ----------------------------------------------------------------

    /**
     * Copy bytes from a <code>byte[]</code> to an <code>OutputStream</code>.
     * @param input the byte array to read from
     * @param output the <code>OutputStream</code> to write to
     * @throws IOException In case of an I/O problem
     */
    public static void copy(byte[] input, OutputStream output)
            throws IOException {
        output.write(input);
    }

    // ----------------------------------------------------------------
    // byte[] -> Writer
    // ----------------------------------------------------------------

    /**
     * Copy and convert bytes from a <code>byte[]</code> to chars on a
     * <code>Writer</code>.
     * The platform's default encoding is used for the byte-to-char conversion.
     * @param input the byte array to read from
     * @param output the <code>Writer</code> to write to
     * @throws IOException In case of an I/O problem
     */
    public static void copy(byte[] input, Writer output)
            throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(input);
        copy(in, output);
    }


    /**
     * Copy and convert bytes from a <code>byte[]</code> to chars on a
     * <code>Writer</code>, using the specified encoding.
     * @param input the byte array to read from
     * @param output the <code>Writer</code> to write to
     * @param encoding The name of a supported character encoding. See the
     * <a href="http://www.iana.org/assignments/character-sets">IANA
     * Charset Registry</a> for a list of valid encoding types.
     * @throws IOException In case of an I/O problem
     */
    public static void copy(
            byte[] input,
            Writer output,
            String encoding)
                throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(input);
        copy(in, output, encoding);
    }


    // ----------------------------------------------------------------
    // Core copy methods
    // ----------------------------------------------------------------

    /**
     * Copy bytes from an <code>InputStream</code> to an
     * <code>OutputStream</code>.
     * @param input the <code>InputStream</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @return the number of bytes copied
     * @throws IOException In case of an I/O problem
     */
    public static int copy(
            InputStream input,
            OutputStream output)
                throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    // ----------------------------------------------------------------
    // Reader -> Writer
    // ----------------------------------------------------------------

    /**
     * Copy chars from a <code>Reader</code> to a <code>Writer</code>.
     * @param input the <code>Reader</code> to read from
     * @param output the <code>Writer</code> to write to
     * @return the number of characters copied
     * @throws IOException In case of an I/O problem
     */
    public static int copy(
            Reader input,
            Writer output)
                throws IOException {
        char[] buffer = new char[DEFAULT_BUFFER_SIZE];
        int count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    // ----------------------------------------------------------------
    // InputStream -> Writer
    // ----------------------------------------------------------------

    /**
     * Copy and convert bytes from an <code>InputStream</code> to chars on a
     * <code>Writer</code>.
     * The platform's default encoding is used for the byte-to-char conversion.
     * @param input the <code>InputStream</code> to read from
     * @param output the <code>Writer</code> to write to
     * @throws IOException In case of an I/O problem
     */
    public static void copy(
            InputStream input,
            Writer output)
                throws IOException {
        InputStreamReader in = new InputStreamReader(input);
        copy(in, output);
    }

    /**
     * Copy and convert bytes from an <code>InputStream</code> to chars on a
     * <code>Writer</code>, using the specified encoding.
     * @param input the <code>InputStream</code> to read from
     * @param output the <code>Writer</code> to write to
     * @param encoding The name of a supported character encoding. See the
     * <a href="http://www.iana.org/assignments/character-sets">IANA
     * Charset Registry</a> for a list of valid encoding types.
     * @throws IOException In case of an I/O problem
     */
    public static void copy(
            InputStream input,
            Writer output,
            String encoding)
                throws IOException {
        InputStreamReader in = new InputStreamReader(input, encoding);
        copy(in, output);
    }


    // ----------------------------------------------------------------
    // Reader -> OutputStream
    // ----------------------------------------------------------------

    /**
     * Serialize chars from a <code>Reader</code> to bytes on an
     * <code>OutputStream</code>, and flush the <code>OutputStream</code>.
     * @param input the <code>Reader</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @throws IOException In case of an I/O problem
     */
    public static void copy(
            Reader input,
            OutputStream output)
                throws IOException {
        OutputStreamWriter out = new OutputStreamWriter(output);
        copy(input, out);
        // XXX Unless anyone is planning on rewriting OutputStreamWriter, we
        // have to flush here.
        out.flush();
    }

    // ----------------------------------------------------------------
    // String -> OutputStream
    // ----------------------------------------------------------------

    /**
     * Serialize chars from a <code>String</code> to bytes on an
     * <code>OutputStream</code>, and
     * flush the <code>OutputStream</code>.
     * @param input the <code>String</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @throws IOException In case of an I/O problem
     */
    public static void copy(
            String input,
            OutputStream output)
                throws IOException {
        StringReader in = new StringReader(input);
        OutputStreamWriter out = new OutputStreamWriter(output);
        copy(in, out);
        // XXX Unless anyone is planning on rewriting OutputStreamWriter, we
        // have to flush here.
        out.flush();
    }

    // ----------------------------------------------------------------
    // String -> Writer
    // ----------------------------------------------------------------

    /**
     * Copy chars from a <code>String</code> to a <code>Writer</code>.
     * @param input the <code>String</code> to read from
     * @param output the <code>Writer</code> to write to
     * @throws IOException In case of an I/O problem
     */
    public static void copy(String input, Writer output)
                throws IOException {
        output.write(input);
    }

}
