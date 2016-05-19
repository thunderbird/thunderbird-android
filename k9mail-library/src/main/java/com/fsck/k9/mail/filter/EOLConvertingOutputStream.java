package com.fsck.k9.mail.filter;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class EOLConvertingOutputStream extends FilterOutputStream {
    private static final int CR = '\r';
    private static final int LF = '\n';
    private int lastChar;
    private static final int IGNORE_LF = Integer.MIN_VALUE;


    public EOLConvertingOutputStream(OutputStream out) {
        super(out);
    }

    @Override
    public void write(int oneByte) throws IOException {
        if (oneByte == LF && lastChar == IGNORE_LF) {
            lastChar = LF;
            return;
        }
        if (oneByte == LF && lastChar != CR) {
            super.write(CR);
        } else if (oneByte != LF && lastChar == CR) {
            super.write(LF);
        }
        super.write(oneByte);
        lastChar = oneByte;
    }

    @Override
    public void flush() throws IOException {
        if (lastChar == CR) {
            super.write(LF);
            // We have to ignore the next character if it is <LF>. Otherwise it
            // will be expanded to an additional <CR><LF> sequence although it
            // belongs to the one just completed.
            lastChar = IGNORE_LF;
        }
        super.flush();
    }
}
