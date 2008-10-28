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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility code for dealing with different endian systems.
 * <p>
 * Different computer architectures adopt different conventions for
 * byte ordering. In so-called "Little Endian" architectures (eg Intel),
 * the low-order byte is stored in memory at the lowest address, and
 * subsequent bytes at higher addresses. For "Big Endian" architectures
 * (eg Motorola), the situation is reversed.
 * This class helps you solve this incompatability.
 * <p>
 * Origin of code: Excalibur
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Id: EndianUtils.java 539632 2007-05-18 23:37:59Z bayard $
 * @see org.apache.commons.io.input.SwappedDataInputStream
 */
public class EndianUtils {

    /**
     * Instances should NOT be constructed in standard programming.
     */
    public EndianUtils() {
        super();
    }

    // ========================================== Swapping routines

    /**
     * Converts a "short" value between endian systems.
     * @param value value to convert
     * @return the converted value
     */
    public static short swapShort(short value) {
        return (short) ( ( ( ( value >> 0 ) & 0xff ) << 8 ) +
            ( ( ( value >> 8 ) & 0xff ) << 0 ) );
    }

    /**
     * Converts a "int" value between endian systems.
     * @param value value to convert
     * @return the converted value
     */
    public static int swapInteger(int value) {
        return
            ( ( ( value >> 0 ) & 0xff ) << 24 ) +
            ( ( ( value >> 8 ) & 0xff ) << 16 ) +
            ( ( ( value >> 16 ) & 0xff ) << 8 ) +
            ( ( ( value >> 24 ) & 0xff ) << 0 );
    }

    /**
     * Converts a "long" value between endian systems.
     * @param value value to convert
     * @return the converted value
     */
    public static long swapLong(long value) {
        return
            ( ( ( value >> 0 ) & 0xff ) << 56 ) +
            ( ( ( value >> 8 ) & 0xff ) << 48 ) +
            ( ( ( value >> 16 ) & 0xff ) << 40 ) +
            ( ( ( value >> 24 ) & 0xff ) << 32 ) +
            ( ( ( value >> 32 ) & 0xff ) << 24 ) +
            ( ( ( value >> 40 ) & 0xff ) << 16 ) +
            ( ( ( value >> 48 ) & 0xff ) << 8 ) +
            ( ( ( value >> 56 ) & 0xff ) << 0 );
    }

    /**
     * Converts a "float" value between endian systems.
     * @param value value to convert
     * @return the converted value
     */
    public static float swapFloat(float value) {
        return Float.intBitsToFloat( swapInteger( Float.floatToIntBits( value ) ) );
    }

    /**
     * Converts a "double" value between endian systems.
     * @param value value to convert
     * @return the converted value
     */
    public static double swapDouble(double value) {
        return Double.longBitsToDouble( swapLong( Double.doubleToLongBits( value ) ) );
    }

    // ========================================== Swapping read/write routines

    /**
     * Writes a "short" value to a byte array at a given offset. The value is
     * converted to the opposed endian system while writing.
     * @param data target byte array
     * @param offset starting offset in the byte array
     * @param value value to write
     */
    public static void writeSwappedShort(byte[] data, int offset, short value) {
        data[ offset + 0 ] = (byte)( ( value >> 0 ) & 0xff );
        data[ offset + 1 ] = (byte)( ( value >> 8 ) & 0xff );
    }

    /**
     * Reads a "short" value from a byte array at a given offset. The value is
     * converted to the opposed endian system while reading.
     * @param data source byte array
     * @param offset starting offset in the byte array
     * @return the value read
     */
    public static short readSwappedShort(byte[] data, int offset) {
        return (short)( ( ( data[ offset + 0 ] & 0xff ) << 0 ) +
            ( ( data[ offset + 1 ] & 0xff ) << 8 ) );
    }

    /**
     * Reads an unsigned short (16-bit) value from a byte array at a given
     * offset. The value is converted to the opposed endian system while
     * reading.
     * @param data source byte array
     * @param offset starting offset in the byte array
     * @return the value read
     */
    public static int readSwappedUnsignedShort(byte[] data, int offset) {
        return ( ( ( data[ offset + 0 ] & 0xff ) << 0 ) +
            ( ( data[ offset + 1 ] & 0xff ) << 8 ) );
    }

