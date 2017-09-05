package com.fsck.k9.mail.store.pop3;


import java.io.IOException;
import java.io.InputStream;


class Pop3ResponseInputStream extends InputStream {
    private InputStream mIn;
    private boolean mStartOfLine = true;
    private boolean mFinished;

    Pop3ResponseInputStream(InputStream in) {
        mIn = in;
    }

    @Override
    public int read() throws IOException {
        if (mFinished) {
            return -1;
        }
        int d = mIn.read();
        if (mStartOfLine && d == '.') {
            d = mIn.read();
            if (d == '\r') {
                mFinished = true;
                mIn.read();
                return -1;
            }
        }

        mStartOfLine = (d == '\n');

        return d;
    }
}
