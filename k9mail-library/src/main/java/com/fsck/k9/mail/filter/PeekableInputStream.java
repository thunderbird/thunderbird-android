package com.fsck.k9.mail.filter;


import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;


/**
 * A filtering InputStream that allows single byte "peeks" without consuming the byte. The
 * client of this stream can call peek() to see the next available byte in the stream
 * and a subsequent read will still return the peeked byte.
 */
public class PeekableInputStream extends FilterInputStream {
    private boolean peeked;
    private int peekedByte;


    public PeekableInputStream(InputStream in) {
        super(in);
    }

    @Override
    public int read() throws IOException {
        if (!peeked) {
            return in.read();
        } else {
            peeked = false;
            return peekedByte;
        }
    }

    public int peek() throws IOException {
        if (!peeked) {
            peekedByte = in.read();
            peeked = true;
        }
        return peekedByte;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (!peeked) {
            return in.read(buffer, offset, length);
        } else {
            buffer[offset] = (byte) peekedByte;
            peeked = false;
            int r = in.read(buffer, offset + 1, length - 1);
            if (r == -1) {
                return 1;
            } else {
                return r + 1;
            }
        }
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        return read(buffer, 0, buffer.length);
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "PeekableInputStream(in=%s, peeked=%b, peekedByte=%d)",
                in.toString(), peeked, peekedByte);
    }
}