    /**
     * Writes a "int" value to a byte array at a given offset. The value is
     * converted to the opposed endian system while writing.
     * @param data target byte array
     * @param offset starting offset in the byte array
     * @param value value to write
     */
    public static void writeSwappedInteger(byte[] data, int offset, int value) {
        data[ offset + 0 ] = (byte)( ( value >> 0 ) & 0xff );
        data[ offset + 1 ] = (byte)( ( value >> 8 ) & 0xff );
        data[ offset + 2 ] = (byte)( ( value >> 16 ) & 0xff );
        data[ offset + 3 ] = (byte)( ( value >> 24 ) & 0xff );
    }

    /**
     * Reads a "int" value from a byte array at a given offset. The value is
     * converted to the opposed endian system while reading.
     * @param data source byte array
     * @param offset starting offset in the byte array
     * @return the value read
     */
    public static int readSwappedInteger(byte[] data, int offset) {
        return ( ( ( data[ offset + 0 ] & 0xff ) << 0 ) +
            ( ( data[ offset + 1 ] & 0xff ) << 8 ) +
            ( ( data[ offset + 2 ] & 0xff ) << 16 ) +
            ( ( data[ offset + 3 ] & 0xff ) << 24 ) );
    }

    /**
     * Reads an unsigned integer (32-bit) value from a byte array at a given
     * offset. The value is converted to the opposed endian system while
     * reading.
     * @param data source byte array
     * @param offset starting offset in the byte array
     * @return the value read
     */
    public static long readSwappedUnsignedInteger(byte[] data, int offset) {
        long low = ( ( ( data[ offset + 0 ] & 0xff ) << 0 ) +
                     ( ( data[ offset + 1 ] & 0xff ) << 8 ) +
                     ( ( data[ offset + 2 ] & 0xff ) << 16 ) );

        long high = data[ offset + 3 ] & 0xff;

        return (high << 24) + (0xffffffffL & low); 
    }

    /**
     * Writes a "long" value to a byte array at a given offset. The value is
     * converted to the opposed endian system while writing.
     * @param data target byte array
     * @param offset starting offset in the byte array
     * @param value value to write
     */
    public static void writeSwappedLong(byte[] data, int offset, long value) {
        data[ offset + 0 ] = (byte)( ( value >> 0 ) & 0xff );
        data[ offset + 1 ] = (byte)( ( value >> 8 ) & 0xff );
        data[ offset + 2 ] = (byte)( ( value >> 16 ) & 0xff );
        data[ offset + 3 ] = (byte)( ( value >> 24 ) & 0xff );
        data[ offset + 4 ] = (byte)( ( value >> 32 ) & 0xff );
        data[ offset + 5 ] = (byte)( ( value >> 40 ) & 0xff );
        data[ offset + 6 ] = (byte)( ( value >> 48 ) & 0xff );
        data[ offset + 7 ] = (byte)( ( value >> 56 ) & 0xff );
    }

    /**
     * Reads a "long" value from a byte array at a given offset. The value is
     * converted to the opposed endian system while reading.
     * @param data source byte array
     * @param offset starting offset in the byte array
     * @return the value read
     */
    public static long readSwappedLong(byte[] data, int offset) {
        long low = 
            ( ( data[ offset + 0 ] & 0xff ) << 0 ) +
            ( ( data[ offset + 1 ] & 0xff ) << 8 ) +
            ( ( data[ offset + 2 ] & 0xff ) << 16 ) +
            ( ( data[ offset + 3 ] & 0xff ) << 24 );
        long high = 
            ( ( data[ offset + 4 ] & 0xff ) << 0 ) +
            ( ( data[ offset + 5 ] & 0xff ) << 8 ) +
            ( ( data[ offset + 6 ] & 0xff ) << 16 ) +
            ( ( data[ offset + 7 ] & 0xff ) << 24 );
        return (high << 32) + (0xffffffffL & low); 
    }

