
package com.fsck.k9.mail.filter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * A filtering InputStream that allows single byte "peeks" without consuming the byte. The
 * client of this stream can call peek() to see the next available byte in the stream
 * and a subsequent read will still return the peeked byte.
 */
public class PeekableInputStream extends InputStream {
    private InputStream mIn;
    private boolean mPeeked;
    private int mPeekedByte;

    public PeekableInputStream(InputStream in) {
        this.mIn = in;
    }

    @Override
    public int read() throws IOException {
        if (!mPeeked) {
            return mIn.read();
        } else {
            mPeeked = false;
            return mPeekedByte;
        }
    }

    public int peek() throws IOException {
        if (!mPeeked) {
            mPeekedByte = mIn.read();
            mPeeked = true;
        }
        return mPeekedByte;
    }

    @Override
    public int read(byte[] b, int offset, int length) throws IOException {
        if (!mPeeked) {
            return mIn.read(b, offset, length);
        } else {
            b[offset] = (byte)mPeekedByte;
            mPeeked = false;
            int r = mIn.read(b, offset + 1, length - 1);
            if (r == -1) {
                return 1;
            } else {
                return r + 1;
            }
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "PeekableInputStream(in=%s, peeked=%b, peekedByte=%d)",
                             mIn.toString(), mPeeked, mPeekedByte);
    }
}
