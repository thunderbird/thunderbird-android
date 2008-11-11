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

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.EndianUtils;

/**
 * DataInput for systems relying on little endian data formats.
 * When read, values will be changed from little endian to big 
 * endian formats for internal usage. 
 * <p>
 * <b>Origin of code: </b>Avalon Excalibur (IO)
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version CVS $Revision: 610010 $ $Date: 2008-01-08 14:50:59 +0000 (Tue, 08 Jan 2008) $
 */
public class SwappedDataInputStream extends ProxyInputStream
    implements DataInput
{

    /**
     * Constructs a SwappedDataInputStream.
     *
     * @param input InputStream to read from
     */
    public SwappedDataInputStream( InputStream input )
    {
        super( input );
    }

    /**
     * Return <code>{@link #readByte()} == 0</code>
     * @return the true if the byte read is zero, otherwise false
     * @throws IOException if an I/O error occurs
     * @throws EOFException if an end of file is reached unexpectedly
     */
    public boolean readBoolean()
        throws IOException, EOFException
    {
        return ( 0 == readByte() );
    }

    /**
     * Invokes the delegate's <code>read()</code> method.
     * @return the byte read or -1 if the end of stream
     * @throws IOException if an I/O error occurs
     * @throws EOFException if an end of file is reached unexpectedly
     */
    public byte readByte()
        throws IOException, EOFException
    {
        return (byte)in.read();
    }

    /**
     * Reads a character delegating to {@link #readShort()}.
     * @return the byte read or -1 if the end of stream
     * @throws IOException if an I/O error occurs
     * @throws EOFException if an end of file is reached unexpectedly
     */
    public char readChar()
        throws IOException, EOFException
    {
        return (char)readShort();
    }

    /**
     * Delegates to {@link EndianUtils#readSwappedDouble(InputStream)}. 
     * @return the read long
     * @throws IOException if an I/O error occurs
     * @throws EOFException if an end of file is reached unexpectedly
     */
    public double readDouble()
        throws IOException, EOFException
    {
        return EndianUtils.readSwappedDouble( in );
    }

    /**
     * Delegates to {@link EndianUtils#readSwappedFloat(InputStream)}. 
     * @return the read long
     * @throws IOException if an I/O error occurs
     * @throws EOFException if an end of file is reached unexpectedly
     */
    public float readFloat()
        throws IOException, EOFException
    {
        return EndianUtils.readSwappedFloat( in );
    }

    /**
     * Invokes the delegate's <code>read(byte[] data, int, int)</code> method.
     * 
     * @param data the buffer to read the bytes into
     * @throws EOFException if an end of file is reached unexpectedly
     * @throws IOException if an I/O error occurs
     */
    public void readFully( byte[] data )
        throws IOException, EOFException
    {
        readFully( data, 0, data.length );
    }


    /**
     * Invokes the delegate's <code>read(byte[] data, int, int)</code> method.
     * 
     * @param data the buffer to read the bytes into
     * @param offset The start offset
     * @param length The number of bytes to read
     * @throws EOFException if an end of file is reached unexpectedly
     * @throws IOException if an I/O error occurs
     */
    public void readFully( byte[] data, int offset, int length )
        throws IOException, EOFException
    {
        int remaining = length;

        while( remaining > 0 )
        {
            int location = offset + ( length - remaining );
            int count = read( data, location, remaining );

            if( -1 == count )
            {
                throw new EOFException();
            }

            remaining -= count;
        }
    }

    /**
     * Delegates to {@link EndianUtils#readSwappedInteger(InputStream)}. 
     * @return the read long
     * @throws EOFException if an end of file is reached unexpectedly
     * @throws IOException if an I/O error occurs
     */
    public int readInt()
        throws IOException, EOFException
    {
        return EndianUtils.readSwappedInteger( in );
    }

    /**
     * Not currently supported - throws {@link UnsupportedOperationException}.
     * @return the line read
     * @throws EOFException if an end of file is reached unexpectedly
     * @throws IOException if an I/O error occurs
     */
    public String readLine()
        throws IOException, EOFException
    {
        throw new UnsupportedOperationException( 
                "Operation not supported: readLine()" );
    }

    /**
     * Delegates to {@link EndianUtils#readSwappedLong(InputStream)}. 
     * @return the read long
     * @throws EOFException if an end of file is reached unexpectedly
     * @throws IOException if an I/O error occurs
     */
    public long readLong()
        throws IOException, EOFException
    {
        return EndianUtils.readSwappedLong( in );
    }

    /**
     * Delegates to {@link EndianUtils#readSwappedShort(InputStream)}. 
     * @return the read long
     * @throws EOFException if an end of file is reached unexpectedly
     * @throws IOException if an I/O error occurs
     */
    public short readShort()
        throws IOException, EOFException
    {
        return EndianUtils.readSwappedShort( in );
    }

    /**
     * Invokes the delegate's <code>read()</code> method.
     * @return the byte read or -1 if the end of stream
     * @throws EOFException if an end of file is reached unexpectedly
     * @throws IOException if an I/O error occurs
     */
    public int readUnsignedByte()
        throws IOException, EOFException
    {
        return in.read();
    }

    /**
     * Delegates to {@link EndianUtils#readSwappedUnsignedShort(InputStream)}. 
     * @return the read long
     * @throws EOFException if an end of file is reached unexpectedly
     * @throws IOException if an I/O error occurs
     */
    public int readUnsignedShort()
        throws IOException, EOFException
    {
        return EndianUtils.readSwappedUnsignedShort( in );
    }

    /**
     * Not currently supported - throws {@link UnsupportedOperationException}.
     * @return UTF String read
     * @throws EOFException if an end of file is reached unexpectedly
     * @throws IOException if an I/O error occurs
     */
    public String readUTF()
        throws IOException, EOFException
    {
        throw new UnsupportedOperationException( 
                "Operation not supported: readUTF()" );
    }

    /**
     * Invokes the delegate's <code>skip(int)</code> method.
     * @param count the number of bytes to skip
     * @return the number of bytes to skipped or -1 if the end of stream
     * @throws EOFException if an end of file is reached unexpectedly
     * @throws IOException if an I/O error occurs
     */
    public int skipBytes( int count )
        throws IOException, EOFException
    {
        return (int)in.skip( count );
    }

}