    /**
     * Writes a "float" value to a byte array at a given offset. The value is
     * converted to the opposed endian system while writing.
     * @param data target byte array
     * @param offset starting offset in the byte array
     * @param value value to write
     */
    public static void writeSwappedFloat(byte[] data, int offset, float value) {
        writeSwappedInteger( data, offset, Float.floatToIntBits( value ) );
    }

    /**
     * Reads a "float" value from a byte array at a given offset. The value is
     * converted to the opposed endian system while reading.
     * @param data source byte array
     * @param offset starting offset in the byte array
     * @return the value read
     */
    public static float readSwappedFloat(byte[] data, int offset) {
        return Float.intBitsToFloat( readSwappedInteger( data, offset ) );
    }

    /**
     * Writes a "double" value to a byte array at a given offset. The value is
     * converted to the opposed endian system while writing.
     * @param data target byte array
     * @param offset starting offset in the byte array
     * @param value value to write
     */
    public static void writeSwappedDouble(byte[] data, int offset, double value) {
        writeSwappedLong( data, offset, Double.doubleToLongBits( value ) );
    }

    /**
     * Reads a "double" value from a byte array at a given offset. The value is
     * converted to the opposed endian system while reading.
     * @param data source byte array
     * @param offset starting offset in the byte array
     * @return the value read
     */
    public static double readSwappedDouble(byte[] data, int offset) {
        return Double.longBitsToDouble( readSwappedLong( data, offset ) );
    }

    /**
     * Writes a "short" value to an OutputStream. The value is
     * converted to the opposed endian system while writing.
     * @param output target OutputStream
     * @param value value to write
     * @throws IOException in case of an I/O problem
     */
    public static void writeSwappedShort(OutputStream output, short value)
        throws IOException
    {
        output.write( (byte)( ( value >> 0 ) & 0xff ) );
        output.write( (byte)( ( value >> 8 ) & 0xff ) );
    }

    /**
     * Reads a "short" value from an InputStream. The value is
     * converted to the opposed endian system while reading.
     * @param input source InputStream
     * @return the value just read
     * @throws IOException in case of an I/O problem
     */
    public static short readSwappedShort(InputStream input)
        throws IOException
    {
        return (short)( ( ( read( input ) & 0xff ) << 0 ) +
            ( ( read( input ) & 0xff ) << 8 ) );
    }

    /**
     * Reads a unsigned short (16-bit) from an InputStream. The value is
     * converted to the opposed endian system while reading.
     * @param input source InputStream
     * @return the value just read
     * @throws IOException in case of an I/O problem
     */
    public static int readSwappedUnsignedShort(InputStream input)
        throws IOException
    {
        int value1 = read( input );
        int value2 = read( input );

        return ( ( ( value1 & 0xff ) << 0 ) +
            ( ( value2 & 0xff ) << 8 ) );
    }

    /**
     * Writes a "int" value to an OutputStream. The value is
     * converted to the opposed endian system while writing.
     * @param output target OutputStream
     * @param value value to write
     * @throws IOException in case of an I/O problem
     */
    public static void writeSwappedInteger(OutputStream output, int value)
        throws IOException
    {
        output.write( (byte)( ( value >> 0 ) & 0xff ) );
        output.write( (byte)( ( value >> 8 ) & 0xff ) );
        output.write( (byte)( ( value >> 16 ) & 0xff ) );
        output.write( (byte)( ( value >> 24 ) & 0xff ) );
    }

    /**
     * Reads a "int" value from an InputStream. The value is
     * converted to the opposed endian system while reading.
     * @param input source InputStream
     * @return the value just read
     * @throws IOException in case of an I/O problem
     */
    public static int readSwappedInteger(InputStream input)
        throws IOException
    {
        int value1 = read( input );
        int value2 = read( input );
        int value3 = read( input );
        int value4 = read( input );

        return ( ( value1 & 0xff ) << 0 ) +
            ( ( value2 & 0xff ) << 8 ) +
            ( ( value3 & 0xff ) << 16 ) +
            ( ( value4 & 0xff ) << 24 );
    }

