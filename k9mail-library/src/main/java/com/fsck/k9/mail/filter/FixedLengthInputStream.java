
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
    private final InputStream in;
    private final int length;
    private int count = 0;

    public FixedLengthInputStream(InputStream in, int length) {
        this.in = in;
        this.length = length;
    }

    @Override
    public int available() throws IOException {
        return length - count;
    }

    @Override
    public int read() throws IOException {
        if (count >= length) {
            return -1;
        }

        int d = in.read();
        if (d != -1) {
            count++;
        }
        return d;
    }

    @Override
    public int read(byte[] b, int offset, int length) throws IOException {
        if (count >= this.length) {
            return -1;
        }

        int d = in.read(b, offset, Math.min(this.length - count, length));
        if (d != -1) {
            count += d;
        }
        return d;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public long skip(long n) throws IOException {
        long d = in.skip(Math.min(n, available()));
        if (d > 0) {
            count += d;
        }
        return d;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "FixedLengthInputStream(in=%s, length=%d)", in.toString(), length);
    }

    public void skipRemaining() throws IOException {
        while (available() > 0) {
            skip(available());
        }
    }
}
