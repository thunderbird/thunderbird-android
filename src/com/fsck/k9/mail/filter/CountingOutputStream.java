package com.fsck.k9.mail.filter;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A simple OutputStream that does nothing but count how many bytes are written to it and
 * makes that count available to callers.
 */
public class CountingOutputStream extends OutputStream {
    private long mCount;

    public CountingOutputStream() {
    }

    public long getCount() {
        return mCount;
    }

    @Override
    public void write(int oneByte) throws IOException {
        mCount++;
    }

    @Override
    public void write(byte b[], int offset, int len) throws IOException {
        mCount += len;
    }

    @Override
    public void write(byte[] b) throws IOException {
        mCount += b.length;
    }
}