    /**
     * Reads a unsigned integer (32-bit) from an InputStream. The value is
     * converted to the opposed endian system while reading.
     * @param input source InputStream
     * @return the value just read
     * @throws IOException in case of an I/O problem
     */
    public static long readSwappedUnsignedInteger(InputStream input)
        throws IOException
    {
        int value1 = read( input );
        int value2 = read( input );
        int value3 = read( input );
        int value4 = read( input );

        long low = ( ( ( value1 & 0xff ) << 0 ) +
                     ( ( value2 & 0xff ) << 8 ) +
                     ( ( value3 & 0xff ) << 16 ) );

        long high = value4 & 0xff;

        return (high << 24) + (0xffffffffL & low); 
    }

    /**
     * Writes a "long" value to an OutputStream. The value is
     * converted to the opposed endian system while writing.
     * @param output target OutputStream
     * @param value value to write
     * @throws IOException in case of an I/O problem
     */
    public static void writeSwappedLong(OutputStream output, long value)
        throws IOException
    {
        output.write( (byte)( ( value >> 0 ) & 0xff ) );
        output.write( (byte)( ( value >> 8 ) & 0xff ) );
        output.write( (byte)( ( value >> 16 ) & 0xff ) );
        output.write( (byte)( ( value >> 24 ) & 0xff ) );
        output.write( (byte)( ( value >> 32 ) & 0xff ) );
        output.write( (byte)( ( value >> 40 ) & 0xff ) );
        output.write( (byte)( ( value >> 48 ) & 0xff ) );
        output.write( (byte)( ( value >> 56 ) & 0xff ) );
    }

    /**
     * Reads a "long" value from an InputStream. The value is
     * converted to the opposed endian system while reading.
     * @param input source InputStream
     * @return the value just read
     * @throws IOException in case of an I/O problem
     */
    public static long readSwappedLong(InputStream input)
        throws IOException
    {
        byte[] bytes = new byte[8];
        for ( int i=0; i<8; i++ ) {
            bytes[i] = (byte) read( input );
        }
        return readSwappedLong( bytes, 0 );
    }

    /**
     * Writes a "float" value to an OutputStream. The value is
     * converted to the opposed endian system while writing.
     * @param output target OutputStream
     * @param value value to write
     * @throws IOException in case of an I/O problem
     */
    public static void writeSwappedFloat(OutputStream output, float value)
        throws IOException
    {
        writeSwappedInteger( output, Float.floatToIntBits( value ) );
    }

    /**
     * Reads a "float" value from an InputStream. The value is
     * converted to the opposed endian system while reading.
     * @param input source InputStream
     * @return the value just read
     * @throws IOException in case of an I/O problem
     */
    public static float readSwappedFloat(InputStream input)
        throws IOException
    {
        return Float.intBitsToFloat( readSwappedInteger( input ) );
    }

    /**
     * Writes a "double" value to an OutputStream. The value is
     * converted to the opposed endian system while writing.
     * @param output target OutputStream
     * @param value value to write
     * @throws IOException in case of an I/O problem
     */
    public static void writeSwappedDouble(OutputStream output, double value)
        throws IOException
    {
        writeSwappedLong( output, Double.doubleToLongBits( value ) );
    }

    /**
     * Reads a "double" value from an InputStream. The value is
     * converted to the opposed endian system while reading.
     * @param input source InputStream
     * @return the value just read
     * @throws IOException in case of an I/O problem
     */
    public static double readSwappedDouble(InputStream input)
        throws IOException
    {
        return Double.longBitsToDouble( readSwappedLong( input ) );
    }

    /**
     * Reads the next byte from the input stream.
     * @param input  the stream
     * @return the byte
     * @throws IOException if the end of file is reached
     */
    private static int read(InputStream input)
        throws IOException
    {
        int value = input.read();

        if( -1 == value ) {
            throw new EOFException( "Unexpected EOF reached" );
        }

        return value;
    }
}
