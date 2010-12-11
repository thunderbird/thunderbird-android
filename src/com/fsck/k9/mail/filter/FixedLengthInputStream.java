
package com.fsck.k9.mail.filter;

import java.io.IOException;
import java.io.InputStream;

/**
 * A filtering InputStream that stops allowing reads after the given length has been read. This
 * is used to allow a client to read directly from an underlying protocol stream without reading
 * past where the protocol handler intended the client to read.
 */
public class FixedLengthInputStream extends InputStream
{
    private InputStream mIn;
    private int mLength;
    private int mCount;

    public FixedLengthInputStream(InputStream in, int length)
    {
        this.mIn = in;
        this.mLength = length;
    }

    @Override
    public int available() throws IOException
    {
        return mLength - mCount;
    }

    @Override
    public int read() throws IOException
    {
        if (mCount < mLength)
        {
            mCount++;
            return mIn.read();
        }
        else
        {
            return -1;
        }
    }

    @Override
    public int read(byte[] b, int offset, int length) throws IOException
    {
        if (mCount < mLength)
        {
            int d = mIn.read(b, offset, Math.min(mLength - mCount, length));
            if (d == -1)
            {
                return -1;
            }
            else
            {
                mCount += d;
                return d;
            }
        }
        else
        {
            return -1;
        }
    }

    @Override
    public int read(byte[] b) throws IOException
    {
        return read(b, 0, b.length);
    }

    @Override
    public String toString()
    {
        return String.format("FixedLengthInputStream(in=%s, length=%d)", mIn.toString(), mLength);
    }
}
