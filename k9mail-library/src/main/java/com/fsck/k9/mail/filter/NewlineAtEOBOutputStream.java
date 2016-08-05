package com.fsck.k9.mail.filter;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class NewlineAtEOBOutputStream extends FilterOutputStream {
    private static final int CR = '\r';
    private static final int LF = '\n';
    private int lastChar;

    public NewlineAtEOBOutputStream(OutputStream out) {
        super(out);
    }

    @Override
    public void write(int oneByte) throws IOException {
        super.write(oneByte);
        lastChar = oneByte;
    }

    // write End of Buffer
    //
    // use if you want to write <CR><LF> at the end of buffer.
    public void writeEOB() throws IOException {
        switch (lastChar) {
            case CR:
                super.write(LF);
                break;
            case LF:
                break;
            default:
                super.write(CR);
                super.write(LF);
                break;
        }
        super.flush();
    }
}
