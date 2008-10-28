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

import java.io.IOException;
import java.io.OutputStream;

/**
 * Dumps data in hexadecimal format.
 * <p>
 * Provides a single function to take an array of bytes and display it
 * in hexadecimal form.
 * <p>
 * Origin of code: POI.
 *
 * @author Scott Sanders
 * @author Marc Johnson
 * @version $Id: HexDump.java 596667 2007-11-20 13:50:14Z niallp $
 */
public class HexDump {

    /**
     * Instances should NOT be constructed in standard programming.
     */
    public HexDump() {
        super();
    }

    /**
     * Dump an array of bytes to an OutputStream.
     *
     * @param data  the byte array to be dumped
     * @param offset  its offset, whatever that might mean
     * @param stream  the OutputStream to which the data is to be
     *               written
     * @param index initial index into the byte array
     *
     * @throws IOException is thrown if anything goes wrong writing
     *         the data to stream
     * @throws ArrayIndexOutOfBoundsException if the index is
     *         outside the data array's bounds
     * @throws IllegalArgumentException if the output stream is null
     */

    public static void dump(byte[] data, long offset,
                            OutputStream stream, int index)
            throws IOException, ArrayIndexOutOfBoundsException,
            IllegalArgumentException {
        
        if ((index < 0) || (index >= data.length)) {
            throw new ArrayIndexOutOfBoundsException(
                    "illegal index: " + index + " into array of length "
                    + data.length);
        }
        if (stream == null) {
            throw new IllegalArgumentException("cannot write to nullstream");
        }
        long display_offset = offset + index;
        StringBuffer buffer = new StringBuffer(74);

        for (int j = index; j < data.length; j += 16) {
            int chars_read = data.length - j;

            if (chars_read > 16) {
                chars_read = 16;
            }
            dump(buffer, display_offset).append(' ');
            for (int k = 0; k < 16; k++) {
                if (k < chars_read) {
                    dump(buffer, data[k + j]);
                } else {
                    buffer.append("  ");
                }
                buffer.append(' ');
            }
            for (int k = 0; k < chars_read; k++) {
                if ((data[k + j] >= ' ') && (data[k + j] < 127)) {
                    buffer.append((char) data[k + j]);
                } else {
                    buffer.append('.');
                }
            }
            buffer.append(EOL);
            stream.write(buffer.toString().getBytes());
            stream.flush();
            buffer.setLength(0);
            display_offset += chars_read;
        }
    }

    /**
     * The line-separator (initializes to "line.separator" system property.
     */
    public static final String EOL =
            System.getProperty("line.separator");
    private static final char[] _hexcodes =
            {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'A', 'B', 'C', 'D', 'E', 'F'
            };
    private static final int[] _shifts =
            {
                28, 24, 20, 16, 12, 8, 4, 0
            };

    /**
     * Dump a long value into a StringBuffer.
     *
     * @param _lbuffer the StringBuffer to dump the value in
     * @param value  the long value to be dumped
     * @return StringBuffer containing the dumped value.
     */
    private static StringBuffer dump(StringBuffer _lbuffer, long value) {
        for (int j = 0; j < 8; j++) {
            _lbuffer
                    .append(_hexcodes[((int) (value >> _shifts[j])) & 15]);
        }
        return _lbuffer;
    }

    /**
     * Dump a byte value into a StringBuffer.
     *
     * @param _cbuffer the StringBuffer to dump the value in
     * @param value  the byte value to be dumped
     * @return StringBuffer containing the dumped value.
     */
    private static StringBuffer dump(StringBuffer _cbuffer, byte value) {
        for (int j = 0; j < 2; j++) {
            _cbuffer.append(_hexcodes[(value >> _shifts[j + 6]) & 15]);
        }
        return _cbuffer;
    }

}
