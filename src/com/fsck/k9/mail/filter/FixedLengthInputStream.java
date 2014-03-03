
package com.fsck.k9.mail.filter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * A filtering InputStream that stops allowing reads after the given length has been read. This
 * is used to allow a client to read directly from an underlying protocol stream without reading
 * past where the protocol handler intended the client to read.
 */
public class FixedLengthInputStream extends InputStream {
    private final InputStream mIn;
    private final int mLength;
    private int mCount = 0;

    public FixedLengthInputStream(InputStream in, int length) {
        this.mIn = in;
        this.mLength = length;
    }

    @Override
    public int available() throws IOException {
        return mLength - mCount;
    }

    @Override
    public int read() throws IOException {
        if (mCount >= mLength) {
            return -1;
        }

        int d = mIn.read();
        if (d != -1) {
            mCount++;
        }
        return d;
    }

    @Override
    public int read(byte[] b, int offset, int length) throws IOException {
        if (mCount >= mLength) {
            return -1;
        }

        int d = mIn.read(b, offset, Math.min(mLength - mCount, length));
        if (d != -1) {
            mCount += d;
        }
        return d;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public long skip(long n) throws IOException {
        long d = mIn.skip(Math.min(n, available()));
        if (d > 0) {
            mCount += d;
        }
        return d;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "FixedLengthInputStream(in=%s, length=%d)", mIn.toString(), mLength);
    }
}
